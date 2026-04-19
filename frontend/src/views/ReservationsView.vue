<template>
  <section class="content-grid sidebar-layout">
    <BasePanel tag="预约申请" title="冲突检测与推荐" panel-class="form-panel">
      <form class="stack-form" @submit.prevent="handleSubmit">
        <label>
          <span>实验室</span>
          <select v-model.number="form.labId">
            <option v-for="lab in labOptions" :key="lab.id" :value="lab.id">{{ lab.name }}</option>
          </select>
        </label>
        <label><span>预约日期</span><input v-model="form.reservationDate" type="date" /></label>
        <label><span>开始时间</span><input v-model="form.startClock" type="time" /></label>
        <label><span>结束时间</span><input v-model="form.endClock" type="time" /></label>
        <label>
          <span>预约类型</span>
          <select v-model.number="form.reservationType">
            <option :value="1">教学预约</option>
            <option :value="2">科研预约</option>
            <option :value="3">个人预约</option>
          </select>
        </label>
        <label><span>用途说明</span><input v-model="form.usagePurpose" /></label>
        <label><span>课程 / 项目名称</span><input v-model="form.courseOrProjectName" /></label>
        <label><span>参与人数</span><input v-model.number="form.participantCount" type="number" min="1" /></label>
        <label><span>联系电话</span><input v-model="form.contactPhone" /></label>
        <div class="button-row">
          <button type="button" class="ghost-btn wide" @click="handleCheck">冲突检测</button>
          <button type="submit" class="primary-btn wide">提交申请</button>
        </div>
      </form>

      <p v-if="formMessage" class="info-text">{{ formMessage }}</p>

      <div class="recommendation-box">
        <h4>推荐时间段</h4>
        <div v-for="item in timeItems" :key="item.range" class="mini-card">
          <strong>{{ item.range }}</strong>
          <span>{{ item.note }}</span>
        </div>
        <h4>推荐实验室</h4>
        <div v-for="item in labItems" :key="item.name" class="mini-card">
          <strong>{{ item.name }}</strong>
          <span>{{ item.location }} · {{ item.reason }}</span>
        </div>
      </div>
    </BasePanel>

    <BasePanel tag="预约列表" title="预约申请与审核状态" :note="`共 ${reservationState.total} 条`">
      <div class="toolbar">
        <button type="button" class="ghost-btn" @click="loadReservations">刷新列表</button>
      </div>
      <p v-if="tableMessage" class="info-text">{{ tableMessage }}</p>
      <BaseTable :headers="['申请人', '实验室', '日期', '时间段', '类型', '优先级', '状态']">
        <tr v-for="item in displayReservations" :key="item.id">
          <td>{{ item.applicant }}</td>
          <td>{{ item.labName }}</td>
          <td>{{ item.date }}</td>
          <td>{{ item.timeRange }}</td>
          <td>{{ item.type }}</td>
          <td>{{ item.priority }}</td>
          <td><span :class="getBadgeClass(item.status)">{{ item.status }}</span></td>
        </tr>
      </BaseTable>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  checkReservationConflict,
  createReservation,
  fetchMyReservations,
  fetchReservations,
  recommendLabs,
  recommendTimes,
} from '../api/reservations';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { labRecommendations as fallbackLabRecommendations, labs, reservations as fallbackReservations, timeRecommendations as fallbackTimeRecommendations } from '../data/mock';
import { useAuthStore } from '../stores/auth';
import type { LabRecommendation, ReservationCreatePayload, ReservationDto, ReservationItem, TimeRecommendation } from '../types';
import { getBadgeClass } from '../utils/format';

const auth = useAuthStore();

const labOptions = computed(() => labs);
const form = reactive({
  labId: labs[0]?.id ?? 1,
  reservationDate: '2026-04-20',
  startClock: '08:00',
  endClock: '10:00',
  reservationType: 1,
  usagePurpose: '课程实验',
  courseOrProjectName: '软件工程实验',
  participantCount: 32,
  contactPhone: '13800000000',
});

const reservationState = ref<{ list: ReservationItem[]; total: number }>({
  list: fallbackReservations,
  total: fallbackReservations.length,
});
const timeItems = ref<TimeRecommendation[]>(fallbackTimeRecommendations);
const labItems = ref<LabRecommendation[]>(fallbackLabRecommendations);
const formMessage = ref('');
const tableMessage = ref('');

const displayReservations = computed(() => reservationState.value.list);

function typeText(value: number): ReservationItem['type'] {
  if (value === 1) return '教学预约';
  if (value === 2) return '科研预约';
  return '个人预约';
}

function priorityText(value: number): ReservationItem['priority'] {
  if (value === 1) return '高';
  if (value === 2) return '中';
  return '低';
}

function statusText(value: number): ReservationItem['status'] {
  if (value === 2) return '已通过';
  if (value === 3) return '已驳回';
  if (value === 4) return '已取消';
  return '待审核';
}

function buildDateTime(date: string, time: string): string {
  return `${date} ${time}:00`;
}

function buildPayload(): ReservationCreatePayload {
  return {
    labId: form.labId,
    reservationType: form.reservationType,
    reservationDate: form.reservationDate,
    startTime: buildDateTime(form.reservationDate, form.startClock),
    endTime: buildDateTime(form.reservationDate, form.endClock),
    usagePurpose: form.usagePurpose,
    courseOrProjectName: form.courseOrProjectName,
    participantCount: form.participantCount,
    contactPhone: form.contactPhone,
  };
}

function mapReservation(dto: ReservationDto): ReservationItem {
  const matchedLab = labs.find((item) => item.id === dto.labId);
  return {
    id: dto.id,
    applicant: `用户 #${dto.applicantUserId}`,
    labName: matchedLab?.name ?? `实验室 #${dto.labId}`,
    date: dto.reservationDate,
    timeRange: `${dto.startTime.slice(11, 16)} - ${dto.endTime.slice(11, 16)}`,
    type: typeText(dto.reservationType),
    priority: priorityText(dto.priorityLevel),
    status: statusText(dto.status),
  };
}

async function loadReservations(): Promise<void> {
  try {
    const data = auth.token.value
      ? await fetchMyReservations({ pageNum: 1, pageSize: 10 }, auth.token.value)
      : await fetchReservations({ pageNum: 1, pageSize: 10 });

    reservationState.value = {
      list: data.list.map(mapReservation),
      total: data.total,
    };
    tableMessage.value = auth.token.value ? '已加载当前登录用户的预约记录。' : '未登录时展示公共预约列表。';
  } catch (error) {
    reservationState.value = {
      list: fallbackReservations,
      total: fallbackReservations.length,
    };
    tableMessage.value = error instanceof Error ? `后端请求失败，当前显示演示数据：${error.message}` : '后端请求失败，当前显示演示数据。';
  }
}

async function handleCheck(): Promise<void> {
  const payload = buildPayload();
  try {
    const conflict = await checkReservationConflict(payload, auth.token.value || undefined);
    const times = await recommendTimes(payload, auth.token.value || undefined);
    const labsData = await recommendLabs(payload, auth.token.value || undefined);

    timeItems.value = times.map((item) => ({
      range: `${item.startTime} - ${item.endTime}`,
      note: item.reason,
    }));
    labItems.value = labsData.map((item) => ({
      name: item.labName,
      location: [item.buildingName, item.roomNo].filter(Boolean).join(' / ') || '位置待补充',
      reason: item.reason,
    }));
    formMessage.value = conflict.message;
  } catch (error) {
    timeItems.value = fallbackTimeRecommendations;
    labItems.value = fallbackLabRecommendations;
    formMessage.value = error instanceof Error ? `冲突检测失败，已回退为演示数据：${error.message}` : '冲突检测失败，已回退为演示数据。';
  }
}

async function handleSubmit(): Promise<void> {
  if (!auth.token.value) {
    formMessage.value = '请先登录后再提交预约。';
    return;
  }

  try {
    await createReservation(buildPayload(), auth.token.value);
    formMessage.value = '预约提交成功，已刷新列表。';
    await loadReservations();
  } catch (error) {
    formMessage.value = error instanceof Error ? error.message : '预约提交失败，请稍后重试。';
  }
}

onMounted(() => {
  void loadReservations();
});
</script>
