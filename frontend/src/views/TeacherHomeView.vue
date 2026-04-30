<template>
  <section class="content-grid teacher-home-grid">
    <BasePanel tag="教师端" title="今日我的预约">
      <div class="teacher-summary-grid">
        <RouterLink
          v-for="card in summaryCards"
          :key="card.key"
          class="teacher-summary-card"
          :class="card.tone"
          :to="{ name: 'my-reservations', query: { reservationType: card.queryType } }"
        >
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.note }}</small>
        </RouterLink>
        <RouterLink to="/admin/devices" class="teacher-summary-card brand">
          <span>设备查询</span>
          <strong>报修</strong>
          <small>查看本学院设备并提交报修单</small>
        </RouterLink>
      </div>
    </BasePanel>

    <BasePanel tag="说明" title="教师预约说明">
      <div class="teacher-notice-list">
        <article class="teacher-notice-card">
          <span class="teacher-notice-tag">使用须知</span>
          <h4>预约前先确认实验室开放状态</h4>
          <p>教师端保留最核心的课表预约流程，点击实验室后按节次选择空闲格子提交申请。</p>
        </article>
        <article class="teacher-notice-card">
          <span class="teacher-notice-tag">类型说明</span>
          <h4>支持三种预约类型</h4>
          <p>课程实验预约、科研训练预约、个人预约统一沿用同一套节次逻辑，仅在表单字段上做轻量差异化。</p>
        </article>
        <article class="teacher-notice-card">
          <span class="teacher-notice-tag">规则说明</span>
          <h4>预约结果会显示在“我的预约”</h4>
          <p>已提交、待审核、已通过和已完成状态都可以在“我的预约”里继续查看和取消。</p>
        </article>
      </div>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { fetchMyReservations } from '../api/reservations';
import BasePanel from '../components/BasePanel.vue';
import { useAuthStore } from '../stores/auth';
import type { ReservationDto } from '../types';

const auth = useAuthStore();
const reservations = ref<ReservationDto[]>([]);

const todayKey = computed(() => new Date().toLocaleDateString('sv-SE'));

const todayReservations = computed(() =>
  reservations.value.filter((item) => item.slots?.some((slot) => slot.reservationDate === todayKey.value)),
);

function countByType(type: number): number {
  return todayReservations.value.filter((item) => item.reservationType === type).length;
}

const summaryCards = computed(() => [
  {
    key: 'course',
    label: '课程实验预约',
    value: countByType(1),
    note: '点击查看课程实验预约',
    queryType: '1',
    tone: 'brand',
  },
  {
    key: 'research',
    label: '科研训练预约',
    value: countByType(2),
    note: '点击查看科研训练预约',
    queryType: '2',
    tone: 'success',
  },
  {
    key: 'personal',
    label: '个人预约',
    value: countByType(3),
    note: '点击查看个人预约',
    queryType: '3',
    tone: 'warning',
  },
] as const);

onMounted(async () => {
  try {
    const reservationData = await fetchMyReservations({ pageNum: 1, pageSize: 100 }, auth.token.value);
    reservations.value = reservationData.list;
  } catch {
    reservations.value = [];
  }
});
</script>

<style scoped>
.teacher-home-grid {
  display: grid;
  gap: 16px;
}

.teacher-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.teacher-summary-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(241, 245, 249, 0.9));
  color: inherit;
  text-decoration: none;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.04);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.teacher-summary-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
}

.teacher-summary-card span {
  font-size: 13px;
  font-weight: 700;
}

.teacher-summary-card strong {
  font-size: 34px;
  line-height: 1;
  color: #0f172a;
}

.teacher-summary-card small {
  color: #64748b;
}

.teacher-summary-card.brand {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.14), rgba(59, 130, 246, 0.04));
  border-color: rgba(59, 130, 246, 0.18);
}

.teacher-summary-card.brand span {
  color: #1d4ed8;
}

.teacher-summary-card.success {
  background: linear-gradient(180deg, rgba(16, 185, 129, 0.14), rgba(16, 185, 129, 0.04));
  border-color: rgba(16, 185, 129, 0.18);
}

.teacher-summary-card.success span {
  color: #047857;
}

.teacher-summary-card.warning {
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.14), rgba(245, 158, 11, 0.04));
  border-color: rgba(245, 158, 11, 0.18);
}

.teacher-summary-card.warning span {
  color: #b45309;
}

.teacher-notice-list {
  display: grid;
  gap: 12px;
}

.teacher-notice-card {
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.9);
}

.teacher-notice-tag {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.teacher-notice-card h4 {
  margin: 10px 0 6px;
  font-size: 16px;
  color: #0f172a;
}

.teacher-notice-card p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .teacher-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
