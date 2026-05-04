<template>
  <article class="conflict-card">
    <header class="conflict-card-head">
      <div class="conflict-card-copy">
        <h4>{{ group.labName }}</h4>
        <p>{{ group.reservationDate }} / {{ group.periodName }}</p>
      </div>
      <div class="conflict-card-summary">
        <span class="summary-pill">冲突 {{ group.conflictCount }} 人</span>
        <span class="summary-note">系统已按审核优先级自动排序</span>
      </div>
    </header>

    <div class="slot-meta">
      <span>实验室：{{ group.labName }}</span>
      <span>时间：{{ group.reservationDate }} {{ group.periodName }}</span>
      <span v-if="group.startTime && group.endTime">节次：{{ group.startTime }} - {{ group.endTime }}</span>
    </div>

    <div class="ranked-list">
      <section
        v-for="item in visibleReservations"
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
              <span :class="reservationTypeTagClass(item.reservationType)">{{ reservationTypeLabel(item.reservationType, item.applicantRole) }}</span>
              <span v-if="item.isRecommended" class="recommend-pill">系统推荐</span>
              <span v-if="item.priorityRank === 1" class="priority-pill">最高优先级</span>
            </div>
            <span :class="statusTagClass(item.status)">{{ statusLabel(item.status) }}</span>
          </div>

          <div class="rank-meta">
            <span v-if="item.applicantRole === 'student'" class="credit-badge">信誉 {{ item.creditScore ?? '--' }}</span>
            <span>{{ item.submitTime || '提交时间待补充' }}</span>
            <span>{{ item.courseOrProjectName || item.usagePurpose || '未填写用途说明' }}</span>
          </div>

          <p class="rank-reason">{{ item.rankReason }}</p>

          <div class="rank-actions">
            <button type="button" class="ghost-btn small-btn" @click="$emit('view', item.id)">查看详情</button>
            <button
              v-if="item.status === 1"
              type="button"
              class="primary-btn small-btn"
              @click="$emit('review', item.id)"
            >
              {{ item.isRecommended ? '推荐通过' : '审核处理' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ConflictGroupViewModel } from '../../utils/adminReservations';
import {
  applicantRoleLabel,
  applicantRoleTagClass,
  reservationTypeLabel,
  reservationTypeTagClass,
  statusLabel,
  statusTagClass,
} from '../../utils/adminReservations';

const props = defineProps<{
  group: ConflictGroupViewModel;
  statusFilter: string;
}>();

defineEmits<{
  (event: 'view', id: number): void;
  (event: 'review', id: number): void;
}>();

const visibleReservations = computed(() => {
  if (!props.statusFilter) {
    return props.group.reservations;
  }
  return props.group.reservations.filter((item) => String(item.status) === props.statusFilter);
});
</script>

<style scoped>
.conflict-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  background: #fff;
  padding: 18px;
  display: grid;
  gap: 16px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
}

.conflict-card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.conflict-card-copy h4 {
  margin: 0;
  font-size: 20px;
}

.conflict-card-copy p {
  margin: 6px 0 0;
  color: #64748b;
}

.conflict-card-summary {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.summary-pill,
.summary-note,
.recommend-pill,
.priority-pill,
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

.summary-pill {
  background: rgba(249, 115, 22, 0.12);
  color: #c2410c;
}

.summary-note {
  background: rgba(15, 23, 42, 0.05);
  color: #475569;
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
  gap: 12px;
}

.rank-row {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.84);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.rank-row.recommended {
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.95), rgba(248, 250, 252, 0.95));
  border-color: rgba(16, 185, 129, 0.24);
}

.rank-index {
  width: 62px;
  height: 62px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: #0f172a;
  color: #fff;
  font-weight: 800;
  font-size: 18px;
}

.rank-main {
  display: grid;
  gap: 10px;
}

.rank-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.rank-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.rank-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.rank-reason {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
}

.rank-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.recommend-pill {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.priority-pill {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
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

@media (max-width: 720px) {
  .conflict-card-head,
  .rank-head {
    display: grid;
  }

  .conflict-card-summary {
    justify-items: start;
  }

  .rank-row {
    grid-template-columns: 1fr;
  }
}
</style>
