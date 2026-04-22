<template>
  <section class="content-grid">
    <BasePanel title="预约记录列表">
      <div class="reservation-toolbar">
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

        <select v-model="statusFilter" class="reservation-status-select">
          <option value="">全部状态</option>
          <option value="1">待审核</option>
          <option value="2">已通过</option>
          <option value="3">已拒绝</option>
          <option value="4">已取消</option>
          <option value="5">已完成</option>
        </select>

        <button type="button" class="ghost-btn" @click="handleReset">重置</button>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <BaseTable :headers="['实验室', '时段详情', '状态', '操作']">
        <tr v-for="item in reservationState.list" :key="item.id" class="clickable-row" @click="openReservation(item.id)">
          <td>{{ labName(item.labId) }}</td>
          <td>{{ slotSummary(item) }}</td>
          <td><span :class="getBadgeClass(statusText(item.status))">{{ statusText(item.status) }}</span></td>
          <td>
            <button v-if="canCancel(item)" type="button" class="ghost-btn small-btn" @click.stop="handleCancel(item.id)">
              取消预约
            </button>
          </td>
        </tr>
      </BaseTable>

      <div class="pagination-wrap">
        <span class="pagination-total">共{{ reservationState.total }} 条</span>
        <button type="button" class="ghost-btn small-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
          上一页
        </button>
        <span class="pagination-text">第{{ currentPage }} / {{ totalPages }} 页</span>
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

  <div v-if="detailVisible" class="detail-dialog-mask" @click.self="closeDetailDialog">
    <div class="detail-dialog" role="dialog" aria-modal="true" aria-labelledby="reservation-detail-title">
      <div class="detail-dialog-head">
        <div>
          <span class="dialog-tag">预约详情</span>
          <h3 id="reservation-detail-title">预约单信息与节次明细</h3>
        </div>
        <button type="button" class="dialog-close" @click="closeDetailDialog">关闭</button>
      </div>

      <div v-if="selectedReservation" class="detail-stack">
        <div class="detail-card">
          <h4>{{ labName(selectedReservation.labId) }}</h4>
          <p>{{ selectedReservation.reservationNo }}</p>
        </div>

        <div class="detail-list">
          <div><strong>状态</strong><span>{{ statusText(selectedReservation.status) }}</span></div>
          <div><strong>类型</strong><span>{{ reservationTypeText(selectedReservation.reservationType) }}</span></div>
          <div><strong>用途</strong><span>{{ selectedReservation.usagePurpose }}</span></div>
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
import { useAuthStore } from '../stores/auth';
import type { LabDto, PageData, ReservationAuditLogDto, ReservationDto } from '../types';
import { getBadgeClass } from '../utils/format';
import { reservationTypeLabel } from '../utils/reservation';

const auth = useAuthStore();
const route = useRoute();

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
const reservationTypeFilter = ref<'all' | 'course' | 'research' | 'personal'>('all');
const currentPage = ref(1);
const pageSize = ref(8);

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
  return `${parts.slice(0, 2).join('，')} 等（共${parts.length}项）`;
}

function statusText(status: number): '待审核' | '已通过' | '已拒绝' | '已取消' | '已完成' {
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
  detailVisible.value = true;
  try {
    const [reservation, logs] = await Promise.all([
      fetchReservationById(id, auth.token.value),
      fetchReservationAuditLogs(id, auth.token.value),
    ]);
    selectedReservation.value = reservation;
    auditLogs.value = logs;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '预约详情加载失败。';
  }
}

function closeDetailDialog(): void {
  detailVisible.value = false;
}

async function handleCancel(id: number): Promise<void> {
  try {
    await cancelReservation(id, auth.token.value);
    message.value = '预约已取消。';
    selectedReservation.value = null;
    detailVisible.value = false;

    const shouldFallbackPage =
      reservationState.value.list.length === 1 && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value;
    await loadReservations(shouldFallbackPage);
  } catch (error) {
    message.value = error instanceof Error ? error.message : '取消预约失败。';
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
  margin-right: 12px;
}

.reservation-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
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

.type-filter-pill {
  min-width: 118px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  background: #ffffff;
  color: #0f172a;
  padding: 14px 22px;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.type-filter-pill.active {
  border-color: rgba(59, 130, 246, 0.42);
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12);
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
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: #ffffff;
  padding: 16px;
  box-shadow: 0 28px 48px rgba(15, 23, 42, 0.3);
}

.detail-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
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
  font-size: 20px;
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
  .detail-list {
    grid-template-columns: 1fr;
  }
}
</style>
