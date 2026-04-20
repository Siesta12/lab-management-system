<template>
  <section class="content-grid split-grid">
    <BasePanel tag="实验室查询" title="实验室列表与筛选" :note="`共 ${labsState.total} 条`">
      <div class="toolbar">
        <input v-model="keyword" placeholder="输入实验室名称或编号筛选" />
        <button type="button" class="ghost-btn" @click="loadLabs">查询</button>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <BaseTable :headers="['名称', '编号', '位置', '开放状态', '运行状态', '容量']">
        <tr
          v-for="lab in displayLabs"
          :key="lab.id"
          class="clickable-row"
          @click="selectLab(lab.id)"
        >
          <td>{{ lab.labName }}</td>
          <td>{{ lab.labCode }}</td>
          <td>{{ buildLocation(lab) }}</td>
          <td><span :class="getBadgeClass(openStatusText(lab.openStatus))">{{ openStatusText(lab.openStatus) }}</span></td>
          <td><span :class="getBadgeClass(labStatusText(lab.labStatus))">{{ labStatusText(lab.labStatus) }}</span></td>
          <td>{{ lab.capacity }}</td>
        </tr>
      </BaseTable>
    </BasePanel>

    <BasePanel tag="实验室详情" title="未来三周课表 / 设备 / 耗材" panel-class="detail-panel">
      <div v-if="selectedLab" class="detail-stack">
        <div class="detail-card">
          <h4>{{ selectedLab.labName }}</h4>
          <p>{{ selectedDepartmentName }} · {{ selectedLab.labType || '类型待补充' }}</p>
        </div>

        <div class="detail-list">
          <div><strong>实验室编号</strong><span>{{ selectedLab.labCode }}</span></div>
          <div><strong>位置</strong><span>{{ buildLocation(selectedLab) }}</span></div>
          <div><strong>容量</strong><span>{{ selectedLab.capacity }} 人</span></div>
          <div><strong>开放状态</strong><span>{{ openStatusText(selectedLab.openStatus) }}</span></div>
          <div><strong>运行状态</strong><span>{{ labStatusText(selectedLab.labStatus) }}</span></div>
        </div>

        <div class="detail-section">
          <h5>实验室简介</h5>
          <p>{{ selectedLab.description || '暂无简介说明。' }}</p>
        </div>

        <div class="detail-section">
          <h5>使用规则</h5>
          <p>{{ selectedLab.usageRule || '暂无使用规则说明。' }}</p>
        </div>

        <div class="detail-section">
          <h5>设备配置</h5>
          <ul class="bullet-list compact-list">
            <li v-for="device in selectedDevices" :key="device.id">
              {{ device.deviceName }} · {{ device.availableQuantity }}/{{ device.quantity }} 可用
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <h5>耗材情况</h5>
          <ul class="bullet-list compact-list">
            <li v-for="consumable in selectedConsumables" :key="consumable.id">
              {{ consumable.consumableName }} · 库存 {{ consumable.stockQuantity }} {{ consumable.unit }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <div class="schedule-head">
            <h5>未来三周课表（按节次）</h5>
            <div v-if="isAuthenticated" class="schedule-actions">
              <label class="mode-pill">
                <span>模式</span>
                <select v-model="mode">
                  <option value="reserve">预约</option>
                  <option v-if="isAdmin" value="maintenance">维护</option>
                </select>
              </label>
              <button type="button" class="ghost-btn" @click="reloadSchedule">刷新课表</button>
            </div>
          </div>

          <p v-if="scheduleMessage" class="info-text">{{ scheduleMessage }}</p>

          <div v-if="schedule" class="schedule-wrap">
            <div class="legend">
              <span class="legend-item free">空闲</span>
              <span class="legend-item reserved">已预约</span>
              <span class="legend-item pending">待审批</span>
              <span class="legend-item maintenance">维护中</span>
              <span class="legend-item closed">不开放</span>
              <span v-if="selectedKeys.length" class="legend-selected">已选 {{ selectedKeys.length }} 个</span>
            </div>

            <div class="schedule-table-scroll">
              <table class="schedule-table">
                <thead>
                  <tr>
                    <th class="sticky-col">节次</th>
                    <th v-for="day in schedule.days" :key="day.date">
                      <div class="day-head">
                        <div class="day-date">{{ day.date.slice(5) }}</div>
                        <div class="day-week">{{ weekdayText(day.weekday) }}</div>
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="period in schedule.periods" :key="period.id">
                    <td class="sticky-col period-col">
                      <div class="period-name">{{ period.periodName }}</div>
                      <div class="period-time">{{ period.startTime }} - {{ period.endTime }}</div>
                    </td>
                    <td
                      v-for="day in schedule.days"
                      :key="`${day.date}-${period.id}`"
                      class="schedule-cell"
                      :class="cellClass(day, period.id)"
                      @click="handleCellClick(day, period.id)"
                    >
                      <div class="cell-main">
                        <span class="cell-status">{{ cellStatusText(day, period.id) }}</span>
                        <span v-if="cell(day, period.id)?.reservationNo" class="cell-sub">#{{ cell(day, period.id)?.reservationNo }}</span>
                        <span v-if="cell(day, period.id)?.maintenanceReason" class="cell-sub">{{ cell(day, period.id)?.maintenanceReason }}</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div v-if="isAuthenticated" class="schedule-forms">
              <div v-if="mode === 'reserve'" class="form-card">
                <h6>预约表单</h6>
                <div class="grid-form">
                  <label>
                    <span>预约类型</span>
                    <select v-model.number="reservationForm.reservationType">
                      <option :value="1">课程</option>
                      <option :value="2">科研</option>
                      <option :value="3">个人</option>
                    </select>
                  </label>
                  <label>
                    <span>优先级</span>
                    <select v-model.number="reservationForm.priorityLevel">
                      <option :value="1">高</option>
                      <option :value="2">中</option>
                      <option :value="3">低</option>
                    </select>
                  </label>
                  <label class="wide">
                    <span>用途说明</span>
                    <input v-model="reservationForm.usagePurpose" placeholder="例如：课程实验 / 科研训练" />
                  </label>
                  <label class="wide">
                    <span>课程/项目名称</span>
                    <input v-model="reservationForm.courseOrProjectName" placeholder="可选" />
                  </label>
                  <label>
                    <span>参与人数</span>
                    <input v-model.number="reservationForm.participantCount" type="number" min="1" />
                  </label>
                  <label>
                    <span>联系电话</span>
                    <input v-model="reservationForm.contactPhone" placeholder="必填" />
                  </label>
                </div>
                <div class="button-row">
                  <button type="button" class="ghost-btn wide" :disabled="!selectedKeys.length" @click="handleRecommend">
                    推荐可选节次
                  </button>
                  <button type="button" class="primary-btn wide" :disabled="!canSubmitReservation" @click="handleCreateReservation">
                    提交预约
                  </button>
                </div>
              </div>

              <div v-if="isAdmin && mode === 'maintenance'" class="form-card">
                <h6>维护设置</h6>
                <div class="grid-form">
                  <label class="wide">
                    <span>维护原因</span>
                    <input v-model="maintenanceReason" placeholder="例如：设备检修" />
                  </label>
                </div>
                <div class="button-row">
                  <button type="button" class="primary-btn wide" :disabled="!canSubmitMaintenance" @click="handleCreateMaintenance">
                    设置维护
                  </button>
                  <button type="button" class="ghost-btn wide" @click="clearSelection">清空选择</button>
                </div>
                <p class="info-text">提示：点击“维护中”格子会直接取消维护。</p>
              </div>

              <div v-if="recommendations.length" class="form-card">
                <h6>推荐结果</h6>
                <ul class="bullet-list compact-list">
                  <li v-for="(item, idx) in recommendations" :key="idx">
                    {{ item.labName }} · {{ item.reservationDate }} · {{ item.periodName || `节次#${item.periodId}` }} · {{ item.recommendationReason }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>

      <p v-else class="info-text">请选择左侧实验室查看详情与课表。</p>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { hasRouteAccess } from '../access';
import { fetchConsumables } from '../api/consumables';
import { fetchDepartmentOptions } from '../api/departments';
import { fetchDevices } from '../api/devices';
import { cancelLabMaintenance, createLabMaintenance, fetchLabById, fetchLabs, fetchLabSchedule } from '../api/labs';
import { createReservation, recommendSlots } from '../api/reservations';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useAuthStore } from '../stores/auth';
import type { ConsumableDto, DeviceDto, LabDto, LabScheduleDto, OptionItem, ScheduleDayDto, ScheduleCellDto, SlotRecommendationItem } from '../types';
import { getBadgeClass } from '../utils/format';

const auth = useAuthStore();
const isAdmin = computed(() => hasRouteAccess(auth.currentUser.value?.roleCodes, ['ADMIN']));
const isAuthenticated = computed(() => Boolean(auth.token.value));

const keyword = ref('');
const message = ref('');
const labsState = ref<{ list: LabDto[]; total: number }>({ list: [], total: 0 });
const departments = ref<OptionItem[]>([]);
const selectedLab = ref<LabDto | null>(null);
const selectedDevices = ref<DeviceDto[]>([]);
const selectedConsumables = ref<ConsumableDto[]>([]);

const schedule = ref<LabScheduleDto | null>(null);
const scheduleMessage = ref('');
const mode = ref<'reserve' | 'maintenance'>('reserve');
const selectedKeys = ref<Array<{ date: string; periodId: number }>>([]);
const recommendations = ref<SlotRecommendationItem[]>([]);
const maintenanceReason = ref('');

const reservationForm = reactive({
  reservationType: 3,
  priorityLevel: 3,
  usagePurpose: '',
  courseOrProjectName: '',
  participantCount: 1,
  contactPhone: '',
});

const displayLabs = computed(() => labsState.value.list);
const selectedDepartmentName = computed(() => {
  if (!selectedLab.value?.departmentId) {
    return '所属学院待补充';
  }
  return departments.value.find((item) => item.value === selectedLab.value?.departmentId)?.label ?? '所属学院待补充';
});

const canSubmitReservation = computed(() => {
  return (
    isAuthenticated.value &&
    mode.value === 'reserve' &&
    selectedKeys.value.length > 0 &&
    reservationForm.usagePurpose.trim().length > 0 &&
    reservationForm.contactPhone.trim().length > 0
  );
});

const canSubmitMaintenance = computed(() => {
  return isAdmin.value && mode.value === 'maintenance' && selectedKeys.value.length > 0 && maintenanceReason.value.trim().length > 0;
});

function buildLocation(lab: LabDto): string {
  return [lab.buildingName, lab.roomNo].filter(Boolean).join(' / ') || '位置待补充';
}

function openStatusText(status: number): '开放' | '关闭' {
  return status === 1 ? '开放' : '关闭';
}

function labStatusText(status: number): '正常' | '维护' {
  return status === 2 ? '维护' : '正常';
}

function weekdayText(weekday: number): string {
  const map = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  return map[weekday - 1] ?? `星期${weekday}`;
}

async function loadLabs(): Promise<void> {
  try {
    const data = await fetchLabs({ pageNum: 1, pageSize: 50, labName: keyword.value || undefined }, auth.token.value || undefined);
    labsState.value = data;
    if (data.list.length && !selectedLab.value) {
      await selectLab(data.list[0].id);
    }
    message.value = '已加载实验室列表。';
  } catch (error) {
    message.value = error instanceof Error ? `实验室数据加载失败：${error.message}` : '实验室数据加载失败。';
  }
}

async function selectLab(id: number): Promise<void> {
  try {
    const [lab, devices, consumables] = await Promise.all([
      fetchLabById(id, auth.token.value || undefined),
      fetchDevices({ labId: id, pageNum: 1, pageSize: 30 }, auth.token.value || undefined),
      fetchConsumables({ labId: id, pageNum: 1, pageSize: 30 }, auth.token.value || undefined),
    ]);
    selectedLab.value = lab;
    selectedDevices.value = devices.list;
    selectedConsumables.value = consumables.list;
    clearSelection();
    recommendations.value = [];
    await reloadSchedule();
  } catch (error) {
    message.value = error instanceof Error ? `实验室详情加载失败：${error.message}` : '实验室详情加载失败。';
  }
}

function cell(day: ScheduleDayDto, periodId: number): ScheduleCellDto | undefined {
  return day.cells.find((c) => c.periodId === periodId);
}

function cellStatusText(day: ScheduleDayDto, periodId: number): string {
  const c = cell(day, periodId);
  if (!c) return '--';
  if (c.status === 'FREE') return '空闲';
  if (c.status === 'RESERVED') return '已预约';
  if (c.status === 'PENDING') return '待审批';
  if (c.status === 'MAINTENANCE') return '维护';
  return '不开放';
}

function isSelected(date: string, periodId: number): boolean {
  return selectedKeys.value.some((k) => k.date === date && k.periodId === periodId);
}

function cellClass(day: ScheduleDayDto, periodId: number): Record<string, boolean> {
  const c = cell(day, periodId);
  const status = c?.status ?? 'CLOSED';
  return {
    free: status === 'FREE',
    reserved: status === 'RESERVED',
    pending: status === 'PENDING',
    maintenance: status === 'MAINTENANCE',
    closed: status === 'CLOSED',
    selected: isSelected(day.date, periodId),
    clickable: status === 'FREE' || (mode.value === 'maintenance' && status === 'MAINTENANCE'),
  };
}

function toggleSelection(date: string, periodId: number): void {
  const idx = selectedKeys.value.findIndex((k) => k.date === date && k.periodId === periodId);
  if (idx >= 0) {
    selectedKeys.value.splice(idx, 1);
  } else {
    selectedKeys.value.push({ date, periodId });
  }
}

async function reloadSchedule(): Promise<void> {
  schedule.value = null;
  scheduleMessage.value = '';
  if (!selectedLab.value || !auth.token.value) {
    scheduleMessage.value = '登录后可查看未来三周课表并进行预约/维护操作。';
    return;
  }
  try {
    schedule.value = await fetchLabSchedule(selectedLab.value.id, auth.token.value);
    scheduleMessage.value = '课表已加载。点击空闲格子选择节次。';
  } catch (error) {
    scheduleMessage.value = error instanceof Error ? `课表加载失败：${error.message}` : '课表加载失败。';
  }
}

function clearSelection(): void {
  selectedKeys.value = [];
}

async function handleCellClick(day: ScheduleDayDto, periodId: number): Promise<void> {
  const c = cell(day, periodId);
  if (!c) return;

  if (mode.value === 'maintenance') {
    if (!isAdmin.value) return;
    if (c.status === 'MAINTENANCE' && c.maintenanceId) {
      try {
        await cancelLabMaintenance(selectedLab.value!.id, c.maintenanceId, auth.token.value!);
        scheduleMessage.value = '已取消维护。';
        await reloadSchedule();
      } catch (error) {
        scheduleMessage.value = error instanceof Error ? error.message : '取消维护失败。';
      }
      return;
    }
    if (c.status === 'FREE') {
      toggleSelection(day.date, periodId);
    }
    return;
  }

  if (c.status === 'FREE') {
    toggleSelection(day.date, periodId);
    return;
  }

  scheduleMessage.value = '该节次不可预约，可点击“推荐可选节次”获取建议。';
}

async function handleCreateReservation(): Promise<void> {
  if (!selectedLab.value || !auth.token.value) return;
  if (!canSubmitReservation.value) {
    scheduleMessage.value = '请先选择空闲节次，并填写用途说明与联系电话。';
    return;
  }
  try {
    await createReservation(
      {
        labId: selectedLab.value.id,
        reservationType: reservationForm.reservationType,
        priorityLevel: reservationForm.priorityLevel,
        usagePurpose: reservationForm.usagePurpose,
        courseOrProjectName: reservationForm.courseOrProjectName || undefined,
        participantCount: reservationForm.participantCount,
        contactPhone: reservationForm.contactPhone,
        slots: selectedKeys.value.map((k) => ({ reservationDate: k.date, periodId: k.periodId })),
      },
      auth.token.value,
    );
    scheduleMessage.value = '预约提交成功，已刷新课表。';
    clearSelection();
    recommendations.value = [];
    await reloadSchedule();
  } catch (error) {
    scheduleMessage.value = error instanceof Error ? error.message : '预约提交失败。';
  }
}

async function handleCreateMaintenance(): Promise<void> {
  if (!selectedLab.value || !auth.token.value) return;
  if (!canSubmitMaintenance.value) {
    scheduleMessage.value = '请先选择空闲节次，并填写维护原因。';
    return;
  }
  const groups = new Map<string, number[]>();
  for (const item of selectedKeys.value) {
    const list = groups.get(item.date) ?? [];
    list.push(item.periodId);
    groups.set(item.date, list);
  }
  try {
    for (const [date, periodIds] of groups.entries()) {
      await createLabMaintenance(
        selectedLab.value.id,
        { maintenanceDate: date, periodIds: Array.from(new Set(periodIds)), reason: maintenanceReason.value },
        auth.token.value,
      );
    }
    scheduleMessage.value = '维护设置成功，已刷新课表。';
    clearSelection();
    await reloadSchedule();
  } catch (error) {
    scheduleMessage.value = error instanceof Error ? error.message : '维护设置失败。';
  }
}

async function handleRecommend(): Promise<void> {
  if (!selectedLab.value || !auth.token.value) return;
  if (!selectedKeys.value.length) {
    scheduleMessage.value = '请先选择你想预约的节次（或选择一个不可预约的节次，再看推荐）。';
    return;
  }
  try {
    recommendations.value = await recommendSlots(
      {
        labId: selectedLab.value.id,
        participantCount: reservationForm.participantCount,
        slots: selectedKeys.value.map((k) => ({ reservationDate: k.date, periodId: k.periodId })),
      },
      auth.token.value,
    );
    scheduleMessage.value = recommendations.value.length ? '已生成推荐结果。' : '暂无可推荐的节次/实验室。';
  } catch (error) {
    scheduleMessage.value = error instanceof Error ? error.message : '推荐查询失败。';
  }
}

onMounted(async () => {
  try {
    departments.value = await fetchDepartmentOptions(auth.token.value || undefined);
  } catch {
    departments.value = [];
  }
  await loadLabs();
});
</script>

<style scoped>
.schedule-wrap {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.66);
  overflow: hidden;
}

.schedule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.schedule-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
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

.legend-selected {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.8;
}

.schedule-table-scroll {
  overflow-x: auto;
}

.schedule-table {
  width: max-content;
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.schedule-table th,
.schedule-table td {
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  padding: 8px;
  text-align: center;
  vertical-align: middle;
  min-width: 92px;
}

.schedule-table th.sticky-col,
.schedule-table td.sticky-col {
  position: sticky;
  left: 0;
  z-index: 2;
  background: rgba(255, 255, 255, 0.95);
  min-width: 160px;
  text-align: left;
}

.period-col {
  padding: 10px 12px;
}

.period-name {
  font-weight: 650;
}

.period-time {
  font-size: 12px;
  opacity: 0.75;
  margin-top: 2px;
}

.day-head {
  display: grid;
  gap: 2px;
}

.day-date {
  font-weight: 650;
}

.day-week {
  font-size: 12px;
  opacity: 0.75;
}

.schedule-cell {
  cursor: default;
  user-select: none;
}

.schedule-cell.clickable {
  cursor: pointer;
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
  max-width: 90px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.schedule-cell.free {
  background: rgba(16, 185, 129, 0.08);
}
.schedule-cell.reserved {
  background: rgba(239, 68, 68, 0.10);
}
.schedule-cell.pending {
  background: rgba(245, 158, 11, 0.12);
}
.schedule-cell.maintenance {
  background: rgba(14, 165, 233, 0.12);
}
.schedule-cell.closed {
  background: rgba(148, 163, 184, 0.16);
}

.schedule-cell.selected {
  outline: 2px solid rgba(15, 23, 42, 0.55);
  outline-offset: -2px;
}

.schedule-forms {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.form-card {
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.75);
}

.form-card h6 {
  margin: 0 0 10px;
  font-size: 13px;
  opacity: 0.9;
}

.grid-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.grid-form label.wide {
  grid-column: 1 / -1;
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

@media (max-width: 980px) {
  .grid-form {
    grid-template-columns: 1fr;
  }
  .schedule-table th.sticky-col,
  .schedule-table td.sticky-col {
    min-width: 140px;
  }
}
</style>
