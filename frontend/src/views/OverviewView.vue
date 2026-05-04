<template>
  <section class="card-grid metrics-grid">
    <button
      v-for="card in cardsToShow"
      :key="card.label"
      type="button"
      class="metric-card metric-card-button"
      :class="card.tone"
      @click="handleMetricCardClick(card.label)"
    >
      <span>{{ card.label }}</span>
      <strong>{{ card.value }}</strong>
      <small>{{ card.trend }}</small>
    </button>
  </section>

  <section class="content-grid overview-stack">
    <BasePanel tag="今日业务" title="每日预约概览" note="只保留核心预约指标">
      <template v-if="dashboard">
        <div class="overview-summary-grid">
          <button type="button" class="overview-summary-card overview-summary-card-total" @click="openTimelineDialog">
            <span class="summary-label">今日预约总数</span>
            <strong>{{ dashboard.todayOverview.total }}</strong>
            <small>点击查看今日预约时间轴</small>
          </button>

          <button
            type="button"
            class="overview-summary-card overview-summary-card-action overview-summary-card-pending"
            @click="goToReservations('pending')"
          >
            <span class="summary-label">待审核数量</span>
            <strong>{{ dashboard.todayOverview.pending }}</strong>
            <small>点击前往预约管理</small>
          </button>

          <button
            type="button"
            class="overview-summary-card overview-summary-card-action overview-summary-card-conflict"
            @click="goToReservations('conflict')"
          >
            <span class="summary-label">冲突数量</span>
            <strong>{{ dashboard.todayOverview.conflict }}</strong>
            <small>点击前往冲突预约管理</small>
          </button>
        </div>
      </template>
      <div v-else class="empty-state">正在加载每日预约概览...</div>
    </BasePanel>

    <BasePanel tag="占用情况" title="各类型实验室今日占用情况" note="按实验室类型汇总今日开放节次">
      <template v-if="occupancyItems.length">
        <div class="occupancy-type-list">
          <article v-for="group in occupancyItems" :key="group.labType" class="occupancy-type-card">
            <div class="occupancy-type-head">
              <div>
                <strong>{{ group.labType }}</strong>
                <p>今日已占用 {{ group.occupiedSlots }} / {{ group.totalOpenSlots }} 节次</p>
              </div>
              <span>{{ formatPercent(occupancyPercent(group.occupancyRate)) }}</span>
            </div>
            <div class="occupancy-bar">
              <i
                v-if="occupancyPercent(group.occupancyRate) > 0"
                :style="{ width: `${occupancyPercent(group.occupancyRate)}%` }"
              />
            </div>
            <small>共 {{ group.labCount }} 间实验室</small>
          </article>
        </div>
      </template>
      <div v-else class="empty-state">正在加载实验室类型占用情况...</div>
    </BasePanel>
  </section>

  <div v-if="timelineDialogVisible" class="timeline-dialog-mask" @click.self="closeTimelineDialog">
    <div class="timeline-dialog" role="dialog" aria-modal="true" aria-labelledby="timeline-dialog-title">
      <div class="timeline-dialog-head">
        <div>
          <span class="dialog-tag">今日预约时间轴</span>
          <h3 id="timeline-dialog-title">今日预约时间轴</h3>
        </div>
        <button type="button" class="dialog-close" @click="closeTimelineDialog">关闭</button>
      </div>

      <div class="timeline-dialog-body">
        <article
          v-for="item in pagedTimelineItems"
          :key="`${item.reservationId}-${item.labId}-${item.periodId}`"
          class="timeline-item"
        >
          <div class="timeline-time">{{ item.timeRange || '--' }}</div>
          <div class="timeline-body">
            <div class="timeline-title-row">
              <strong>{{ item.labName }}</strong>
              <span class="timeline-type" :class="typeClass(item.typeLabel)">{{ item.typeLabel }}</span>
            </div>
            <p>{{ item.applicantName }} · {{ item.note }}</p>
          </div>
          <span class="timeline-status" :class="statusClass(item.statusLabel)">{{ item.statusLabel }}</span>
        </article>
        <div v-if="!timelineItems.length" class="empty-state">今日暂无预约时间轴数据。</div>
      </div>

      <div v-if="timelineItems.length" class="timeline-dialog-footer">
        <div class="timeline-dialog-count">共 {{ timelineItems.length }} 条，当前第 {{ timelinePage }} / {{ timelinePageCount }} 页</div>
        <div class="timeline-dialog-pager">
          <button type="button" class="pager-btn" :disabled="timelinePage === 1" @click="timelinePage--">上一页</button>
          <button type="button" class="pager-btn" :disabled="timelinePage === timelinePageCount" @click="timelinePage++">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAdminDashboard } from '../api/dashboard';
import BasePanel from '../components/BasePanel.vue';
import type { AdminDashboardDto, DashboardCardDto } from '../types';
import { useAuthStore } from '../stores/auth';
import { formatPercent } from '../utils/format';

const auth = useAuthStore();
const router = useRouter();

const dashboard = ref<AdminDashboardDto | null>(null);
const timelineDialogVisible = ref(false);
const timelinePage = ref(1);
const timelinePageSize = 5;

const fallbackCards: DashboardCardDto[] = [
  { label: '开放实验室', value: '--', trend: '当前可预约实验室', tone: 'success' },
  { label: '待审核申请', value: '--', trend: '等待管理员审核', tone: 'warning' },
  { label: '低库存耗材', value: '--', trend: '需要补货提醒', tone: 'accent' },
  { label: '设备维护中', value: '--', trend: '需要关注设备状态', tone: 'brand' },
];

const cardsToShow = computed(() => dashboard.value?.cards?.length ? dashboard.value.cards : fallbackCards);
const timelineItems = computed(() => dashboard.value?.todayTimeline ?? []);
const occupancyItems = computed(() => dashboard.value?.occupancyRates ?? []);
const timelinePageCount = computed(() => Math.max(1, Math.ceil(timelineItems.value.length / timelinePageSize)));

const pagedTimelineItems = computed(() => {
  const start = (timelinePage.value - 1) * timelinePageSize;
  return timelineItems.value.slice(start, start + timelinePageSize);
});

const metricCardRouteMap: Record<string, { name: string; query?: Record<string, string> }> = {
  开放实验室: { name: 'labs' },
  待审核申请: { name: 'reservations', query: { view: 'list', status: '1' } },
  低库存耗材: { name: 'consumables', query: { status: '2' } },
  设备维护中: { name: 'devices', query: { tab: 'repair' } },
};

function openTimelineDialog(): void {
  timelinePage.value = 1;
  timelineDialogVisible.value = true;
}

function closeTimelineDialog(): void {
  timelineDialogVisible.value = false;
}

function goToReservations(view: 'pending' | 'conflict'): void {
  if (view === 'conflict') {
    void router.push({ name: 'reservations', query: { view: 'conflict' } });
    return;
  }
  void router.push({ name: 'reservations', query: { view: 'list', status: '1' } });
}

function handleMetricCardClick(label: string): void {
  const target = metricCardRouteMap[label];
  if (!target) {
    return;
  }
  void router.push({ name: target.name, query: target.query });
}

function typeClass(typeLabel?: string): string {
  if (typeLabel === '课程实验') {
    return 'type-teach';
  }
  if (typeLabel === '科研训练') {
    return 'type-research';
  }
  if (typeLabel === '维护') {
    return 'type-maintenance';
  }
  return 'type-personal';
}

function occupancyPercent(rate: number): number {
  return Math.max(0, Math.round(rate * 100));
}

function statusClass(statusLabel?: string): string {
  if (statusLabel === '已通过' || statusLabel === '已完成') {
    return 'status-running';
  }
  if (statusLabel === '即将开始') {
    return 'status-upcoming';
  }
  if (statusLabel === '冲突待处理') {
    return 'status-warning';
  }
  return 'status-pending';
}

async function loadDashboard(): Promise<void> {
  try {
    dashboard.value = await fetchAdminDashboard(auth.token.value || '');
  } catch {
    dashboard.value = null;
  }
}

onMounted(() => {
  void loadDashboard();
});
</script>

<style scoped>
.metric-card-button {
  border: 0;
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}

.metric-card-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
  filter: saturate(1.04);
}

.metric-card-button:focus-visible {
  outline: 3px solid rgba(59, 130, 246, 0.28);
  outline-offset: 2px;
}

.overview-stack {
  display: grid;
  gap: 16px;
}

.overview-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.overview-summary-card {
  border: 0;
  border-radius: 18px;
  padding: 18px 20px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.94), rgba(241, 245, 249, 0.88));
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.04);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.overview-summary-card:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.22);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.overview-summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 34px;
  line-height: 1;
  color: #0f172a;
}

.summary-label {
  font-size: 13px;
  color: #475569;
  font-weight: 650;
}

.overview-summary-card small {
  display: block;
  margin-top: 10px;
  color: #64748b;
}

.overview-summary-card-total {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.12), rgba(59, 130, 246, 0.04));
  border-color: rgba(59, 130, 246, 0.18);
}

.overview-summary-card-total .summary-label {
  color: #1d4ed8;
}

.overview-summary-card-action {
  position: relative;
}

.overview-summary-card-action::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}

.overview-summary-card-pending {
  background: linear-gradient(180deg, rgba(251, 191, 36, 0.16), rgba(251, 191, 36, 0.05));
  border-color: rgba(245, 158, 11, 0.22);
}

.overview-summary-card-pending .summary-label {
  color: #b45309;
}

.overview-summary-card-pending strong {
  color: #92400e;
}

.overview-summary-card-conflict {
  background: linear-gradient(180deg, rgba(248, 113, 113, 0.16), rgba(248, 113, 113, 0.05));
  border-color: rgba(239, 68, 68, 0.22);
}

.overview-summary-card-conflict .summary-label {
  color: #b91c1c;
}

.overview-summary-card-conflict strong {
  color: #991b1b;
}

.occupancy-type-list {
  display: grid;
  gap: 12px;
}

.occupancy-type-card {
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.88);
}

.occupancy-type-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.occupancy-type-head strong {
  font-size: 16px;
  color: #0f172a;
}

.occupancy-type-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.occupancy-type-head span {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.occupancy-bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.occupancy-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa 0%, #22c55e 100%);
}

.occupancy-type-card small {
  display: block;
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.timeline-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(10px);
}

.timeline-dialog {
  width: min(1200px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  display: grid;
  gap: 18px;
  padding: 24px 24px 20px;
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.24);
}

.timeline-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dialog-tag {
  display: inline-flex;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.timeline-dialog-head h3 {
  margin: 10px 0 0;
  font-size: 22px;
  color: #0f172a;
}

.dialog-close {
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: #fff;
  color: #334155;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
}

.timeline-dialog-body {
  display: grid;
  gap: 14px;
  overflow: auto;
  padding-right: 4px;
}

.timeline-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 4px;
}

.timeline-dialog-count {
  color: #64748b;
  font-size: 13px;
}

.timeline-dialog-pager {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pager-btn {
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: #fff;
  color: #334155;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
}

.pager-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.timeline-item {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.86);
}

.timeline-time {
  font-weight: 700;
  font-size: 16px;
  color: #0f172a;
}

.timeline-body {
  display: grid;
  gap: 4px;
}

.timeline-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.timeline-title-row strong {
  color: #1e293b;
}

.timeline-body p {
  margin: 0;
  color: #475569;
  font-size: 14px;
}

.timeline-type,
.timeline-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.timeline-type.type-teach {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.timeline-type.type-research {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.timeline-type.type-personal {
  background: rgba(168, 85, 247, 0.12);
  color: #7c3aed;
}

.timeline-type.type-maintenance {
  background: rgba(148, 163, 184, 0.16);
  color: #475569;
}

.timeline-status.status-running {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.timeline-status.status-upcoming {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.timeline-status.status-warning {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.timeline-status.status-pending {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.empty-state {
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px dashed rgba(148, 163, 184, 0.4);
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1360px) {
  .overview-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .overview-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1120px) {
  .timeline-item {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .timeline-dialog-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
