<template>
  <section class="card-grid metrics-grid">
    <article class="metric-card brand">
      <span>本周预约</span>
      <strong>{{ reservationCount }}</strong>
      <small>当前账号下的预约总数</small>
    </article>
    <article class="metric-card success">
      <span>已通过</span>
      <strong>{{ approvedCount }}</strong>
      <small>可直接安排使用</small>
    </article>
    <article class="metric-card warning">
      <span>待审批</span>
      <strong>{{ pendingCount }}</strong>
      <small>建议及时补充用途与项目说明</small>
    </article>
    <article class="metric-card accent">
      <span>可用实验室</span>
      <strong>{{ labCount }}</strong>
      <small>当前可查看实验室数量</small>
    </article>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="教师端" title="推荐操作顺序">
      <ol class="number-list">
        <li>进入“实验室查询”，在实验室详情页查看未来三周课表。</li>
        <li>在课表中按“日期 + 节次”选择空闲格子，填写用途与人数后提交预约。</li>
        <li>如遇维护或冲突，可点击“推荐可选节次”获取相邻空闲节次或其他实验室建议。</li>
        <li>在“我的预约”中跟进审批状态与节次明细。</li>
      </ol>
    </BasePanel>

    <BasePanel tag="提醒" title="教师工作提示">
      <div class="alert-list">
        <div class="alert-item">
          <span class="alert-tag">课表模式</span>
          <h4>预约基于节次课表</h4>
          <p>系统以“实验室 + 日期 + 节次”为冲突判断标准，避免任意时间段带来的边界问题。</p>
        </div>
        <div class="alert-item">
          <span class="alert-tag">守时与规范</span>
          <h4>按时到场并规范使用</h4>
          <p>迟到、爽约或违规使用会影响信用分，也会影响后续预约与审批。</p>
        </div>
      </div>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchLabs } from '../api/labs';
import { fetchMyReservations } from '../api/reservations';
import BasePanel from '../components/BasePanel.vue';
import { useAuthStore } from '../stores/auth';
import type { ReservationDto } from '../types';

const auth = useAuthStore();
const reservations = ref<ReservationDto[]>([]);
const labCount = ref(0);

const reservationCount = computed(() => reservations.value.length);
const approvedCount = computed(() => reservations.value.filter((item) => item.status === 2).length);
const pendingCount = computed(() => reservations.value.filter((item) => item.status === 1).length);

onMounted(async () => {
  const [reservationData, labData] = await Promise.all([
    fetchMyReservations({ pageNum: 1, pageSize: 20 }, auth.token.value),
    fetchLabs({ pageNum: 1, pageSize: 50 }, auth.token.value),
  ]);

  reservations.value = reservationData.list;
  labCount.value = labData.total;
});
</script>

