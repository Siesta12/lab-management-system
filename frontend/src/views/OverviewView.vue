<template>
  <section class="card-grid metrics-grid">
    <article v-for="card in visibleCards" :key="card.label" class="metric-card" :class="card.tone">
      <span>{{ card.label }}</span>
      <strong>{{ card.value }}</strong>
      <small>{{ card.trend }}</small>
    </article>
  </section>

  <section class="content-grid two-columns">
    <BasePanel :tag="trendTag" :title="trendTitle" note="单位：次" panel-class="chart-panel">
      <div class="sparkline" v-html="bars"></div>
      <div class="sparkline-axis">
        <span>周一</span>
        <span>周二</span>
        <span>周三</span>
        <span>周四</span>
        <span>周五</span>
        <span>周六</span>
        <span>周日</span>
      </div>
    </BasePanel>

    <BasePanel :tag="alertTag" :title="alertTitle">
      <div class="alert-list">
        <div v-for="item in visibleAlerts" :key="item.title" class="alert-item">
          <span class="alert-tag">{{ item.tag }}</span>
          <h4>{{ item.title }}</h4>
          <p>{{ item.detail }}</p>
        </div>
      </div>
    </BasePanel>
  </section>

  <section class="content-grid two-columns">
    <BasePanel :tag="heatmapTag" :title="heatmapTitle">
      <div class="heat-list">
        <div v-for="item in dashboardData.heatmap" :key="item.label" class="heat-row">
          <span>{{ item.label }}</span>
          <div class="heat-bar"><i :style="{ width: `${item.value}%` }"></i></div>
          <strong>{{ formatPercent(item.value) }}</strong>
        </div>
      </div>
    </BasePanel>

    <BasePanel :tag="focusTag" :title="focusTitle" panel-class="emphasis-panel">
      <ul class="bullet-list">
        <li v-for="item in focusItems" :key="item">{{ item }}</li>
      </ul>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { getPrimaryRole } from '../access';
import BasePanel from '../components/BasePanel.vue';
import { dashboardData } from '../data/mock';
import { useAuthStore } from '../stores/auth';
import { chartBars, formatPercent } from '../utils/format';

const auth = useAuthStore();
const primaryRole = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));
const bars = computed(() => chartBars(dashboardData.reservationTrend));

const visibleCards = computed(() => {
  if (primaryRole.value === 'STUDENT') {
    return [
      dashboardData.cards[0],
      dashboardData.cards[2],
      { label: '我的预约', value: '3', trend: '其中 1 条待审核', tone: 'accent' as const },
      { label: '信誉状态', value: '良好', trend: '近 30 天无违规记录', tone: 'success' as const },
    ];
  }

  if (primaryRole.value === 'TEACHER') {
    return [
      dashboardData.cards[0],
      dashboardData.cards[1],
      { label: '我的教学预约', value: '6', trend: '本周待使用 2 条', tone: 'brand' as const },
      { label: '设备协同', value: '8', trend: '可联动教学设备 8 台', tone: 'warning' as const },
    ];
  }

  return dashboardData.cards;
});

const visibleAlerts = computed(() => {
  if (primaryRole.value === 'STUDENT') {
    return [
      { title: '请按时签到', detail: '已通过预约需在开始前 15 分钟内签到，超时可能影响信誉分。', tag: '学生提醒' },
      { title: '本周开放规则更新', detail: '周末开放时段已延长，可在规则页查看新的固定时间段。', tag: '规则提醒' },
    ];
  }

  if (primaryRole.value === 'TEACHER') {
    return [
      { title: '教学预约优先审核', detail: '本周教学实验预约较多，建议尽早提交并填写课程信息。', tag: '教师提醒' },
      { title: '设备维护冲突提示', detail: 'A102 投影设备今日维护，排课前请先确认设备状态。', tag: '设备提示' },
    ];
  }

  return dashboardData.alerts;
});

const trendTag = computed(() => (primaryRole.value === 'STUDENT' ? '我的预约' : primaryRole.value === 'TEACHER' ? '教学预约' : '预约趋势'));
const trendTitle = computed(() => (primaryRole.value === 'STUDENT' ? '近七天我的预约节奏' : primaryRole.value === 'TEACHER' ? '近七天教学与科研预约波动' : '近七天预约波动'));
const alertTag = computed(() => (primaryRole.value === 'ADMIN' ? '运行提醒' : '我的提醒'));
const alertTitle = computed(() => (primaryRole.value === 'STUDENT' ? '学生使用提醒' : primaryRole.value === 'TEACHER' ? '教师工作提醒' : '今日重点事项'));
const heatmapTag = computed(() => (primaryRole.value === 'STUDENT' ? '推荐时段' : '热门时段'));
const heatmapTitle = computed(() => (primaryRole.value === 'STUDENT' ? '当前更容易预约的时间段' : '实验室使用热力'));
const focusTag = computed(() => (primaryRole.value === 'ADMIN' ? '毕业设计亮点' : '使用建议'));
const focusTitle = computed(() => {
  if (primaryRole.value === 'STUDENT') return '学生进入系统后最常用的功能';
  if (primaryRole.value === 'TEACHER') return '教师进入系统后建议优先处理的事项';
  return '当前首页已预留的答辩展示点';
});

const focusItems = computed(() => {
  if (primaryRole.value === 'STUDENT') {
    return [
      '先查看实验室开放规则，再提交预约申请',
      '遇到时间冲突时直接使用推荐时间段',
      '按时签到和结束签退，避免影响信誉分',
      '优先在“我的预约”里追踪审核状态',
    ];
  }

  if (primaryRole.value === 'TEACHER') {
    return [
      '优先提交教学预约，系统会按业务优先级参与审核',
      '预约前先确认实验室设备与开放规则',
      '重点关注课程项目名称和参与人数填写完整性',
      '通过统计页快速查看高峰时段，避开拥堵时间',
    ];
  }

  return [
    '预约冲突预警和推荐策略',
    '实验室运行状态一屏总览',
    '热门时间段与使用率可视化',
    '耗材预警与设备维护提醒',
  ];
});
</script>
