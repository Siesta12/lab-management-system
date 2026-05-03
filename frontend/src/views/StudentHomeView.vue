<template>
  <section class="content-grid student-grid student-main-stack">
    <BasePanel tag="今日安排" title="今日安排">
      <div v-if="loading" class="empty-state muted">
        <strong>正在加载学生首页数据...</strong>
        <span>请稍候，正在整理预约与信用信息。</span>
      </div>

      <div v-else-if="todayReservation" class="agenda-card">
        <div class="agenda-head">
          <div class="agenda-title">
            <h4>{{ getLabName(todayReservation.labId) }}</h4>
            <span class="agenda-subtitle">今日预约</span>
          </div>
          <span class="status-pill" :class="statusTone(todayReservation.status)">
            {{ reservationStatusText(todayReservation.status) }}
          </span>
        </div>

        <div class="agenda-meta">
          <div>
            <span>预约日期</span>
            <strong>{{ firstSlot(todayReservation)?.reservationDate ?? '--' }}</strong>
          </div>
          <div>
            <span>预约节次 / 时间段</span>
            <strong>{{ reservationSlotsText(todayReservation) }}</strong>
          </div>
          <div>
            <span>签到状态</span>
            <strong>{{ checkInStatusText(todayReservation) }}</strong>
          </div>
        </div>

        <p class="agenda-note">
          {{ agendaNote(todayReservation) }}
        </p>

        <div class="action-row">
          <RouterLink :to="reservationDetailLink(todayReservation)" class="action-btn secondary">
            查看详情
          </RouterLink>
          <RouterLink
            v-if="canCheckIn(todayReservation)"
            :to="checkinLink(todayReservation)"
            class="action-btn primary"
          >
            去签到
          </RouterLink>
        </div>
      </div>

      <div v-else class="empty-state">
        <strong>今日暂无预约，可前往实验室查询进行预约</strong>
        <span>你可以先查看实验室开放情况，再按节次提交预约申请。</span>
      </div>
    </BasePanel>

    <BasePanel tag="提醒事项" title="提醒事项">
      <div v-if="loading" class="empty-state muted">
        <strong>正在计算待处理事项...</strong>
        <span>会根据你的预约、签到与信用情况自动整理。</span>
      </div>

      <div v-else-if="pendingItems.length" class="task-list">
        <RouterLink v-for="item in pendingItems" :key="item.key" class="task-item" :to="item.to">
          <div class="task-head">
            <span class="task-tag" :class="item.tone">{{ item.title }}</span>
            <strong>{{ item.summary }}</strong>
          </div>
          <p>{{ item.detail }}</p>
        </RouterLink>
      </div>

      <div v-else class="empty-state">
        <strong>暂无待处理事项，保持良好使用记录。</strong>
        <span>继续按时签到、及时关注审批结果即可。</span>
      </div>
    </BasePanel>
  </section>

</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { fetchLabs } from '../api/labs';
import { fetchMyReservations } from '../api/reservations';
import { fetchMyProfile } from '../api/users';
import BasePanel from '../components/BasePanel.vue';
import { useAuthStore } from '../stores/auth';
import type { LabDto, ReservationDto, ReservationSlotDto, UserVO } from '../types';

type StatusTone = 'brand' | 'success' | 'warning' | 'danger' | 'neutral';

interface PendingItem {
  key: string;
  title: string;
  summary: string;
  detail: string;
  tone: StatusTone;
  to: string | { path: string; query: Record<string, string | number> };
}

const auth = useAuthStore();
const profile = ref<UserVO | null>(null);
const reservations = ref<ReservationDto[]>([]);
const labs = ref<LabDto[]>([]);
const loading = ref(true);

const pendingCount = computed(() => reservations.value.filter((item) => item.status === 1).length);

const labMap = computed(() => {
  const map = new Map<number, string>();
  labs.value.forEach((lab) => {
    map.set(lab.id, lab.labName);
  });
  return map;
});

const todayKey = computed(() => formatDateOnly(new Date()));

const sortedReservations = computed(() =>
  [...reservations.value].sort((left, right) => compareReservations(left, right)),
);

const todayReservation = computed(() =>
  sortedReservations.value.find((item) => {
    if (![1, 2, 5].includes(item.status)) {
      return false;
    }
    const slot = firstSlot(item);
    return slot?.reservationDate === todayKey.value;
  }) ?? null,
);

const upcomingReservation = computed(() =>
  sortedReservations.value.find((item) => {
    if (item.status !== 2) {
      return false;
    }
    const slot = firstSlot(item);
    if (!slot?.reservationDate) {
      return false;
    }
    return slot.reservationDate >= todayKey.value;
  }) ?? null,
);

const riskReservations = computed(() =>
  sortedReservations.value.filter((item) => {
    if (item.status !== 2 || item.checkInTime) {
      return false;
    }
    const slot = firstSlot(item);
    return Boolean(slot?.reservationDate && slot.reservationDate <= todayKey.value);
  }),
);

const pendingItems = computed<PendingItem[]>(() => {
  const items: PendingItem[] = [];

  if (pendingCount.value > 0) {
    items.push({
      key: 'pending',
      title: '待审批预约',
      summary: `${pendingCount.value} 条待审批`,
      detail: '请关注预约审核结果，审核通过后才能进入签到流程。',
      tone: 'warning',
      to: { path: '/admin/my-reservations', query: { status: '1' } },
    });
  }

  if (upcomingReservation.value) {
    const slot = firstSlot(upcomingReservation.value);
    items.push({
      key: 'upcoming',
      title: '即将开始的预约',
      summary: `${getLabName(upcomingReservation.value.labId)} · ${slot?.reservationDate ?? '--'}`,
      detail: `节次：${reservationSlotsText(upcomingReservation.value)}，请提前确认实验室与时间安排。`,
      tone: 'brand',
      to: reservationDetailLink(upcomingReservation.value),
    });
  }

  if (riskReservations.value.length) {
    const item = riskReservations.value[0];
    const slot = firstSlot(item);
    items.push({
      key: 'risk',
      title: '签到风险',
      summary: `${getLabName(item.labId)} · ${slot?.reservationDate ?? '--'}`,
      detail: item.checkInTime
        ? '你已经签到，可以继续关注后续使用安排。'
        : '当前预约尚未签到，若已到场请尽快完成签到，避免爽约风险。',
      tone: 'danger',
      to: reservationDetailLink(item),
    });
  }

  if ((profile.value?.creditScore ?? 100) < 80 || (profile.value?.violationCount ?? 0) > 0) {
    items.push({
      key: 'credit',
      title: '信用提醒',
      summary: `信用分 ${profile.value?.creditScore ?? '--'} 分`,
      detail:
        (profile.value?.creditScore ?? 100) < 80
          ? '信用分低于 80，请注意违规、迟到和爽约对后续预约的影响。'
          : `当前违规次数为 ${profile.value?.violationCount ?? 0} 次，请继续保持良好记录。`,
      tone: (profile.value?.creditScore ?? 100) < 80 ? 'warning' : 'neutral',
      to: '/admin/my-credit',
    });
  }

  return items.slice(0, 4);
});

function getLabName(labId: number): string {
  return labMap.value.get(labId) ?? `实验室${labId}`;
}

function reservationStatusText(status: number): string {
  if (status === 2) {
    return '已通过';
  }
  if (status === 3) {
    return '已驳回';
  }
  if (status === 4) {
    return '已取消';
  }
  if (status === 5) {
    return '已完成';
  }
  return '待审批';
}

function statusTone(status: number): StatusTone {
  if (status === 2 || status === 5) {
    return 'success';
  }
  if (status === 1) {
    return 'warning';
  }
  if (status === 3 || status === 4) {
    return 'danger';
  }
  return 'neutral';
}

function checkInStatusText(item: ReservationDto): string {
  if (item.checkInTime) {
    return '已签到';
  }
  if (item.status === 5) {
    return '已完成';
  }
  if (item.status === 2) {
    return '未签到';
  }
  if (item.status === 1) {
    return '待审批';
  }
  if (item.status === 3) {
    return '已驳回';
  }
  if (item.status === 4) {
    return '已取消';
  }
  return '--';
}

function canCheckIn(item: ReservationDto): boolean {
  const slot = firstSlot(item);
  return item.status === 2 && !item.checkInTime && slot?.reservationDate === todayKey.value;
}

function reservationDetailLink(item: ReservationDto): { path: string; query: { reservationId: number } } {
  return {
    path: '/admin/my-reservations',
    query: {
      reservationId: item.id,
    },
  };
}

function checkinLink(item: ReservationDto): { path: string; query: { lab_id: number } } {
  return {
    path: '/checkin',
    query: {
      lab_id: item.labId,
    },
  };
}

function agendaNote(item: ReservationDto): string {
  if (item.checkInTime) {
    return '本次预约已签到，后续可在“我的预约”中查看完整记录。';
  }
  if (item.status === 2) {
    return '如果你已经到达实验室，可以直接去签到。';
  }
  if (item.status === 1) {
    return '该预约仍在审批中，建议先关注审核结果。';
  }
  if (item.status === 5) {
    return '本次预约已完成，可以继续查看历史记录。';
  }
  return '你可以继续查看详情，了解预约的完整状态。';
}

function reservationSlotsText(item: ReservationDto): string {
  const slots = displaySlots(item);
  if (!slots.length) {
    return '--';
  }
  const sameDate = slots.every((slot) => slot.reservationDate === slots[0].reservationDate);
  const periodText = slots
    .map((slot) => slot.periodName || `第${slot.periodNo || slot.periodId}节`)
    .filter(Boolean)
    .join('、');
  if (sameDate) {
    return periodText || '--';
  }
  return slots.map((slot) => `${slot.reservationDate} ${slot.periodName || `第${slot.periodNo || slot.periodId}节`}`).join('、');
}

function displaySlots(item: ReservationDto): ReservationSlotDto[] {
  const activeSlots = (item.slots ?? []).filter((slot) => slot.slotStatus === 1);
  const source = activeSlots.length ? activeSlots : item.slots ?? [];
  return [...source].sort((left, right) => {
    const dateCompare = left.reservationDate.localeCompare(right.reservationDate);
    if (dateCompare !== 0) {
      return dateCompare;
    }
    return (left.periodNo || left.periodId || 0) - (right.periodNo || right.periodId || 0);
  });
}

function firstSlot(item: ReservationDto): ReservationSlotDto | undefined {
  return displaySlots(item)[0];
}

function compareReservations(left: ReservationDto, right: ReservationDto): number {
  const leftSlot = firstSlot(left);
  const rightSlot = firstSlot(right);
  if (!leftSlot?.reservationDate && !rightSlot?.reservationDate) {
    return right.id - left.id;
  }
  if (!leftSlot?.reservationDate) {
    return 1;
  }
  if (!rightSlot?.reservationDate) {
    return -1;
  }
  const dateCompare = leftSlot.reservationDate.localeCompare(rightSlot.reservationDate);
  if (dateCompare !== 0) {
    return dateCompare;
  }
  const leftPeriod = leftSlot.periodNo || leftSlot.periodId || 0;
  const rightPeriod = rightSlot.periodNo || rightSlot.periodId || 0;
  if (leftPeriod !== rightPeriod) {
    return leftPeriod - rightPeriod;
  }
  return right.id - left.id;
}

function formatDateOnly(value: Date): string {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

async function loadHomeData(): Promise<void> {
  loading.value = true;
  try {
    const [profileResult, reservationResult, labResult] = await Promise.allSettled([
      fetchMyProfile(auth.token.value),
      fetchMyReservations({ pageNum: 1, pageSize: 200 }, auth.token.value),
      fetchLabs({ pageNum: 1, pageSize: 200 }, auth.token.value),
    ]);

    profile.value = profileResult.status === 'fulfilled' ? profileResult.value : null;
    reservations.value = reservationResult.status === 'fulfilled' ? reservationResult.value.list ?? [] : [];
    labs.value = labResult.status === 'fulfilled' ? labResult.value.list ?? [] : [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadHomeData();
});
</script>

<style scoped>
.student-grid {
  margin-top: 2px;
  align-items: start;
}

.student-main-stack {
  grid-template-columns: 1fr;
}

.agenda-card,
.task-item,
.quick-action,
.overview-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
}

.agenda-card {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.agenda-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.agenda-title {
  display: grid;
  gap: 6px;
}

.agenda-title h4 {
  margin: 0;
  font-size: 22px;
  line-height: 1.2;
  color: #0f172a;
}

.agenda-subtitle {
  color: #64748b;
  font-size: 13px;
}

.agenda-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.agenda-meta div {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.agenda-meta span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.agenda-meta strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.5;
}

.agenda-note {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-pill.brand {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.status-pill.success {
  background: rgba(15, 140, 127, 0.12);
  color: #0f8c7f;
}

.status-pill.warning {
  background: rgba(245, 158, 11, 0.16);
  color: #9a6700;
}

.status-pill.danger {
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
}

.status-pill.neutral {
  background: rgba(148, 163, 184, 0.16);
  color: #64748b;
}

.task-list {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.task-item {
  padding: 16px 18px;
  display: grid;
  gap: 10px;
  color: inherit;
  text-decoration: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.task-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.task-item:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.22);
  outline-offset: 3px;
}

.task-head {
  display: grid;
  gap: 8px;
}

.task-head strong {
  color: #0f172a;
  font-size: 15px;
}

.task-item p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.task-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.task-tag.brand {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.task-tag.success {
  background: rgba(15, 140, 127, 0.12);
  color: #0f8c7f;
}

.task-tag.warning {
  background: rgba(245, 158, 11, 0.16);
  color: #9a6700;
}

.task-tag.danger {
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
}

.task-tag.neutral {
  background: rgba(148, 163, 184, 0.16);
  color: #64748b;
}

.empty-state {
  display: grid;
  gap: 8px;
  align-items: start;
  padding: 10px 2px 2px;
  color: #334155;
}

.student-grid .panel {
  height: auto;
}

.empty-state strong {
  color: #0f172a;
  font-size: 15px;
}

.empty-state span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.empty-state.muted strong,
.empty-state.muted span {
  color: #64748b;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-row.compact {
  margin-top: 4px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 700;
  font-size: 14px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.action-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.22);
}

.action-btn.secondary {
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  border: 1px solid rgba(37, 99, 235, 0.16);
}

.action-btn.ghost {
  background: rgba(255, 255, 255, 0.82);
  color: #0f172a;
  border: 1px solid rgba(148, 163, 184, 0.24);
}

@media (max-width: 1100px) {
  .student-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .task-list {
    grid-template-columns: 1fr;
  }

  .agenda-meta {
    grid-template-columns: 1fr;
  }

  .agenda-head {
    flex-direction: column;
  }

  .status-pill {
    align-self: flex-start;
  }
}
</style>
