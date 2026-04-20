<template>
  <section class="content-grid">
    <BasePanel tag="每日总览" title="某天所有实验室节次状态" :note="schedule ? schedule.date : ''">
      <div class="toolbar">
        <label>
          <span>日期</span>
          <input v-model="date" type="date" />
        </label>
        <button type="button" class="ghost-btn" @click="loadDaily">查询</button>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <div v-if="schedule" class="daily-wrap">
        <div class="legend">
          <span class="legend-item free">空闲</span>
          <span class="legend-item reserved">已预约</span>
          <span class="legend-item pending">待审批</span>
          <span class="legend-item maintenance">维护中</span>
          <span class="legend-item closed">不开放</span>
        </div>

        <div class="table-scroll">
          <table class="daily-table">
            <thead>
              <tr>
                <th class="sticky-col">实验室</th>
                <th v-for="p in schedule.periods" :key="p.id">
                  <div class="p-name">{{ p.periodName }}</div>
                  <div class="p-time">{{ p.startTime }} - {{ p.endTime }}</div>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="lab in schedule.labs" :key="lab.labId">
                <td class="sticky-col lab-col">{{ lab.labName }}</td>
                <td v-for="p in schedule.periods" :key="`${lab.labId}-${p.id}`" class="cell" :class="cellClass(lab.cells, p.id)">
                  <div class="cell-main">
                    <span class="cell-status">{{ cellText(lab.cells, p.id) }}</span>
                    <span v-if="cell(lab.cells, p.id)?.reservationNo" class="cell-sub">#{{ cell(lab.cells, p.id)?.reservationNo }}</span>
                    <span v-if="cell(lab.cells, p.id)?.maintenanceReason" class="cell-sub">{{ cell(lab.cells, p.id)?.maintenanceReason }}</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchDailySchedule } from '../api/labs';
import BasePanel from '../components/BasePanel.vue';
import { useAuthStore } from '../stores/auth';
import type { DailyScheduleDto, ScheduleCellDto } from '../types';

const auth = useAuthStore();
const schedule = ref<DailyScheduleDto | null>(null);
const message = ref('');

const today = new Date();
const date = ref(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`);

function cell(cells: ScheduleCellDto[], periodId: number): ScheduleCellDto | undefined {
  return cells.find((c) => c.periodId === periodId);
}

function cellText(cells: ScheduleCellDto[], periodId: number): string {
  const c = cell(cells, periodId);
  if (!c) return '--';
  if (c.status === 'FREE') return '空闲';
  if (c.status === 'RESERVED') return '已预约';
  if (c.status === 'PENDING') return '待审批';
  if (c.status === 'MAINTENANCE') return '维护';
  return '不开放';
}

function cellClass(cells: ScheduleCellDto[], periodId: number): Record<string, boolean> {
  const c = cell(cells, periodId);
  const s = c?.status ?? 'CLOSED';
  return {
    free: s === 'FREE',
    reserved: s === 'RESERVED',
    pending: s === 'PENDING',
    maintenance: s === 'MAINTENANCE',
    closed: s === 'CLOSED',
  };
}

async function loadDaily(): Promise<void> {
  schedule.value = null;
  message.value = '';
  try {
    schedule.value = await fetchDailySchedule(date.value, auth.token.value);
    message.value = '已加载每日总览。';
  } catch (error) {
    message.value = error instanceof Error ? error.message : '查询失败。';
  }
}

onMounted(() => {
  void loadDaily();
});
</script>

<style scoped>
.daily-wrap {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.66);
  overflow: hidden;
}

.legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.legend-item {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.table-scroll {
  overflow-x: auto;
}

.daily-table {
  width: max-content;
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.daily-table th,
.daily-table td {
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  padding: 8px;
  text-align: center;
  vertical-align: middle;
  min-width: 110px;
}

.daily-table th.sticky-col,
.daily-table td.sticky-col {
  position: sticky;
  left: 0;
  z-index: 2;
  background: rgba(255, 255, 255, 0.95);
  min-width: 180px;
  text-align: left;
}

.lab-col {
  font-weight: 650;
}

.p-name {
  font-weight: 650;
}
.p-time {
  font-size: 12px;
  opacity: 0.75;
  margin-top: 2px;
}

.cell-main {
  display: grid;
  gap: 2px;
  justify-items: center;
}
.cell-status {
  font-size: 12px;
  font-weight: 650;
}
.cell-sub {
  font-size: 11px;
  opacity: 0.85;
  max-width: 110px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cell.free {
  background: rgba(16, 185, 129, 0.08);
}
.cell.reserved {
  background: rgba(239, 68, 68, 0.10);
}
.cell.pending {
  background: rgba(245, 158, 11, 0.12);
}
.cell.maintenance {
  background: rgba(14, 165, 233, 0.12);
}
.cell.closed {
  background: rgba(148, 163, 184, 0.16);
}

.legend-item.free {
  background: rgba(16, 185, 129, 0.08);
}
.legend-item.reserved {
  background: rgba(239, 68, 68, 0.10);
}
.legend-item.pending {
  background: rgba(245, 158, 11, 0.12);
}
.legend-item.maintenance {
  background: rgba(14, 165, 233, 0.12);
}
.legend-item.closed {
  background: rgba(148, 163, 184, 0.16);
}
</style>

