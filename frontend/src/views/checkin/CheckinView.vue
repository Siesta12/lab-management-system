<template>
  <main class="checkin-page">
    <section class="checkin-card">
      <div class="checkin-badge">实验室签到</div>
      <h1>实验室签到</h1>

      <div class="checkin-info">
        <div class="info-row">
          <span class="info-label">实验室名称</span>
          <span class="info-value" :class="{ missing: !labInfo }">{{ labInfo?.labName || '加载中...' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">实验室编号 / 房间</span>
          <span class="info-value" :class="{ missing: !labInfo }">{{ labRoomLabel }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">所属学院</span>
          <span class="info-value" :class="{ missing: !departmentName }">{{ departmentName || '加载中...' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前时间</span>
          <span class="info-value">{{ currentTime }}</span>
        </div>
      </div>

      <p class="checkin-tip">请确认您在实验室内进行签到。</p>

      <p
        v-if="statusMessage"
        class="status-message"
        :class="{
          success: messageTone === 'success',
          error: messageTone === 'error',
        }"
        >
        {{ statusMessage }}
      </p>

      <div v-if="checkinResult" class="result-card" :class="{ late: checkinResult.late }">
        <strong>{{ checkinResult.message }}</strong>
        <span>预约单号：{{ checkinResult.reservationNo }}</span>
        <span>时间：{{ checkinResult.reservationDate }} / {{ checkinResult.periodName }}</span>
        <span v-if="checkinResult.checkInTime">签到时间：{{ checkinResult.checkInTime }}</span>
        <span>距离：{{ checkinResult.distanceMeters }} 米</span>
        <span v-if="checkinResult.scoreChange">信誉分变化：{{ checkinResult.scoreChange }}</span>
      </div>

      <button
        type="button"
        class="checkin-btn"
        :disabled="!canCheckIn"
        @click="handleCheckin"
      >
        {{ submitting ? '正在签到...' : '立即签到' }}
      </button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { fetchDepartmentById } from '../../api/departments';
import { submitCheckin } from '../../api/checkin';
import { fetchLabById } from '../../api/labs';
import type { CheckinResultDto, LabDto } from '../../types';

const TOKEN_KEY = 'lab-system-token';

const route = useRoute();
const currentTime = ref('');
const labInfo = ref<LabDto | null>(null);
const departmentName = ref('');
const statusMessage = ref('');
const messageTone = ref<'info' | 'success' | 'error'>('info');
const checkinResult = ref<CheckinResultDto | null>(null);
const submitting = ref(false);
let timerId: number | undefined;

const labIdentifier = computed(() => {
  const value = route.query.lab_id;
  if (Array.isArray(value)) {
    return value[0] ?? '';
  }
  return typeof value === 'string' ? value.trim() : '';
});

const labId = computed<number | null>(() => {
  if (!/^\d+$/.test(labIdentifier.value)) {
    return null;
  }
  return Number(labIdentifier.value);
});

const authToken = computed(() => localStorage.getItem(TOKEN_KEY) ?? '');

const canCheckIn = computed(() =>
  Boolean(labId.value && labInfo.value && authToken.value && !submitting.value),
);

const labRoomLabel = computed(() => {
  if (!labInfo.value) {
    return '加载中...';
  }
  return [labInfo.value.buildingName, labInfo.value.roomNo].filter(Boolean).join(' / ') || '未设置';
});

function setStatus(message: string, tone: 'info' | 'success' | 'error' = 'info'): void {
  statusMessage.value = message;
  messageTone.value = tone;
}

function refreshTime(): void {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

async function loadLabInfo(): Promise<void> {
  labInfo.value = null;
  departmentName.value = '';
  checkinResult.value = null;
  if (!labId.value) {
    setStatus('未获取到实验室信息', 'error');
    return;
  }

  try {
    const lab = await fetchLabById(labId.value);
    labInfo.value = lab;
    setStatus('', 'info');

    if (lab.departmentId != null) {
      try {
        const department = await fetchDepartmentById(lab.departmentId);
        departmentName.value = department.departmentName;
      } catch {
        departmentName.value = `学院ID ${lab.departmentId}`;
      }
    }
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '实验室信息加载失败', 'error');
  }
}

function readPosition(): Promise<{ latitude: number; longitude: number; accuracy?: number }> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前设备不支持定位，请更换浏览器后重试'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracy: position.coords.accuracy,
        });
      },
      (error) => {
        if (error.code === error.PERMISSION_DENIED) {
          reject(new Error('请先允许定位权限，再进行签到'));
          return;
        }
        reject(new Error('未获取到定位信息，请稍后重试'));
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      },
    );
  });
}

async function handleCheckin(): Promise<void> {
  if (!labId.value) {
    setStatus('未获取到实验室信息', 'error');
    return;
  }
  if (!authToken.value) {
    setStatus('请先登录系统后再进行签到', 'error');
    return;
  }

  submitting.value = true;
  setStatus('正在获取当前位置...', 'info');

  try {
    const location = await readPosition();
    setStatus('正在提交签到请求...', 'info');
    const result = await submitCheckin(
      {
        labIdentifier: String(labId.value),
        latitude: location.latitude,
        longitude: location.longitude,
        accuracy: location.accuracy,
        capturedAt: new Date().toISOString(),
        userAgent: navigator.userAgent,
      },
      authToken.value,
    );
    checkinResult.value = result;
    setStatus(result.message || '签到成功', 'success');
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '签到提交失败', 'error');
  } finally {
    submitting.value = false;
  }
}

watch(
  labIdentifier,
  () => {
    void loadLabInfo();
  },
  { immediate: true },
);

onMounted(() => {
  refreshTime();
  timerId = window.setInterval(refreshTime, 1000);
});

onUnmounted(() => {
  if (timerId !== undefined) {
    window.clearInterval(timerId);
  }
});
</script>

<style scoped>
.checkin-page {
  width: 100%;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(180deg, #eef4ff 0%, #f8fbff 100%);
  box-sizing: border-box;
  overflow-x: hidden;
}

.checkin-card {
  width: min(540px, calc(100vw - 48px));
  padding: 32px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
  display: grid;
  gap: 18px;
  text-align: center;
  box-sizing: border-box;
}

.checkin-badge {
  display: inline-flex;
  justify-content: center;
  align-self: center;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.checkin-card h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
  color: #0f172a;
}

.checkin-info {
  display: grid;
  gap: 10px;
  text-align: left;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.info-label {
  font-size: 13px;
  color: #64748b;
}

.info-value {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-all;
  text-align: right;
}

.info-value.missing {
  color: #b45309;
}

.checkin-tip {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  font-size: 14px;
}

.result-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  text-align: left;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.18);
  color: #065f46;
}

.result-card.late {
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.22);
  color: #92400e;
}

.result-card strong {
  font-size: 15px;
}

.result-card span {
  font-size: 13px;
  line-height: 1.6;
}

.status-message {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
  font-size: 14px;
  line-height: 1.6;
}

.status-message.success {
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
}

.status-message.error {
  background: rgba(239, 68, 68, 0.1);
  color: #b91c1c;
}

.checkin-btn {
  height: 50px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(180deg, #2f6bff 0%, #1d4ed8 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 16px 30px rgba(29, 78, 216, 0.26);
}

.checkin-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
}

@media (max-width: 480px) {
  .checkin-page {
    padding: 16px;
  }

  .checkin-card {
    width: min(540px, calc(100vw - 32px));
    padding: 24px 18px;
  }

  .checkin-card h1 {
    font-size: 28px;
  }

  .info-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-value {
    text-align: left;
  }
}
</style>
