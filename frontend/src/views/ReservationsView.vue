<template>
  <section class="content-grid">
    <BasePanel panel-class="reservation-panel">
      <div class="reservation-header-row">
        <div class="reservation-header-copy">
          <h3>{{ viewMode === 'conflict' ? '冲突调度工作台' : '预约审核列表' }}</h3>
        </div>

        <div class="reservation-header-actions">
          <select v-model="statusFilter" class="reservation-status-select" @change="handleStatusChange">
            <option value="">全部状态</option>
            <option value="1">待审核</option>
            <option value="2">已通过</option>
            <option value="3">已驳回</option>
            <option value="4">已取消</option>
            <option value="5">已完成</option>
          </select>

          <div class="mode-switch">
            <button
              type="button"
              class="ghost-btn"
              :class="{ active: viewMode === 'list' }"
              @click="switchMode('list')"
            >
              预约列表
            </button>
            <button
              type="button"
              class="ghost-btn"
              :class="{ active: viewMode === 'conflict' }"
              @click="switchMode('conflict')"
            >
              冲突预约
            </button>
          </div>

          <button type="button" class="ghost-btn" @click="loadPage">刷新</button>
        </div>
      </div>

      <p v-if="message" class="error-text">{{ message }}</p>

      <BaseTable
        v-if="viewMode === 'conflict'"
        class="reservation-table"
        :headers="['实验室', '日期', '节次', '冲突数量']"
      >
        <tr v-if="loading">
          <td colspan="4" class="state-cell">正在加载冲突预约...</td>
        </tr>
        <tr v-else-if="!displayConflictGroups.length">
          <td colspan="4" class="state-cell">当前没有需要处理的冲突预约。</td>
        </tr>
        <tr
          v-for="item in displayConflictGroups"
          v-else
          :key="item.key"
          class="clickable-row"
          @click="openConflictDialog(item)"
        >
          <td>{{ item.labName }}</td>
          <td>{{ item.reservationDate }}</td>
          <td>{{ item.periodName }}</td>
          <td><span class="status-tag status-conflict">冲突 {{ item.conflictCount }} 人</span></td>
        </tr>
      </BaseTable>

      <BaseTable
        v-else
        class="reservation-table"
        :headers="['实验室', '申请人', '时间详情', '当前状态']"
      >
        <tr v-if="loading">
          <td colspan="4" class="state-cell">正在加载预约数据...</td>
        </tr>
        <tr v-else-if="!displayReservationRows.length">
          <td colspan="4" class="state-cell">当前没有符合条件的预约记录。</td>
        </tr>
        <tr
          v-for="row in displayReservationRows"
          v-else
          :key="row.id"
          class="clickable-row"
          @click="openReservationDialog(row.id)"
        >
          <td>{{ row.labName }}</td>
          <td>{{ row.applicantName }}</td>
          <td>
            <div class="table-time-cell">
              <span>{{ row.timeDetail }}</span>
              <small v-if="row.hasConflict" class="conflict-inline-note">冲突</small>
            </div>
          </td>
          <td>
            <div class="status-stack">
              <span :class="statusClass(row.status)">{{ statusText(row.status) }}</span>
              <span v-if="row.isRecommended" class="recommend-pill">系统推荐</span>
            </div>
          </td>
        </tr>
      </BaseTable>

      <div class="pagination-wrap">
        <span class="pagination-total">共 {{ viewMode === 'conflict' ? conflictState.total : state.total }} 条</span>
        <span class="pagination-text">
          第 {{ currentPage }} / {{ currentPageCount }} 页
        </span>
        <button type="button" class="ghost-btn small-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
          上一页
        </button>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="currentPage === currentPageCount"
          @click="changePage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </BasePanel>
  </section>

  <div v-if="detailDialogVisible" class="detail-dialog-mask" @click.self="closeDetailDialog">
    <div class="detail-dialog" role="dialog" aria-modal="true" aria-labelledby="reservation-detail-dialog-title">
      <div class="detail-dialog-head">
        <div>
          <span class="dialog-tag">{{ selectedRow?.isRecommended ? '系统推荐对象' : '预约详情' }}</span>
          <h3 id="reservation-detail-dialog-title">{{ selectedRow?.labName || '预约详情' }}</h3>
          <p v-if="selectedRow" class="dialog-subtitle">
            {{ selectedRow.applicantName }} / {{ selected?.reservationNo }} / {{ selectedRow.timeDetail }}
          </p>
        </div>
        <button type="button" class="dialog-close" @click="closeDetailDialog">关闭</button>
      </div>

      <div v-if="viewMode === 'conflict' && selectedConflict" class="detail-stack">
        <div class="detail-card">
          <div class="conflict-dialog-head">
            <div>
              <h4>{{ selectedConflict.labName }}</h4>
              <p>{{ selectedConflict.reservationDate }} / {{ selectedConflict.periodName }}</p>
            </div>
            <div class="conflict-dialog-summary">
              <span class="status-tag status-conflict">冲突 {{ selectedConflict.conflictCount }} 人</span>
              <span class="summary-note">系统已按审核优先级自动排序</span>
            </div>
          </div>
          <div class="slot-meta">
            <span>实验室：{{ selectedConflict.labName }}</span>
            <span>时间：{{ selectedConflict.reservationDate }} {{ selectedConflict.periodName }}</span>
            <span v-if="selectedConflict.startTime && selectedConflict.endTime">
              节次：{{ selectedConflict.startTime }} - {{ selectedConflict.endTime }}
            </span>
          </div>
        </div>

        <div class="ranked-list">
          <section
            v-for="item in selectedConflict.reservations"
            :key="item.id"
            class="rank-row"
            :class="{ recommended: item.isRecommended }"
          >
            <div class="rank-index">{{ item.rankLabel }}</div>
            <div class="rank-main">
              <div class="rank-head">
                <div class="rank-title">
                  <strong>{{ item.applicantName }}</strong>
                  <span :class="applicantRoleTagClass(item.applicantRole)">{{ applicantRoleLabel(item.applicantRole) }}</span>
                  <span :class="typeClass(item.reservationType)">{{ typeLabel(item.reservationType) }}</span>
                  <span v-if="item.isRecommended" class="recommend-pill">系统推荐</span>
                  <span v-if="item.priorityRank === 1" class="priority-pill">最高优先级</span>
                </div>
                <span :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
              </div>

              <div class="rank-meta">
                <span v-if="item.applicantRole === 'student'" class="credit-badge">信誉 {{ item.creditScore ?? '--' }}</span>
                <span>{{ item.submitTime || '提交时间待补充' }}</span>
                <span>{{ item.courseOrProjectName || item.usagePurpose || '未填写用途说明' }}</span>
              </div>

              <p class="rank-reason">{{ item.rankReason }}</p>

              <div class="rank-actions">
                <button type="button" class="ghost-btn small-btn" @click="openReservationDialog(item.id)">查看详情</button>
                <button
                  v-if="item.status === 1"
                  type="button"
                  class="primary-btn small-btn"
                  @click="openReservationDialog(item.id)"
                >
                  {{ item.isRecommended ? '推荐通过' : '审核处理' }}
                </button>
              </div>
            </div>
          </section>
        </div>
      </div>

      <div v-else-if="selected && selectedRow" class="detail-stack">
        <div class="detail-overview-grid">
          <div class="detail-card">
            <h4>审核判断</h4>
            <div class="detail-pills">
              <span :class="selectedRow.applicantRole === 'teacher' ? 'role-tag role-teacher' : 'role-tag role-student'">
                {{ selectedRow.applicantRole === 'teacher' ? '教师' : '学生' }}
              </span>
              <span :class="typeClass(selectedRow.reservationType)">
                {{ typeLabel(selectedRow.reservationType) }}
              </span>
              <span :class="statusClass(selected.status)">{{ statusText(selected.status) }}</span>
              <span v-if="selectedRow.hasConflict" class="status-tag status-conflict">冲突待审核</span>
              <span v-if="selectedRow.isRecommended" class="recommend-pill">系统推荐</span>
            </div>
            <p class="decision-summary">{{ selectedRow.rankReason }}</p>
          </div>

          <div class="detail-card">
            <h4>预约信息</h4>
            <div class="detail-list">
              <div><strong>实验室</strong><span>{{ selectedRow.labName }}</span></div>
              <div><strong>申请人</strong><span>{{ selectedRow.applicantName }}</span></div>
              <div><strong>提交时间</strong><span>{{ selectedRow.submitTime || '待补充' }}</span></div>
              <div><strong>信誉分</strong><span>{{ selectedRow.creditScore ?? '-' }}</span></div>
              <div><strong>用途说明</strong><span>{{ selected.usagePurpose || '未填写' }}</span></div>
              <div><strong>课程/项目</strong><span>{{ selected.courseOrProjectName || '未填写' }}</span></div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h5>节次详情</h5>
          <ul class="bullet-list compact-list">
            <li v-for="slot in selected.slots" :key="slot.id">
              {{ slot.reservationDate }} / {{ slot.periodName }} / {{ slot.slotStatus === 1 ? '占用' : '已取消' }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <h5>审核操作</h5>
          <div class="stack-form">
            <label class="wide">
              <span>审核备注（可选）</span>
              <input v-model="auditComment" placeholder="例如：系统排序推荐优先通过本条预约" />
            </label>
            <label class="wide">
              <span>驳回原因（驳回时必填）</span>
              <input v-model="rejectReason" placeholder="例如：与更高优先级预约冲突" />
            </label>
            <div class="button-row">
              <button type="button" class="primary-btn wide" :disabled="selected.status !== 1" @click="handleApprove">
                通过
              </button>
              <button type="button" class="ghost-btn wide" :disabled="selected.status !== 1" @click="handleReject">
                驳回
              </button>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h5>审核日志</h5>
          <ul class="bullet-list compact-list">
            <li v-for="log in logs" :key="log.id">
              {{ log.createdAt }} / {{ auditActionText(log.auditAction) }} / {{ log.auditComment || '无备注' }}
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { fetchReservationAuditLogs } from '../api/reservationAuditLogs';
import {
  approveReservation,
  fetchConflictReservations,
  fetchReservationById,
  fetchReservations,
  rejectReservation,
} from '../api/reservations';
import { fetchLabs } from '../api/labs';
import { fetchUsers } from '../api/users';
import { useAuthStore } from '../stores/auth';
import type { LabDto, ReservationAuditLogDto, ReservationConflictSlotDto, ReservationDto, UserVO } from '../types';
import {
  applicantRoleLabel,
  applicantRoleTagClass,
  buildConflictGroups,
  buildReservationRows,
  getApplicantRole,
  getDecisionType,
  priorityLabel,
  reservationTypeLabel as typeLabel,
  reservationTypeTagClass as typeClass,
  statusLabel as statusText,
  statusTagClass as statusClass,
  sortConflictReservations,
  type ConflictGroupViewModel,
  type ReservationRowViewModel,
} from '../utils/adminReservations';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const message = ref('');
const loading = ref(false);
const viewMode = ref<'list' | 'conflict'>('list');
const statusFilter = ref('');
const state = ref<{ list: ReservationDto[]; total: number }>({ list: [], total: 0 });
const conflictState = ref<{ list: ReservationConflictSlotDto[]; total: number }>({ list: [], total: 0 });
const selected = ref<ReservationDto | null>(null);
const selectedConflict = ref<ConflictGroupViewModel | null>(null);
const logs = ref<ReservationAuditLogDto[]>([]);
const labs = ref<LabDto[]>([]);
const users = ref<UserVO[]>([]);
const detailDialogVisible = ref(false);
const auditComment = ref('');
const rejectReason = ref('');
const listPage = ref(1);
const conflictPage = ref(1);
const pageSize = 8;

const userMap = computed(() => new Map(users.value.map((item) => [item.id, item])));
const conflictGroups = computed<ConflictGroupViewModel[]>(() => buildConflictGroups(conflictState.value.list, userMap.value));
const reservationRows = computed<ReservationRowViewModel[]>(() =>
  buildReservationRows(state.value.list, userMap.value, conflictGroups.value, labName),
);
const displayReservationRows = computed(() =>
  statusFilter.value ? reservationRows.value.filter((item) => String(item.status) === statusFilter.value) : reservationRows.value,
);
const displayConflictGroups = computed(() => {
  return conflictGroups.value
    .map((group) => normalizeConflictGroup(group))
    .filter((group) => group.reservations.length > 0);
});

const currentPage = computed(() => (viewMode.value === 'conflict' ? conflictPage.value : listPage.value));
const currentPageCount = computed(() =>
  Math.max(1, Math.ceil((viewMode.value === 'conflict' ? conflictState.value.total : state.value.total) / pageSize)),
);
const selectedRow = computed<ReservationRowViewModel | null>(() => {
  const selectedId = selected.value?.id;
  if (!selectedId || !selected.value) {
    return null;
  }

  const existingRow = reservationRows.value.find((item) => item.id === selectedId);
  if (existingRow) {
    return existingRow;
  }

  const reservationType = getDecisionType(selected.value.reservationType);
  const applicantRole = getApplicantRole(selected.value.reservationType);
  const user = userMap.value.get(selected.value.applicantUserId);
  const conflictEntry = conflictGroups.value
    .flatMap((group) => group.reservations)
    .find((item) => item.id === selectedId);
  const firstSlot = selected.value.slots?.[0];
  const timeDetail = (selected.value.slots ?? []).map((slot) => `${slot.reservationDate} ${slot.periodName}`).join('，');

  return {
    id: selected.value.id,
    reservationNo: selected.value.reservationNo,
    labId: selected.value.labId,
    labName: labName(selected.value.labId),
    applicantName: selected.value.applicantName || `#${selected.value.applicantUserId}`,
    applicantRole,
    reservationType,
    creditScore: applicantRole === 'student' ? (user?.creditScore ?? null) : null,
    submitTime: selected.value.createdAt || '',
    reservationDate: firstSlot?.reservationDate || '',
    periodName: firstSlot?.periodName || '',
    timeDetail: timeDetail || '--',
    status: selected.value.status,
    hasConflict: Boolean(conflictEntry),
    isRecommended: Boolean(conflictEntry?.isRecommended),
    priorityLabel: priorityLabel(reservationType),
    rankReason: conflictEntry ? conflictEntry.rankReason : `${priorityLabel(reservationType)}，当前无同组冲突`,
    usagePurpose: selected.value.usagePurpose,
    courseOrProjectName: selected.value.courseOrProjectName,
  };
});

function resetDetailState(): void {
  selected.value = null;
  selectedConflict.value = null;
  logs.value = [];
  auditComment.value = '';
  rejectReason.value = '';
}

function closeDetailDialog(): void {
  detailDialogVisible.value = false;
  resetDetailState();
}

function labName(id: number): string {
  return labs.value.find((l) => l.id === id)?.labName ?? `实验室 ${id}`;
}

function auditActionText(action: number): string {
  if (action === 1) return '提交申请';
  if (action === 2) return '审核通过';
  if (action === 3) return '审核驳回';
  if (action === 4) return '取消预约';
  if (action === 5) return '签到/完成';
  if (action === 6) return '签退';
  return '状态变更';
}

async function loadPage(): Promise<void> {
  loading.value = true;
  message.value = '';
  try {
    const status = viewMode.value === 'list' && statusFilter.value ? Number(statusFilter.value) : undefined;
    const labPromise = fetchLabs({ pageNum: 1, pageSize: 200 }, auth.token.value);
    const userPromise = fetchUsers({ pageNum: 1, pageSize: 500 }, auth.token.value);

    if (viewMode.value === 'conflict') {
      const [conflictPageData, labData, userData] = await Promise.all([
        fetchConflictReservations({ pageNum: conflictPage.value, pageSize }, auth.token.value),
        labPromise,
        userPromise,
      ]);
      conflictState.value = conflictPageData;
      labs.value = labData.list;
      users.value = userData.list;
      return;
    }

    const [pageData, conflictPageData, labData, userData] = await Promise.all([
      fetchReservations({ pageNum: listPage.value, pageSize, status }, auth.token.value),
      fetchConflictReservations({ pageNum: 1, pageSize: 200 }, auth.token.value),
      labPromise,
      userPromise,
    ]);
    state.value = pageData;
    conflictState.value = conflictPageData;
    labs.value = labData.list;
    users.value = userData.list;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '加载预约数据失败。';
  } finally {
    loading.value = false;
  }
}

function handleStatusChange(): void {
  if (viewMode.value === 'list') {
    listPage.value = 1;
    void loadPage();
  }
}

function changePage(page: number): void {
  if (viewMode.value === 'conflict') {
    conflictPage.value = Math.max(1, page);
  } else {
    listPage.value = Math.max(1, page);
  }
  void loadPage();
}

async function switchMode(mode: 'list' | 'conflict'): Promise<void> {
  if (viewMode.value === mode) return;
  closeDetailDialog();
  viewMode.value = mode;
  if (mode === 'list') {
    listPage.value = 1;
  } else {
    conflictPage.value = 1;
  }
  await loadPage();

  const query: Record<string, string> = { ...(route.query as Record<string, string>) };
  if (mode === 'conflict') {
    query.view = 'conflict';
  } else {
    delete query.view;
  }
  await router.replace({ query });
}

async function openReservationDialog(id: number): Promise<void> {
  try {
    const [detail, auditLogs] = await Promise.all([
      fetchReservationById(id, auth.token.value),
      fetchReservationAuditLogs(id, auth.token.value),
    ]);
    selected.value = detail;
    logs.value = auditLogs;
    selectedConflict.value = null;
    auditComment.value = selectedRow.value?.isRecommended ? '系统排序推荐优先通过该预约。' : '';
    rejectReason.value = '';
    detailDialogVisible.value = true;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '加载预约详情失败。';
  }
}

function openConflictDialog(item: ConflictGroupViewModel): void {
  selectedConflict.value = normalizeConflictGroup(item);
  selected.value = null;
  logs.value = [];
  auditComment.value = '';
  rejectReason.value = '';
  detailDialogVisible.value = true;
}

function normalizeConflictGroup(group: ConflictGroupViewModel): ConflictGroupViewModel {
  const visibleReservations = group.reservations.filter((item) => {
    if (item.status === 2) {
      return false;
    }
    if (!statusFilter.value) {
      return true;
    }
    return String(item.status) === statusFilter.value;
  });

  const sortedReservations = sortConflictReservations(visibleReservations);

  return {
    ...group,
    reservations: sortedReservations,
    conflictCount: sortedReservations.length,
  };
}

async function handleApprove(): Promise<void> {
  if (!selected.value) return;
  try {
    await approveReservation(selected.value.id, auth.token.value, auditComment.value || undefined);
    closeDetailDialog();
    await loadPage();
  } catch (error) {
    message.value = error instanceof Error ? error.message : '审核失败。';
  }
}

async function handleReject(): Promise<void> {
  if (!selected.value) return;
  if (!rejectReason.value.trim()) {
    message.value = '驳回原因不能为空。';
    return;
  }
  try {
    await rejectReservation(selected.value.id, auth.token.value, rejectReason.value.trim(), auditComment.value || undefined);
    closeDetailDialog();
    await loadPage();
  } catch (error) {
    message.value = error instanceof Error ? error.message : '驳回失败。';
  }
}

watch(
  () => route.query,
  () => {
    const view = typeof route.query.view === 'string' ? route.query.view : '';
    viewMode.value = view === 'conflict' ? 'conflict' : 'list';
  },
);

onMounted(async () => {
  const view = typeof route.query.view === 'string' ? route.query.view : '';
  viewMode.value = view === 'conflict' ? 'conflict' : 'list';
  await loadPage();
});
</script>

<style scoped>
.reservation-panel {
  padding-top: 18px;
  padding-bottom: 18px;
}

.reservation-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
}

.reservation-header-copy h3 {
  margin: 0;
  font-size: var(--page-title-size);
  line-height: var(--page-title-line-height);
}

.reservation-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mode-switch {
  display: inline-flex;
  gap: 8px;
}

.mode-switch .ghost-btn.active {
  background: #edf3ff;
  color: #2f63ff;
  border-color: #bdd0ff;
}

.reservation-status-select {
  min-width: 160px;
  padding: 10px 40px 10px 14px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 999px;
  background-color: rgba(255, 255, 255, 0.9);
  color: #0f172a;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8), 0 8px 20px rgba(15, 23, 42, 0.04);
}

.reservation-table :deep(table) {
  table-layout: fixed;
}

.reservation-table :deep(th),
.reservation-table :deep(td) {
  padding-top: 10px;
  padding-bottom: 10px;
  height: var(--table-row-height);
  vertical-align: middle;
}

.state-cell {
  padding: 28px 12px !important;
  text-align: center;
  color: #64748b;
}

.table-time-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
}

.table-time-cell > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conflict-inline-note {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.1);
  color: #c2410c;
  font-size: 11px;
  line-height: 1.2;
}

.summary-note,
.priority-pill,
.credit-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.summary-note {
  background: rgba(15, 23, 42, 0.05);
  color: #475569;
}

.priority-pill {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.credit-badge {
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
}

.pagination-wrap {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  font-size: 12px;
}

.pagination-total {
  margin-right: auto;
  color: rgba(15, 23, 42, 0.72);
}

.pagination-text {
  color: rgba(15, 23, 42, 0.8);
}

.detail-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(10px);
}

.detail-dialog {
  width: min(980px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  display: grid;
  gap: 18px;
  overflow: auto;
  padding: 24px;
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.24);
}

.detail-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dialog-tag,
.recommend-pill,
.role-tag,
.type-tag,
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.dialog-tag {
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
}

.dialog-subtitle {
  margin: 10px 0 0;
  color: #64748b;
}

.detail-dialog-head h3 {
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

.detail-stack {
  display: grid;
  gap: 18px;
}

.detail-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-card {
  border: 1px solid #e6ebf2;
  border-radius: 18px;
  padding: 16px;
  background: #fff;
  display: grid;
  gap: 12px;
}

.detail-card h4 {
  margin: 0;
}

.conflict-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.conflict-dialog-head p {
  margin: 8px 0 0;
  color: #64748b;
}

.conflict-dialog-summary {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.slot-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #475569;
  font-size: 13px;
}

.ranked-list {
  display: grid;
  gap: 14px;
}

.rank-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.rank-row.recommended {
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.95), rgba(248, 250, 252, 0.95));
  border-color: rgba(16, 185, 129, 0.24);
}

.rank-index {
  width: 72px;
  min-height: 72px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.1), rgba(59, 130, 246, 0.16));
  border: 1px solid rgba(59, 130, 246, 0.18);
  color: #1d4ed8;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.04em;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.rank-main {
  display: grid;
  gap: 12px;
  align-content: start;
}

.rank-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.rank-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.rank-title strong {
  font-size: 18px;
  color: #0f172a;
}

.rank-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: #64748b;
  font-size: 14px;
}

.rank-reason {
  margin: 0;
  color: #334155;
  font-size: 15px;
  line-height: 1.7;
}

.rank-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.decision-summary {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.detail-list {
  display: grid;
  gap: 10px;
}

.detail-list > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.2);
}

.detail-section {
  display: grid;
  gap: 12px;
}

.detail-section h5 {
  margin: 0;
  font-size: 16px;
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.8;
}

.stack-form {
  display: grid;
  gap: 12px;
}

.stack-form label {
  display: grid;
  gap: 8px;
}

.stack-form input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.96);
}

.button-row {
  display: flex;
  gap: 10px;
}

.wide {
  flex: 1;
}

.recommend-pill {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.type-course {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.type-research {
  background: rgba(14, 165, 233, 0.12);
  color: #0f766e;
}

.type-personal {
  background: rgba(100, 116, 139, 0.12);
  color: #475569;
}

.role-teacher {
  background: rgba(99, 102, 241, 0.12);
  color: #4338ca;
}

.role-student {
  background: rgba(148, 163, 184, 0.14);
  color: #475569;
}

.status-pending {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.status-approved {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.status-rejected,
.status-cancelled {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.status-completed {
  background: rgba(71, 85, 105, 0.12);
  color: #334155;
}

.status-conflict {
  background: rgba(249, 115, 22, 0.14);
  color: #c2410c;
}

@media (max-width: 920px) {
  .reservation-header-row,
  .detail-overview-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .conflict-dialog-head,
  .rank-head {
    display: grid;
  }

  .conflict-dialog-summary {
    justify-items: start;
  }
}

@media (max-width: 640px) {
  .detail-dialog-mask {
    padding: 12px;
  }

  .detail-dialog {
    width: calc(100vw - 24px);
    max-height: calc(100vh - 24px);
    padding: 18px;
  }

  .rank-row {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .rank-index {
    width: 64px;
    min-height: 64px;
    font-size: 22px;
    border-radius: 18px;
  }

  .button-row {
    display: grid;
  }
}
</style>
