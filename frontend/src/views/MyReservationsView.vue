<template>
  <section class="content-grid">
    <BasePanel>
      <div class="reservation-toolbar">
        <div class="reservation-toolbar-top">
          <div class="reservation-toolbar-title">预约记录列表</div>
        </div>
        <div class="reservation-toolbar-row">
          <div v-if="!isStudent" class="type-filter-row">
            <button
              v-for="tab in typeTabs"
              :key="tab.value"
              type="button"
              class="type-filter-pill"
              :class="{ active: reservationTypeFilter === tab.value }"
              @click="setTypeFilter(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>

          <select v-model="statusFilter" class="reservation-status-select toolbar-select">
            <option value="">全部状态</option>
            <option value="1">待审核</option>
            <option value="2">已通过</option>
            <option value="3">已拒绝</option>
            <option value="4">已取消</option>
            <option value="5">已完成</option>
          </select>

          <button type="button" class="ghost-btn toolbar-ghost-btn" @click="handleReset">重置</button>
        </div>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <BaseTable :headers="['实验室', '时间段详情', '状态', '操作']">
        <tr v-for="item in reservationState.list" :key="item.id" class="clickable-row" @click="openReservation(item.id)">
          <td>{{ labName(item.labId) }}</td>
          <td>{{ slotSummary(item) }}</td>
          <td><span :class="getBadgeClass(statusText(item.status))">{{ statusText(item.status) }}</span></td>
          <td>
            <button v-if="canCancel(item)" type="button" class="ghost-btn small-btn" @click.stop="openCancelConfirm(item)">
              取消预约
            </button>
          </td>
        </tr>
      </BaseTable>

      <div class="pagination-wrap">
        <span class="pagination-total">共 {{ reservationState.total }} 条</span>
        <button type="button" class="ghost-btn small-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
          上一页
        </button>
        <span class="pagination-text">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="currentPage >= totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </BasePanel>
  </section>

  <div v-if="cancelConfirmVisible" class="detail-dialog-mask" @click.self="closeCancelConfirm">
    <div class="detail-dialog cancel-dialog" role="dialog" aria-modal="true" aria-labelledby="cancel-confirm-title">
      <div class="detail-dialog-head">
        <div>
          <span class="dialog-tag">二次确认</span>
          <h3 id="cancel-confirm-title">确认取消预约</h3>
        </div>
        <button type="button" class="dialog-close" @click="closeCancelConfirm">关闭</button>
      </div>

      <div v-if="cancelTarget" class="cancel-confirm-body">
        <div class="cancel-summary-card">
          <span class="cancel-summary-label">本次预约</span>
          <strong>{{ labName(cancelTarget.labId) }}</strong>
          <p>{{ slotSummary(cancelTarget) }}</p>
        </div>

        <div class="cancel-rule-card">
          <p class="cancel-confirm-text">确认后将立即释放该时段，请留意取消时机对信用分的影响。</p>
          <div class="cancel-rule-grid">
            <div class="cancel-rule-item safe">
              <span>提前取消</span>
              <strong>30 分钟以上</strong>
              <small>不扣分</small>
            </div>
            <div class="cancel-rule-item warning">
              <span>临近取消</span>
              <strong>30 分钟内</strong>
              <small>扣 3 分</small>
            </div>
            <div class="cancel-rule-item danger">
              <span>开始后</span>
              <strong>不可取消</strong>
              <small>需按原预约使用</small>
            </div>
          </div>
        </div>
      </div>

      <div class="dialog-actions cancel-confirm-actions">
        <button type="button" class="ghost-btn cancel-secondary-btn" @click="closeCancelConfirm">再想想</button>
        <button type="button" class="primary-btn cancel-primary-btn" :disabled="canceling" @click="confirmCancel">
          {{ canceling ? '取消中...' : '确认取消' }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="detailVisible" class="detail-dialog-mask" @click.self="closeDetailDialog">
    <div class="detail-dialog" role="dialog" aria-modal="true" aria-labelledby="reservation-detail-title">
      <div class="detail-dialog-head">
        <div>
          <span class="dialog-tag">预约详情</span>
          <h3 id="reservation-detail-title">预约单信息与节次明细</h3>
        </div>
        <div class="detail-dialog-actions">
          <button
            v-if="selectedReservation && canCancel(selectedReservation)"
            type="button"
            class="ghost-btn detail-cancel-btn"
            @click="openCancelFromDetail"
          >
            取消预约
          </button>
          <button type="button" class="dialog-close" @click="closeDetailDialog">关闭</button>
        </div>
      </div>

      <div v-if="detailLoading" class="info-text">正在加载预约详情...</div>

      <div v-else-if="selectedReservation" class="detail-stack">
        <div class="detail-card">
          <h4>{{ labName(selectedReservation.labId) }}</h4>
          <p>{{ selectedReservation.reservationNo }}</p>
        </div>

        <div class="detail-list">
          <div><strong>状态</strong><span>{{ statusText(selectedReservation.status) }}</span></div>
          <div><strong>类型</strong><span>{{ reservationTypeText(selectedReservation.reservationType) }}</span></div>
          <div><strong>用途</strong><span>{{ selectedReservation.usagePurpose || '未填写' }}</span></div>
          <div><strong>课程/项目</strong><span>{{ selectedReservation.courseOrProjectName || '未填写' }}</span></div>
          <div><strong>参与人数</strong><span>{{ selectedReservation.participantCount }} 人</span></div>
          <div><strong>联系电话</strong><span>{{ selectedReservation.contactPhone || '未填写' }}</span></div>
          <div><strong>驳回原因</strong><span>{{ selectedReservation.rejectReason || '无' }}</span></div>
        </div>

        <div class="detail-section">
          <h5>节次明细</h5>
          <ul class="bullet-list compact-list">
            <li v-for="slot in selectedReservation.slots" :key="slot.id">
              {{ slot.reservationDate }} · {{ slot.periodName }} · {{ slot.slotStatus === 1 ? '占用' : '已取消' }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <h5>审核日志</h5>
          <ul class="bullet-list compact-list">
            <li v-for="log in auditLogs" :key="log.id">
              {{ log.createdAt }} · {{ auditActionText(log.auditAction) }} · {{ log.auditComment || '无备注' }}
            </li>
          </ul>
        </div>
      </div>

      <p v-else class="info-text">请选择一条预约记录查看详情。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { getPrimaryRole } from '../access';
import { fetchReservationAuditLogs } from '../api/reservationAuditLogs';
import { cancelReservation, fetchMyReservations, fetchReservationById } from '../api/reservations';
import { fetchLabs } from '../api/labs';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useGlobalToast } from '../composables/useGlobalToast';
import { useAuthStore } from '../stores/auth';
import type { LabDto, PageData, ReservationAuditLogDto, ReservationDto } from '../types';
import { getBadgeClass } from '../utils/format';
import { reservationTypeLabel } from '../utils/reservation';

const auth = useAuthStore();
const route = useRoute();
const { showToast } = useGlobalToast();

const message = ref('');
const statusFilter = ref('');
const labs = ref<LabDto[]>([]);
const reservationState = ref<PageData<ReservationDto>>({
  list: [],
  total: 0,
  pageNum: 1,
  pageSize: 8,
});
const selectedReservation = ref<ReservationDto | null>(null);
const auditLogs = ref<ReservationAuditLogDto[]>([]);
const detailVisible = ref(false);
const detailLoading = ref(false);
const cancelConfirmVisible = ref(false);
const cancelTarget = ref<ReservationDto | null>(null);
const canceling = ref(false);
const reservationTypeFilter = ref<'all' | 'course' | 'research' | 'personal'>('all');
const currentPage = ref(1);
const pageSize = ref(8);
let detailRequestVersion = 0;

const primaryRole = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));
const isStudent = computed(() => primaryRole.value === 'STUDENT');
const totalPages = computed(() => Math.max(Math.ceil((reservationState.value.total || 0) / pageSize.value), 1));

const typeTabs = [
  { value: 'all', label: '全部预约' },
  { value: 'course', label: '课程实验预约' },
  { value: 'research', label: '科研训练预约' },
  { value: 'personal', label: '个人预约' },
] as const;

function reservationTypeQueryValue(): number | undefined {
  if (reservationTypeFilter.value === 'course') return 1;
  if (reservationTypeFilter.value === 'research') return 2;
  if (reservationTypeFilter.value === 'personal') return 3;
  return undefined;
}

function setTypeFilter(value: 'all' | 'course' | 'research' | 'personal'): void {
  reservationTypeFilter.value = value;
}

function handleReset(): void {
  statusFilter.value = '';
  reservationTypeFilter.value = isStudent.value ? 'personal' : 'all';
}

function changePage(pageNum: number): void {
  if (pageNum < 1 || pageNum > totalPages.value || pageNum === currentPage.value) {
    return;
  }
  void loadReservations(pageNum);
}

function labName(id: number): string {
  return labs.value.find((item) => item.id === id)?.labName ?? `实验室${id}`;
}

function slotSummary(item: ReservationDto): string {
  const parts = item.slots?.map((slot) => `${slot.reservationDate} ${slot.periodName}`) ?? [];
  if (parts.length <= 2) return parts.join('，') || '--';
  return parts.slice(0, 2).join('，') + ' 等（共' + parts.length + '项）';
}

function statusText(status: number): string {
  if (status === 2) return '已通过';
  if (status === 3) return '已拒绝';
  if (status === 4) return '已取消';
  if (status === 5) return '已完成';
  return '待审核';
}

function reservationTypeText(type: number): string {
  return reservationTypeLabel(type);
}

function auditActionText(action: number): string {
  if (action === 1) return '提交申请';
  if (action === 2) return '审核通过';
  if (action === 3) return '审核驳回';
  if (action === 4) return '取消预约';
  if (action === 5) return '完成/结束';
  return '状态变更';
}

function canCancel(item: ReservationDto): boolean {
  return item.status === 1 || item.status === 2;
}

async function loadReservations(pageNum = currentPage.value): Promise<void> {
  try {
    currentPage.value = Math.max(pageNum, 1);
    const [reservationData, labData] = await Promise.all([
      fetchMyReservations(
        {
          pageNum: currentPage.value,
          pageSize: pageSize.value,
          status: statusFilter.value ? Number(statusFilter.value) : undefined,
          reservationType: reservationTypeQueryValue(),
        },
        auth.token.value,
      ),
      fetchLabs({ pageNum: 1, pageSize: 100 }, auth.token.value),
    ]);

    reservationState.value = reservationData;
    currentPage.value = reservationData.pageNum || currentPage.value;
    pageSize.value = reservationData.pageSize || pageSize.value;
    labs.value = labData.list;
    message.value = '';

    const requestedId = Number(route.query.reservationId ?? route.query.id);
    if (Number.isFinite(requestedId) && requestedId > 0) {
      await openReservation(requestedId);
    }
  } catch (error) {
    message.value = error instanceof Error ? error.message : '预约记录加载失败。';
  }
}

async function openReservation(id: number): Promise<void> {
  const requestVersion = ++detailRequestVersion;
  detailVisible.value = true;
  detailLoading.value = true;
  selectedReservation.value = null;
  auditLogs.value = [];
  try {
    const [reservation, logs] = await Promise.all([
      fetchReservationById(id, auth.token.value),
      fetchReservationAuditLogs(id, auth.token.value),
    ]);
    if (requestVersion !== detailRequestVersion) {
      return;
    }
    selectedReservation.value = reservation;
    auditLogs.value = logs;
  } catch (error) {
    if (requestVersion !== detailRequestVersion) {
      return;
    }
    message.value = error instanceof Error ? error.message : '预约详情加载失败。';
  } finally {
    if (requestVersion === detailRequestVersion) {
      detailLoading.value = false;
    }
  }
}

function closeDetailDialog(): void {
  detailRequestVersion += 1;
  detailLoading.value = false;
  selectedReservation.value = null;
  auditLogs.value = [];
  detailVisible.value = false;
}

function openCancelConfirm(item: ReservationDto): void {
  cancelTarget.value = item;
  detailVisible.value = false;
  cancelConfirmVisible.value = true;
}

function openCancelFromDetail(): void {
  if (!selectedReservation.value || canceling.value) {
    return;
  }
  openCancelConfirm(selectedReservation.value);
}

function resetCancelConfirmState(): void {
  cancelConfirmVisible.value = false;
  cancelTarget.value = null;
}

function closeCancelConfirm(): void {
  if (canceling.value) {
    return;
  }
  resetCancelConfirmState();
}

async function confirmCancel(): Promise<void> {
  if (!cancelTarget.value) {
    return;
  }
  const id = cancelTarget.value.id;
  canceling.value = true;
  try {
    await cancelReservation(id, auth.token.value);
    selectedReservation.value = null;
    auditLogs.value = [];
    detailLoading.value = false;
    detailVisible.value = false;
    resetCancelConfirmState();

    const shouldFallbackPage =
      reservationState.value.list.length === 1 && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value;
    await loadReservations(shouldFallbackPage);
    showToast('success', '预约已取消');
  } catch (error) {
    message.value = error instanceof Error ? error.message : '取消预约失败。';
  } finally {
    canceling.value = false;
  }
}

watch(
  () => route.query.reservationType,
  () => {
    if (isStudent.value) {
      reservationTypeFilter.value = 'personal';
      return;
    }

    const type = route.query.reservationType;
    if (type === '1') {
      reservationTypeFilter.value = 'course';
      return;
    }
    if (type === '2') {
      reservationTypeFilter.value = 'research';
      return;
    }
    if (type === '3') {
      reservationTypeFilter.value = 'personal';
      return;
    }
    reservationTypeFilter.value = 'all';
  },
  { immediate: true },
);

watch(
  () => route.query.status,
  () => {
    const status = route.query.status;
    if (status === '1') {
      statusFilter.value = '1';
      return;
    }
    if (status === '2') {
      statusFilter.value = '2';
      return;
    }
    if (status === '3') {
      statusFilter.value = '3';
      return;
    }
    if (status === '4') {
      statusFilter.value = '4';
      return;
    }
    if (status === '5') {
      statusFilter.value = '5';
      return;
    }
  },
  { immediate: true },
);

watch([statusFilter, reservationTypeFilter], () => {
  void loadReservations(1);
});

onMounted(() => {
  if (isStudent.value) {
    reservationTypeFilter.value = 'personal';
  }
  void loadReservations(1);
});
</script>

<style scoped>
.table-wrap table {
  font-size: 13px;
}

.table-wrap th,
.table-wrap td {
  padding: 10px 8px;
}

.type-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.reservation-toolbar {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
}

.reservation-toolbar-top {
  display: flex;
  justify-content: flex-start;
}

.reservation-toolbar-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
}

.reservation-toolbar-title {
  color: #0f172a;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 700;
}

.reservation-status-select {
  min-width: 160px;
  border: 1px solid #dbe4ee;
  border-radius: 16px;
  background: #fff;
  color: #0f172a;
  height: 42px;
  padding: 0 42px 0 14px;
  box-shadow: none;
  font-size: 14px;
}

.type-filter-pill {
  min-width: 118px;
  height: 42px;
  border-radius: 999px;
  border: 1px solid #dbe4ee;
  background: #ffffff;
  color: #1e293b;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.type-filter-pill.active {
  border-color: rgba(37, 99, 235, 0.22);
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
}

.reservation-toolbar-row > .ghost-btn {
  min-width: 86px;
  height: 42px;
  padding: 0 18px;
}

.pagination-wrap {
  margin-top: 8px;
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
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(2, 6, 23, 0.45);
}

.detail-dialog {
  width: min(860px, 96vw);
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  background: #ffffff;
  padding: 24px;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.28);
}

.detail-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-tag {
  display: inline-flex;
  padding: 7px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(147, 197, 253, 0.28));
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.detail-dialog-head h3 {
  margin: 12px 0 0;
  font-size: 34px;
  line-height: 1.08;
  color: #0f172a;
}

.dialog-close {
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  border-radius: 999px;
  padding: 10px 18px;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.dialog-close:hover {
  border-color: rgba(59, 130, 246, 0.32);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.detail-dialog-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-cancel-btn {
  min-width: 126px;
  padding: 10px 18px;
  border-radius: 999px;
  color: #dc2626;
  border-color: rgba(248, 113, 113, 0.28);
  background: rgba(254, 242, 242, 0.92);
}

.cancel-dialog {
  width: min(860px, 94vw);
}

.cancel-confirm-body {
  display: grid;
  gap: 16px;
}

.cancel-summary-card {
  display: grid;
  gap: 6px;
  padding: 18px 20px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, rgba(147, 197, 253, 0.32), transparent 34%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(241, 245, 249, 0.92));
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.cancel-summary-label {
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
}

.cancel-summary-card strong {
  font-size: 26px;
  line-height: 1.15;
  color: #0f172a;
}

.cancel-summary-card p {
  margin: 0;
  font-size: 18px;
  color: #334155;
}

.cancel-rule-card {
  display: grid;
  gap: 16px;
  padding: 18px 20px 20px;
  border-radius: 20px;
  border: 1px solid rgba(248, 113, 113, 0.18);
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.95), rgba(255, 255, 255, 0.98));
}

.cancel-confirm-text {
  margin: 0;
  font-size: 17px;
  line-height: 1.7;
  color: #334155;
}

.cancel-rule-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.cancel-rule-item {
  display: grid;
  gap: 4px;
  min-height: 110px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.88);
}

.cancel-rule-item span {
  font-size: 13px;
  font-weight: 700;
}

.cancel-rule-item strong {
  font-size: 22px;
  line-height: 1.15;
  color: #0f172a;
}

.cancel-rule-item small {
  font-size: 14px;
  color: #475569;
}

.cancel-rule-item.safe {
  border-color: rgba(45, 212, 191, 0.28);
  background: rgba(240, 253, 250, 0.96);
}

.cancel-rule-item.safe span {
  color: #0f766e;
}

.cancel-rule-item.warning {
  border-color: rgba(251, 191, 36, 0.28);
  background: rgba(255, 251, 235, 0.96);
}

.cancel-rule-item.warning span {
  color: #b45309;
}

.cancel-rule-item.danger {
  border-color: rgba(248, 113, 113, 0.24);
  background: rgba(254, 242, 242, 0.96);
}

.cancel-rule-item.danger span {
  color: #dc2626;
}

.cancel-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 22px;
}

.cancel-secondary-btn,
.cancel-primary-btn {
  min-width: 148px;
  padding: 14px 22px;
  border-radius: 999px;
}

.cancel-primary-btn {
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.24);
}

.detail-stack {
  display: grid;
  gap: 14px;
}

.detail-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  padding: 16px 18px;
  background: rgba(248, 250, 252, 0.9);
}

.detail-card h4 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.detail-card p {
  margin: 8px 0 0;
  color: #64748b;
}

.detail-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.detail-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.9);
}

.detail-list strong {
  color: #475569;
}

.detail-list span {
  text-align: right;
  color: #0f172a;
}

.detail-section {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  padding: 16px 18px;
  background: #ffffff;
}

.detail-section h5 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #0f172a;
}

@media (max-width: 900px) {
  .reservation-toolbar-row {
    justify-content: flex-start;
  }

  .detail-list {
    grid-template-columns: 1fr;
  }

  .detail-dialog {
    padding: 20px;
  }

  .detail-dialog-head h3 {
    font-size: 28px;
  }

  .cancel-rule-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .detail-dialog-mask {
    padding: 12px;
  }

  .detail-dialog {
    padding: 18px;
    border-radius: 20px;
  }

  .detail-dialog-head {
    gap: 12px;
  }

  .detail-dialog-head h3 {
    font-size: 24px;
  }

  .detail-dialog-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .cancel-summary-card strong {
    font-size: 22px;
  }

  .cancel-summary-card p,
  .cancel-confirm-text {
    font-size: 15px;
  }

  .cancel-confirm-actions {
    flex-direction: column-reverse;
  }

  .cancel-secondary-btn,
  .cancel-primary-btn {
    width: 100%;
  }
}
</style>
