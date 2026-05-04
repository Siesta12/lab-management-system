<template>
  <BaseTable
    class="reservation-table decision-table"
    :headers="['实验室', '申请人', '申请人身份', '预约类型', '信誉分', '时间详情', '当前状态', '排序说明 / 推荐结果', '操作']"
  >
    <tr v-if="loading">
      <td colspan="9" class="state-cell">正在加载预约数据...</td>
    </tr>
    <tr v-else-if="!rows.length">
      <td colspan="9" class="state-cell">{{ emptyText }}</td>
    </tr>
    <tr
      v-for="row in rows"
      v-else
      :key="row.id"
      class="clickable-row"
      @click="$emit('view', row.id)"
    >
      <td>
        <div class="lab-cell">
          <strong>{{ row.labName }}</strong>
          <span class="sub-text">{{ row.reservationNo }}</span>
        </div>
      </td>
      <td>
        <div class="user-cell">
          <strong>{{ row.applicantName }}</strong>
          <span v-if="row.courseOrProjectName" class="sub-text">{{ row.courseOrProjectName }}</span>
        </div>
      </td>
      <td><span :class="applicantRoleTagClass(row.applicantRole)">{{ applicantRoleLabel(row.applicantRole) }}</span></td>
      <td>
        <div class="type-stack">
          <span :class="reservationTypeTagClass(row.reservationType)">{{ reservationTypeLabel(row.reservationType, row.applicantRole) }}</span>
          <span class="priority-pill">{{ row.priorityLabel }}</span>
        </div>
      </td>
      <td>
        <span v-if="row.applicantRole === 'student'" class="credit-badge">信誉 {{ row.creditScore ?? '--' }}</span>
        <span v-else class="muted-text">-</span>
      </td>
      <td>
        <div class="time-cell">
          <strong>{{ row.timeDetail }}</strong>
          <span class="sub-text">{{ row.submitTime || '提交时间待补充' }}</span>
        </div>
      </td>
      <td>
        <div class="status-stack">
          <span :class="statusTagClass(row.status)">{{ statusLabel(row.status) }}</span>
          <span v-if="row.hasConflict" class="status-tag status-conflict">冲突待审核</span>
        </div>
      </td>
      <td>
        <div class="decision-cell">
          <span v-if="row.isRecommended" class="recommend-pill">系统推荐</span>
          <span class="decision-text">{{ row.rankReason }}</span>
        </div>
      </td>
      <td>
        <div class="action-cell" @click.stop>
          <button type="button" class="ghost-btn small-btn" @click="$emit('view', row.id)">查看详情</button>
          <button
            v-if="row.status === 1"
            type="button"
            class="primary-btn small-btn"
            @click="$emit('review', row.id)"
          >
            {{ row.isRecommended ? '推荐通过' : '审核处理' }}
          </button>
        </div>
      </td>
    </tr>
  </BaseTable>
</template>

<script setup lang="ts">
import BaseTable from '../BaseTable.vue';
import type { ReservationRowViewModel } from '../../utils/adminReservations';
import {
  applicantRoleLabel,
  applicantRoleTagClass,
  reservationTypeLabel,
  reservationTypeTagClass,
  statusLabel,
  statusTagClass,
} from '../../utils/adminReservations';

defineProps<{
  rows: ReservationRowViewModel[];
  loading: boolean;
  emptyText: string;
}>();

defineEmits<{
  (event: 'view', id: number): void;
  (event: 'review', id: number): void;
}>();
</script>

<style scoped>
.decision-table :deep(table) {
  table-layout: fixed;
}

.decision-table :deep(th),
.decision-table :deep(td) {
  padding-top: 10px;
  padding-bottom: 10px;
  vertical-align: middle;
}

.state-cell {
  text-align: center;
  color: #64748b;
  padding: 28px 12px !important;
}

.lab-cell,
.user-cell,
.time-cell,
.decision-cell {
  display: grid;
  gap: 4px;
}

.type-stack,
.status-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sub-text,
.muted-text {
  font-size: 12px;
  color: #64748b;
}

.decision-text {
  font-size: 13px;
  line-height: 1.55;
  color: #334155;
}

.priority-pill,
.recommend-pill,
.credit-badge,
.type-tag,
.role-tag,
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

.priority-pill {
  background: rgba(15, 23, 42, 0.06);
  color: #334155;
}

.recommend-pill {
  width: fit-content;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.credit-badge {
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
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

.action-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 920px) {
  .decision-table :deep(table) {
    min-width: 1180px;
  }
}
</style>
