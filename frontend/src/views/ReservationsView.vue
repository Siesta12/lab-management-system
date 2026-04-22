<template>
  <section class="content-grid">
    <BasePanel
      tag="预约管理"
      :note="viewMode === 'conflict' ? `共 ${conflictState.total} 条` : `共 ${state.total} 条`"
      panel-class="reservation-panel"
    >
      <div class="reservation-header-row">
        <div class="reservation-header-copy">
          <h3>{{ viewMode === 'conflict' ? '冲突预约列表' : '预约列表' }}</h3>
        </div>

        <div class="reservation-header-actions">
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

          <select v-if="viewMode === 'list'" v-model="statusFilter" class="reservation-status-select" @change="changeListPage(1)">
            <option value="">全部状态</option>
            <option value="1">待审核</option>
            <option value="2">已通过</option>
            <option value="3">已驳回</option>
            <option value="4">已取消</option>
            <option value="5">已完成</option>
          </select>

          <button type="button" class="ghost-btn" @click="loadPage">刷新</button>
        </div>
      </div>

      <p v-if="message" class="error-text">{{ message }}</p>

      <BaseTable
        v-if="viewMode === 'conflict'"
        class="reservation-table"
        :headers="['实验室', '日期', '节次', '冲突数量']"
      >
        <tr
          v-for="item in conflictState.list"
          :key="`${item.labId}-${item.reservationDate}-${item.periodId}`"
          class="clickable-row"
          @click="openConflictDialog(item)"
        >
          <td>{{ item.labName }}</td>
          <td>{{ item.reservationDate }}</td>
          <td>{{ item.periodName }}</td>
          <td><span class="badge badge-warning">{{ item.conflictCount }} 条</span></td>
        </tr>
      </BaseTable>

      <BaseTable
        v-else
        class="reservation-table"
        :headers="['实验室', '申请人', '时段详情', '状态']"
      >
        <tr
          v-for="item in filteredReservations"
          :key="item.id"
          class="clickable-row"
          @click="openReservationDialog(item.id)"
        >
          <td>{{ labName(item.labId) }}</td>
          <td>
            <div class="reservation-user-cell">
              <span>{{ item.applicantName || `#${item.applicantUserId}` }}</span>
            </div>
          </td>
          <td>
            <div class="reservation-summary-cell">
              <span>{{ slotSummary(item) }}</span>
              <small v-if="isConflictReservation(item.id)" class="conflict-inline-note">该时段存在冲突</small>
            </div>
          </td>
          <td>
            <div class="status-stack">
              <span
                v-if="isConflictReservation(item.id)"
                class="badge badge-warning badge-inline"
              >
                冲突 · {{ statusText(item.status) }}
              </span>
              <span v-else :class="getBadgeClass(statusText(item.status))">{{ statusText(item.status) }}</span>
            </div>
          </td>
        </tr>
      </BaseTable>

      <div v-if="viewMode === 'list'" class="pagination-wrap">
        <span class="pagination-total">共 {{ state.total }} 条</span>
        <span class="pagination-text">第 {{ listPage }} / {{ listPageCount }} 页</span>
        <button type="button" class="ghost-btn small-btn" :disabled="listPage === 1" @click="changeListPage(listPage - 1)">
          上一页
        </button>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="listPage === listPageCount"
          @click="changeListPage(listPage + 1)"
        >
          下一页
        </button>
      </div>

      <div v-else class="pagination-wrap">
        <span class="pagination-total">共 {{ conflictState.total }} 条</span>
        <span class="pagination-text">第 {{ conflictPage }} / {{ conflictPageCount }} 页</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="conflictPage === 1"
          @click="changeConflictPage(conflictPage - 1)"
        >
          上一页
        </button>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="conflictPage === conflictPageCount"
          @click="changeConflictPage(conflictPage + 1)"
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
          <span class="dialog-tag">{{ detailDialogTag }}</span>
          <h3 id="reservation-detail-dialog-title">{{ detailDialogTitle }}</h3>
        </div>
        <button type="button" class="dialog-close" @click="closeDetailDialog">关闭</button>
      </div>

      <div v-if="viewMode === 'conflict'">
        <div v-if="selectedConflict" class="detail-stack">
          <div class="detail-card">
            <h4>{{ selectedConflict.labName }}</h4>
            <p>{{ selectedConflict.reservationDate }} / {{ selectedConflict.periodName }}</p>
          </div>

          <div class="detail-list">
            <div><strong>冲突数量</strong><span>{{ selectedConflict.conflictCount }} 条预约</span></div>
            <div>
              <strong>节次</strong>
              <span>{{ selectedConflict.periodName }}（{{ selectedConflict.startTime }} - {{ selectedConflict.endTime }}）</span>
            </div>
            <div><strong>日期</strong><span>{{ selectedConflict.reservationDate }}</span></div>
          </div>

          <div class="detail-section">
            <h5>冲突预约明细</h5>
            <div class="conflict-reservation-list">
              <article
                v-for="item in selectedConflict.reservations"
                :key="item.reservationId"
                class="conflict-reservation-card"
              >
                <div class="conflict-reservation-head">
                  <strong>{{ item.applicantName }}</strong>
                  <span :class="getBadgeClass(statusText(item.status))">{{ statusText(item.status) }}</span>
                </div>
                <div class="conflict-reservation-meta">
                  <span>{{ typeText(item.reservationType) }}</span>
                  <span>{{ priorityText(item.priorityLevel) }}</span>
                  <span>{{ item.createdAt }}</span>
                </div>
                <p class="conflict-reservation-note">
                  {{ item.reservationNo }} / {{ item.courseOrProjectName || item.usagePurpose || '未填写用途' }}
                </p>
                <button type="button" class="ghost-btn small-btn" @click.stop="viewReservation(item.reservationId)">
                  查看该预约
                </button>
              </article>
            </div>
          </div>
        </div>
      </div>

      <div v-else>
        <div v-if="selected" class="detail-stack">
          <div class="detail-card">
            <h4>{{ labName(selected.labId) }}</h4>
            <p>{{ selected.applicantName || `#${selected.applicantUserId}` }} / {{ selected.reservationNo }}</p>
          </div>

          <div class="detail-list">
            <div><strong>状态</strong><span>{{ statusText(selected.status) }}</span></div>
            <div><strong>类型</strong><span>{{ typeText(selected.reservationType) }}</span></div>
            <div><strong>优先级</strong><span>{{ priorityText(selected.priorityLevel) }}</span></div>
            <div><strong>用途</strong><span>{{ selected.usagePurpose }}</span></div>
            <div><strong>课程/项目</strong><span>{{ selected.courseOrProjectName || '未填写' }}</span></div>
            <div><strong>参与人数</strong><span>{{ selected.participantCount }} 人</span></div>
            <div><strong>联系电话</strong><span>{{ selected.contactPhone || '未填写' }}</span></div>
            <div><strong>驳回原因</strong><span>{{ selected.rejectReason || '无' }}</span></div>
          </div>

          <div class="detail-section">
            <h5>节次明细</h5>
            <ul class="bullet-list compact-list">
              <li v-for="slot in selected.slots" :key="slot.id">
                {{ slot.reservationDate }} / {{ slot.periodName }} / {{ slot.slotStatus === 1 ? '占用' : '已取消' }}
              </li>
            </ul>
          </div>

          <div class="detail-section">
            <h5>审批操作</h5>
            <div class="stack-form">
              <label class="wide">
                <span>备注（可选）</span>
                <input v-model="auditComment" placeholder="例如：请注意设备使用规范" />
              </label>
              <label class="wide">
                <span>驳回原因（驳回时必填）</span>
                <input v-model="rejectReason" placeholder="例如：节次冲突 / 人数超限 / 维护中" />
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
import { useAuthStore } from '../stores/auth';
import type { LabDto, ReservationAuditLogDto, ReservationConflictSlotDto, ReservationDto } from '../types';
import { getBadgeClass } from '../utils/format';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const message = ref('');
const viewMode = ref<'list' | 'conflict'>('list');
const statusFilter = ref('');
const state = ref<{ list: ReservationDto[]; total: number }>({ list: [], total: 0 });
const conflictState = ref<{ list: ReservationConflictSlotDto[]; total: number }>({ list: [], total: 0 });
const conflictReservationIds = ref<Set<number>>(new Set());
const selected = ref<ReservationDto | null>(null);
const selectedConflict = ref<ReservationConflictSlotDto | null>(null);
const logs = ref<ReservationAuditLogDto[]>([]);
const labs = ref<LabDto[]>([]);
const auditComment = ref('');
const rejectReason = ref('');
const detailDialogVisible = ref(false);
const listPage = ref(1);
const conflictPage = ref(1);
const listPageSize = 8;
const conflictPageSize = 8;

const filteredReservations = computed(() => state.value.list);
const listPageCount = computed(() => Math.max(1, Math.ceil(state.value.total / listPageSize)));
const conflictPageCount = computed(() => Math.max(1, Math.ceil(conflictState.value.total / conflictPageSize)));
const detailDialogTag = computed(() => (viewMode.value === 'conflict' ? '冲突详情' : '预约详情'));
const detailDialogTitle = computed(() => (viewMode.value === 'conflict' ? '冲突时段详情' : '预约详情与审批'));

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

function slotSummary(item: ReservationDto): string {
  const parts = item.slots?.map((slot) => `${slot.reservationDate} ${slot.periodName}`) ?? [];
  if (parts.length <= 2) return parts.join('，') || '--';
  return `${parts.slice(0, 2).join('，')} 等（共 ${parts.length} 项）`;
}

function isConflictReservation(id: number): boolean {
  return conflictReservationIds.value.has(id);
}

function statusText(status: number): '待审核' | '已通过' | '已驳回' | '已取消' | '已完成' {
  if (status === 2) return '已通过';
  if (status === 3) return '已驳回';
  if (status === 4) return '已取消';
  if (status === 5) return '已完成';
  return '待审核';
}

function typeText(type: number): string {
  if (type === 1) return '课程预约';
  if (type === 2) return '科研预约';
  return '个人预约';
}

function priorityText(value: number): string {
  if (value === 1) return '高';
  if (value === 2) return '中';
  return '低';
}

function auditActionText(action: number): string {
  if (action === 1) return '提交申请';
  if (action === 2) return '审核通过';
  if (action === 3) return '审核驳回';
  if (action === 4) return '取消预约';
  if (action === 5) return '完成/结束';
  return '状态变更';
}

async function loadPage(): Promise<void> {
  try {
    const labPromise = fetchLabs({ pageNum: 1, pageSize: 200 }, auth.token.value);

    if (viewMode.value === 'conflict') {
      const [conflictPageData, labData] = await Promise.all([
        fetchConflictReservations({ pageNum: conflictPage.value, pageSize: conflictPageSize }, auth.token.value),
        labPromise,
      ]);
      conflictState.value = conflictPageData;
      conflictReservationIds.value = new Set(
        conflictPageData.list.flatMap((item) => item.reservations.map((reservation) => reservation.reservationId)),
      );
      labs.value = labData.list;
      return;
    }

    const status = statusFilter.value ? Number(statusFilter.value) : undefined;
    const [pageData, conflictPageData, labData] = await Promise.all([
      fetchReservations(
        { pageNum: listPage.value, pageSize: listPageSize, status },
        auth.token.value,
      ),
      fetchConflictReservations({ pageNum: 1, pageSize: 200 }, auth.token.value),
      labPromise,
    ]);
    state.value = pageData;
    conflictState.value = conflictPageData;
    conflictReservationIds.value = new Set(
      conflictPageData.list.flatMap((item) => item.reservations.map((reservation) => reservation.reservationId)),
    );
    labs.value = labData.list;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '加载失败。';
  }
}

function changeListPage(page: number): void {
  listPage.value = Math.max(1, page);
  void loadPage();
}

function changeConflictPage(page: number): void {
  conflictPage.value = Math.max(1, page);
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
    selectedConflict.value = null;
    logs.value = auditLogs;
    auditComment.value = '';
    rejectReason.value = '';
    detailDialogVisible.value = true;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '加载预约详情失败。';
  }
}

function openConflictDialog(item: ReservationConflictSlotDto): void {
  selectedConflict.value = item;
  selected.value = null;
  logs.value = [];
  auditComment.value = '';
  rejectReason.value = '';
  detailDialogVisible.value = true;
}

async function viewReservation(id: number): Promise<void> {
  viewMode.value = 'list';
  listPage.value = 1;
  const query: Record<string, string> = { ...(route.query as Record<string, string>) };
  delete query.view;
  await router.replace({ query });
  await loadPage();
  await openReservationDialog(id);
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
    await rejectReservation(
      selected.value.id,
      auth.token.value,
      rejectReason.value.trim(),
      auditComment.value || undefined,
    );
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
    if (view === 'conflict') {
      viewMode.value = 'conflict';
    } else {
      viewMode.value = 'list';
    }
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
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.reservation-header-copy h3 {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
}

.reservation-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
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

.reservation-status-select:focus {
  border-color: rgba(37, 99, 235, 0.44);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1), 0 14px 28px rgba(37, 99, 235, 0.08);
  outline: none;
}

.reservation-summary-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.reservation-user-cell {
  display: grid;
  gap: 0;
  min-width: 0;
}

.conflict-inline-note {
  color: #c2410c;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-stack {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.badge-inline {
  white-space: nowrap;
}

.reservation-table :deep(table) {
  table-layout: fixed;
}

.reservation-table :deep(th:nth-child(1)),
.reservation-table :deep(td:nth-child(1)) {
  width: 28%;
}

.reservation-table :deep(th:nth-child(2)),
.reservation-table :deep(td:nth-child(2)) {
  width: 18%;
}

.reservation-table :deep(th:nth-child(3)),
.reservation-table :deep(td:nth-child(3)) {
  width: 34%;
}

.reservation-table :deep(th:nth-child(4)),
.reservation-table :deep(td:nth-child(4)) {
  width: 20%;
}

.reservation-table :deep(th),
.reservation-table :deep(td) {
  height: 60px;
  padding-top: 8px;
  padding-bottom: 8px;
  vertical-align: middle;
}

.reservation-table :deep(td) {
  overflow: hidden;
}

.reservation-table :deep(td:nth-child(1)),
.reservation-table :deep(td:nth-child(2)),
.reservation-table :deep(td:nth-child(4)) {
  white-space: nowrap;
}

.reservation-table :deep(td:nth-child(3) .reservation-summary-cell span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrap {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  font-size: 12px;
}

.pagination-total {
  margin-right: auto;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
}

.pagination-text {
  font-size: 12px;
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

.dialog-tag {
  display: inline-flex;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
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

.conflict-reservation-list {
  display: grid;
  gap: 12px;
}

.conflict-reservation-card {
  border: 1px solid #e6ebf2;
  border-radius: 16px;
  padding: 14px 16px;
  background: #fff;
  display: grid;
  gap: 8px;
}

.conflict-reservation-head,
.conflict-reservation-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.conflict-reservation-meta {
  color: #5f6f8b;
  font-size: 13px;
}

.conflict-reservation-note {
  margin: 0;
  color: #334155;
  font-size: 14px;
}

.badge-warning {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff3cd;
  color: #8a5b00;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 640px) {
  .reservation-header-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-dialog-mask {
    padding: 12px;
  }

  .detail-dialog {
    width: calc(100vw - 24px);
    max-height: calc(100vh - 24px);
    padding: 18px;
  }

  .detail-dialog-head {
    display: grid;
  }
}
</style>
