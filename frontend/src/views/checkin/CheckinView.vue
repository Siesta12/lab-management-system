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
      <p v-if="statusMessage" class="status-message">{{ statusMessage }}</p>

      <button type="button" class="checkin-btn" :disabled="!labIdentifier || !labInfo || submitting" @click="handleCheckin">
        {{ submitting ? '正在签到...' : '立即签到' }}
      </button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { fetchDepartmentById } from '../../api/departments';
import { fetchLabById, fetchLabs } from '../../api/labs';
import { submitCheckin } from '../../api/checkin';
import type { LabDto } from '../../types';

const route = useRoute();
const currentTime = ref('');
const labInfo = ref<LabDto | null>(null);
const departmentName = ref('');
const statusMessage = ref('');
const submitting = ref(false);
let timerId: number | undefined;

const labIdentifier = computed(() => {
  const value = route.query.lab_id;
  if (Array.isArray(value)) {
    return value[0] ?? '';
  }
  return typeof value === 'string' ? value.trim() : '';
});

const labRoomLabel = computed(() => {
  if (!labInfo.value) {
    return '加载中...';
  }
  return [labInfo.value.buildingName, labInfo.value.roomNo].filter(Boolean).join(' / ') || '未设置';
});

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

function isNumericIdentifier(value: string): boolean {
  return /^\d+$/.test(value);
}

async function loadLabInfo(): Promise<void> {
  labInfo.value = null;
  departmentName.value = '';

  if (!labIdentifier.value) {
    statusMessage.value = '未获取到实验室信息';
    return;
  }

  try {
    let lab: LabDto | null = null;
    if (isNumericIdentifier(labIdentifier.value)) {
      lab = await fetchLabById(Number(labIdentifier.value));
    } else {
      const result = await fetchLabs(
        { pageNum: 1, pageSize: 1, labCode: labIdentifier.value },
      );
      lab = result.list[0] ?? null;
    }

    if (!lab) {
      statusMessage.value = '未找到对应的实验室信息';
      return;
    }

    labInfo.value = lab;
    statusMessage.value = '';

    if (lab.departmentId != null) {
      try {
        const department = await fetchDepartmentById(lab.departmentId);
        departmentName.value = department.departmentName;
      } catch {
        departmentName.value = `学院ID ${lab.departmentId}`;
      }
    }
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '实验室信息加载失败';
  }
}

function readPosition(): Promise<{ latitude: number | null; longitude: number | null; accuracy: number | null }> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({ latitude: null, longitude: null, accuracy: null });
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
      () => {
        resolve({ latitude: null, longitude: null, accuracy: null });
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
  if (!labIdentifier.value) {
    statusMessage.value = '未获取到实验室信息';
    return;
  }

  submitting.value = true;
  try {
    const location = await readPosition();
    console.log('点击签到', labIdentifier.value, location);
    await submitCheckin({
      labIdentifier: labIdentifier.value,
      latitude: location.latitude,
      longitude: location.longitude,
      accuracy: location.accuracy,
      capturedAt: new Date().toISOString(),
      userAgent: navigator.userAgent,
    });
    statusMessage.value = location.latitude != null && location.longitude != null
      ? '签到信息已提交'
      : '签到信息已提交，但未获取到定位';
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '签到提交失败';
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
  console.log('当前 lab_id:', labIdentifier.value || null);
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

.status-message {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
  font-size: 14px;
  line-height: 1.6;
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
