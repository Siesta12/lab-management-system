<template>
  <section class="card-grid metrics-grid">
    <article class="metric-card brand">
      <span>我的预约</span>
      <strong>{{ myReservationCount }}</strong>
      <small>当前账号下的预约总数</small>
    </article>
    <article class="metric-card warning">
      <span>待审批预约</span>
      <strong>{{ pendingCount }}</strong>
      <small>建议及时关注审批状态</small>
    </article>
    <article class="metric-card success">
      <span>信用分</span>
      <strong>{{ profile?.creditScore ?? '--' }}</strong>
      <small>保持良好信用可提升预约体验</small>
    </article>
    <article class="metric-card accent">
      <span>违规次数</span>
      <strong>{{ profile?.violationCount ?? '--' }}</strong>
      <small>可在“我的信用”查看详细记录</small>
    </article>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="快捷入口" title="常用功能">
      <div class="quick-grid">
        <RouterLink to="/labs" class="quick-card">
          <strong>实验室查询</strong>
          <span>查看实验室详情、设备与耗材信息。</span>
        </RouterLink>
        <RouterLink to="/labs" class="quick-card">
          <strong>课表式预约</strong>
          <span>在实验室详情页的“未来三周课表”中按节次选择空闲格子提交预约。</span>
        </RouterLink>
        <RouterLink to="/my-reservations" class="quick-card">
          <strong>我的预约</strong>
          <span>查看预约单、节次明细、审核日志，并可取消可取消的预约。</span>
        </RouterLink>
        <RouterLink to="/my-credit" class="quick-card">
          <strong>我的信用</strong>
          <span>查看信用分、违规次数与扣分原因。</span>
        </RouterLink>
      </div>
    </BasePanel>

    <BasePanel tag="提醒" title="本周使用提示">
      <div class="alert-list">
        <div class="alert-item">
          <span class="alert-tag">预约提示</span>
          <h4>先看课表再预约</h4>
          <p>预约基于“日期 + 节次”，请在未来三周课表中选择空闲且开放的节次提交申请。</p>
        </div>
        <div class="alert-item">
          <span class="alert-tag">守时提示</span>
          <h4>已通过预约请按时到场</h4>
          <p>迟到、爽约或违规使用会影响信用分，并可能降低后续预约优先级。</p>
        </div>
      </div>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { fetchMyReservations } from '../api/reservations';
import { fetchMyProfile } from '../api/users';
import BasePanel from '../components/BasePanel.vue';
import { useAuthStore } from '../stores/auth';
import type { ReservationDto, UserVO } from '../types';

const auth = useAuthStore();
const profile = ref<UserVO | null>(null);
const reservations = ref<ReservationDto[]>([]);

const myReservationCount = computed(() => reservations.value.length);
const pendingCount = computed(() => reservations.value.filter((item) => item.status === 1).length);

onMounted(async () => {
  const [profileData, reservationData] = await Promise.all([
    fetchMyProfile(auth.token.value),
    fetchMyReservations({ pageNum: 1, pageSize: 20 }, auth.token.value),
  ]);

  profile.value = profileData;
  reservations.value = reservationData.list;
});
</script>

