<template>
  <section class="card-grid metrics-grid">
    <article class="metric-card brand">
      <span>当前信用分</span>
      <strong>{{ profile?.creditScore ?? '--' }}</strong>
      <small>基于预约表现情况自动变更</small>
    </article>
    <article class="metric-card warning">
      <span>违纪次数</span>
      <strong>{{ profile?.violationCount ?? '--' }}</strong>
      <small>多次违纪将会降低预约优先级</small>
    </article>
    <article class="metric-card success">
      <span>最新得分</span>
      <strong>{{ latestScoreChange }}</strong>
      <small>鏌ョ湅杩濊璁板綍浜嗚В鎵ｅ垎鍘熷洜</small>
    </article>
    <article class="metric-card accent">
      <span>璁板綍鎬绘暟</span>
      <strong>{{ violations.length }}</strong>
      <small>当前用户可查看自己的违纪详情</small>
    </article>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="信用说明" title="预约及使用相关说明">
      <ul class="bullet-list">
        <li>按时到达、按时结束使用，有助于保持良好的信用记录。</li>
        <li>迟到、早退或违纪使用实验室，将被记录到黑名单并扣量信用分。</li>
        <li>信用分较低时，预约审核优先级可能会降低。</li>
      </ul>
    </BasePanel>

    <BasePanel tag="违纪记录" title="我的违纪记录列表">
      <BaseTable :headers="['时间', '违纪类型', '扣分', '说明']">
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
  if (type === 1) return '损坏';
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

