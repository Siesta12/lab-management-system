<template>
  <section class="card-grid metrics-grid">
    <article class="metric-card brand">
      <span>当前信用分</span>
      <strong>{{ profile?.creditScore ?? '--' }}</strong>
      <small>基于预约履约情况动态变化</small>
    </article>
    <article class="metric-card warning">
      <span>违规次数</span>
      <strong>{{ profile?.violationCount ?? '--' }}</strong>
      <small>多次违规会影响预约优先级</small>
    </article>
    <article class="metric-card success">
      <span>最近扣分</span>
      <strong>{{ latestScoreChange }}</strong>
      <small>查看违规记录了解扣分原因</small>
    </article>
    <article class="metric-card accent">
      <span>记录总数</span>
      <strong>{{ violations.length }}</strong>
      <small>当前账号可查看自己的违规明细</small>
    </article>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="信用说明" title="预约与信用关系">
      <ul class="bullet-list">
        <li>按时签到、按时结束使用，有助于维持良好的信用记录。</li>
        <li>爽约、迟到或违规使用实验室，会被记录到违规列表并扣减信用分。</li>
        <li>信用分较低时，预约审核优先级可能下降。</li>
      </ul>
    </BasePanel>

    <BasePanel tag="违规记录" title="我的违规记录列表">
      <BaseTable :headers="['时间', '违规类型', '扣分', '备注']">
        <tr v-for="item in violations" :key="item.id">
          <td>{{ item.createdAt }}</td>
          <td>{{ violationTypeText(item.violationType) }}</td>
          <td>{{ item.scoreChange }}</td>
          <td>{{ item.remark }}</td>
        </tr>
      </BaseTable>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchMyProfile } from '../api/users';
import { fetchMyViolations } from '../api/violations';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useAuthStore } from '../stores/auth';
import type { UserVO, ViolationRecordDto } from '../types';

const auth = useAuthStore();
const profile = ref<UserVO | null>(null);
const violations = ref<ViolationRecordDto[]>([]);

const latestScoreChange = computed(() => {
  if (!violations.value.length) {
    return '0';
  }

  return String(violations.value[0].scoreChange);
});

function violationTypeText(type: number): string {
  if (type === 1) return '爽约';
  if (type === 2) return '迟到';
  if (type === 3) return '违规使用';
  return '其他';
}

onMounted(async () => {
  const [profileData, violationData] = await Promise.all([
    fetchMyProfile(auth.token.value),
    fetchMyViolations(auth.token.value, 1, 20),
  ]);
  profile.value = profileData;
  violations.value = violationData.list;
});
</script>
