<template>
  <div class="teacher-home">
    <section class="content-grid teacher-grid teacher-main-stack">
      <BasePanel tag="今日安排" title="今日安排">
        <div v-if="loading" class="empty-state muted">
          <strong>正在加载教师首页数据...</strong>
          <span>请稍候，正在整理预约、实验室与报告信息。</span>
        </div>

        <div v-else-if="todayReservation" class="agenda-card">
          <div class="agenda-head">
            <div class="agenda-title">
              <h4>{{ getLabName(todayReservation.labId) }}</h4>
              <span class="agenda-subtitle">{{ reservationTypeText(todayReservation.reservationType) }}</span>
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
              <span>预约状态</span>
              <strong>{{ reservationStatusText(todayReservation.status) }}</strong>
            </div>
          </div>

          <p class="agenda-note">
            {{ agendaNote(todayReservation) }}
          </p>

          <div class="action-row">
            <RouterLink :to="reservationDetailLink(todayReservation)" class="action-btn secondary">
              查看详情
            </RouterLink>
          </div>
        </div>

        <div v-else class="empty-state">
          <strong>今日暂无预约，可前往实验室预约提交申请</strong>
          <span>你可以先查看实验室开放情况，再按节次选择空闲格子。</span>
          <div class="action-row compact">
            <RouterLink to="/admin/experiment-reports" class="action-btn secondary">报告审核</RouterLink>
          </div>
        </div>
      </BasePanel>

      <BasePanel tag="提醒事项" title="提醒事项">
        <div v-if="loading" class="empty-state muted">
          <strong>正在计算待处理事项...</strong>
          <span>会根据你的预约和报告状态自动整理。</span>
        </div>

        <div v-else-if="pendingItems.length" class="task-list">
          <RouterLink
            v-for="item in pendingItems"
            :key="item.key"
            class="task-item"
            :to="item.to"
          >
            <div class="task-head">
              <span class="task-tag" :class="item.tone">{{ item.title }}</span>
              <strong>{{ item.summary }}</strong>
            </div>
            <p>{{ item.detail }}</p>
          </RouterLink>
        </div>

        <div v-else class="empty-state">
          <strong>暂无待处理事项，今天的工作台很清爽。</strong>
          <span>继续关注预约审批结果和学生报告提交情况即可。</span>
        </div>
      </BasePanel>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { fetchExperimentReports } from '../api/experimentReports';
import { fetchLabs } from '../api/labs';
import { fetchMyReservations } from '../api/reservations';
import BasePanel from '../components/BasePanel.vue';
import { useAuthStore } from '../stores/auth';
import type { ExperimentReportDto, LabDto, ReservationDto, ReservationSlotDto } from '../types';

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
const reservations = ref<ReservationDto[]>([]);
const reports = ref<ExperimentReportDto[]>([]);
const labs = ref<LabDto[]>([]);
const loading = ref(true);

const pendingReservationCount = computed(() => reservations.value.filter((item) => item.status === 1).length);
const pendingReportCount = computed(() => reports.value.filter((item) => item.status === 2).length);

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
    return displaySlots(item).some((slot) => slot.reservationDate === todayKey.value);
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

const pendingItems = computed<PendingItem[]>(() => {
  const items: PendingItem[] = [];

  if (pendingReportCount.value > 0) {
    const report = reports.value.find((item) => item.status === 2);
    items.push({
      key: 'reports',
      title: '报告审核',
      summary: `${pendingReportCount.value} 份报告待审核`,
      detail: report
        ? `${report.studentName ?? '学生'} 提交了《${report.experimentName}》，建议及时处理。`
        : '学生提交后的实验报告会在这里形成待办。',
      tone: 'success',
      to: { path: '/admin/experiment-reports', query: { status: '2' } },
    });
  }

  if (pendingReservationCount.value > 0) {
    items.push({
      key: 'pending-reservations',
      title: '待审批预约',
      summary: `${pendingReservationCount.value} 条预约待审批`,
      detail: '请关注审批状态，审核通过后才能进入实验室使用流程。',
      tone: 'warning',
      to: { path: '/admin/my-reservations', query: { status: '1' } },
    });
  }

  if (upcomingReservation.value) {
    const slot = firstSlot(upcomingReservation.value);
    items.push({
      key: 'upcoming',
      title: '即将开始',
      summary: `${getLabName(upcomingReservation.value.labId)} · ${slot?.reservationDate ?? '--'}`,
      detail: `类型：${reservationTypeText(upcomingReservation.value.reservationType)}，节次：${reservationSlotsText(upcomingReservation.value)}。`,
      tone: 'brand',
      to: reservationDetailLink(upcomingReservation.value),
    });
  }

  items.push({
    key: 'repair',
    title: '设备报修',
    summary: '发现设备异常可直接提交报修',
    detail: '进入设备查询页，选择本学院设备后提交报修单。',
    tone: 'neutral',
    to: '/admin/devices',
  });

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
    return '已拒绝';
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

function reservationTypeText(type: number): string {
  if (type === 1) {
    return '课程实验预约';
  }
  if (type === 2) {
    return '科研训练预约';
  }
  return '个人预约';
}

function agendaNote(item: ReservationDto): string {
  if (item.status === 1) {
    return '该预约仍在审批中，建议先关注审核结果。';
  }
  if (item.status === 2) {
    return '本次预约已通过，请提前确认实验室设备和课程安排。';
  }
  if (item.status === 5) {
    return '本次预约已完成，可以在“我的预约”中查看完整记录。';
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

function reservationDetailLink(item: ReservationDto): { path: string; query: { reservationId: number } } {
  return {
    path: '/admin/my-reservations',
    query: {
      reservationId: item.id,
    },
  };
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
    const [reservationResult, reportResult, labResult] = await Promise.allSettled([
      fetchMyReservations({ pageNum: 1, pageSize: 200 }, auth.token.value),
      fetchExperimentReports({ pageNum: 1, pageSize: 200 }, auth.token.value),
      fetchLabs({ pageNum: 1, pageSize: 200 }, auth.token.value),
    ]);

    reservations.value = reservationResult.status === 'fulfilled' ? reservationResult.value.list ?? [] : [];
    reports.value = reportResult.status === 'fulfilled' ? reportResult.value.list ?? [] : [];
    labs.value = labResult.status === 'fulfilled' ? labResult.value.list ?? [] : [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadHomeData();
});
</script>

<style>
.teacher-home {
  display: grid;
  gap: 0;
}

.teacher-home .teacher-grid {
  margin-top: 2px;
  align-items: start;
}

.teacher-home .teacher-main-stack {
  grid-template-columns: 1fr;
}

.teacher-home .agenda-card,
.teacher-home .task-item {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
}

.teacher-home .agenda-card {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.teacher-home .agenda-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.teacher-home .agenda-title {
  display: grid;
  gap: 6px;
}

.teacher-home .agenda-title h4 {
  margin: 0;
  font-size: 22px;
  line-height: 1.2;
  color: #0f172a;
}

.teacher-home .agenda-subtitle {
  color: #64748b;
  font-size: 13px;
}

.teacher-home .agenda-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.teacher-home .agenda-meta div {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.teacher-home .agenda-meta span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.teacher-home .agenda-meta strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.5;
}

.teacher-home .agenda-note {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}

.teacher-home .status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.teacher-home .status-pill.brand {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.teacher-home .status-pill.success {
  background: rgba(15, 140, 127, 0.12);
  color: #0f8c7f;
}

.teacher-home .status-pill.warning {
  background: rgba(245, 158, 11, 0.16);
  color: #9a6700;
}

.teacher-home .status-pill.danger {
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
}

.teacher-home .status-pill.neutral {
  background: rgba(148, 163, 184, 0.16);
  color: #64748b;
}

.teacher-home .task-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.teacher-home .task-item {
  display: grid;
  gap: 10px;
  padding: 16px 18px;
  color: inherit;
  text-decoration: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.teacher-home .task-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.teacher-home .task-item:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.22);
  outline-offset: 3px;
}

.teacher-home .task-head {
  display: grid;
  gap: 8px;
}

.teacher-home .task-head strong {
  color: #0f172a;
  font-size: 15px;
}

.teacher-home .task-item p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.teacher-home .task-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.teacher-home .task-tag.brand {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.teacher-home .task-tag.success {
  background: rgba(15, 140, 127, 0.12);
  color: #0f8c7f;
}

.teacher-home .task-tag.warning {
  background: rgba(245, 158, 11, 0.16);
  color: #9a6700;
}

.teacher-home .task-tag.danger {
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
}

.teacher-home .task-tag.neutral {
  background: rgba(148, 163, 184, 0.16);
  color: #64748b;
}

.teacher-home .empty-state {
  display: grid;
  gap: 8px;
  align-items: start;
  padding: 10px 2px 2px;
  color: #334155;
}

.teacher-home .empty-state strong {
  color: #0f172a;
  font-size: 15px;
}

.teacher-home .empty-state span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.teacher-home .empty-state.muted strong,
.teacher-home .empty-state.muted span {
  color: #64748b;
}

.teacher-home .action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.teacher-home .action-row.compact {
  margin-top: 4px;
}

.teacher-home .action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.teacher-home .action-btn:hover {
  transform: translateY(-1px);
}

.teacher-home .action-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.22);
}

.teacher-home .action-btn.secondary {
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  border: 1px solid rgba(37, 99, 235, 0.16);
}

.teacher-home .action-btn.ghost {
  background: rgba(255, 255, 255, 0.82);
  color: #0f172a;
  border: 1px solid rgba(148, 163, 184, 0.24);
}

@media (max-width: 1100px) {
  .teacher-home .teacher-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .teacher-home .task-list,
  .teacher-home .agenda-meta {
    grid-template-columns: 1fr;
  }

  .teacher-home .agenda-head {
    flex-direction: column;
  }

  .teacher-home .status-pill {
    align-self: flex-start;
  }
}
</style>
