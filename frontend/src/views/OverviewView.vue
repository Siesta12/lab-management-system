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
      { label: '我的预订', value: '3', trend: '其中 1 项待审核', tone: 'accent' as const },
      { label: '信用状态', value: '优秀', trend: '近30天无违纪记录', tone: 'success' as const },
    ];
  }

  if (primaryRole.value === 'TEACHER') {
    return [
      dashboardData.cards[0],
      dashboardData.cards[1],
      { label: '我的教学预约', value: '6', trend: '本学期已使用2项', tone: 'brand' as const },
      { label: '设备配套', value: '8', trend: '可连接实验室设备8台', tone: 'warning' as const },
    ];
  }

  return dashboardData.cards;
});

const visibleAlerts = computed(() => {
  if (primaryRole.value === 'STUDENT') {
    return [
      { title: '请按时签到', detail: '已通过预订需在开始前 15 分钟内签到，超时可能影响信用分。', tag: '学生提醒' },
      { title: '本学期开放时间更新', detail: '实验室开放时间段有所延长，可在预约页面查看新的固定时间段。', tag: '规则提醒' },
    ];
  }

  if (primaryRole.value === 'TEACHER') {
    return [
      { title: '教学预约优先审核', detail: '本学期实验室预约较多，建议提前申请并填写教学信息。', tag: '教师提醒' },
      { title: '设备维护期间提示', detail: 'A102 房间设备今日维护，请预约前先确认设备状态。', tag: '设备提示' },
    ];
  }

  return dashboardData.alerts;
});

const trendTag = computed(() => (primaryRole.value === 'STUDENT' ? '我的预订' : primaryRole.value === 'TEACHER' ? '教学预订' : '预订概览'));
const trendTitle = computed(() => (primaryRole.value === 'STUDENT' ? '最近7天我的预约情况' : primaryRole.value === 'TEACHER' ? '最近7天教师与管理员预约活动' : '最近7天预约活动'));
const alertTag = computed(() => (primaryRole.value === 'ADMIN' ? '运行提醒' : '我的提醒'));
const alertTitle = computed(() => (primaryRole.value === 'STUDENT' ? '学生使用提醒' : primaryRole.value === 'TEACHER' ? '教师工作提醒' : '今日重点事项'));
const heatmapTag = computed(() => (primaryRole.value === 'STUDENT' ? '预约时段' : '热力时段'));
const heatmapTitle = computed(() => (primaryRole.value === 'STUDENT' ? '当前最容易预约的时间段' : '实验室使用热力图'));
const focusTag = computed(() => (primaryRole.value === 'ADMIN' ? '关键设计要点' : '使用建议'));
const focusTitle = computed(() => {
  if (primaryRole.value === 'STUDENT') return '学生进入系统后最常用的功能';
  if (primaryRole.value === 'TEACHER') return '教师进入系统后建议优先处理的事项';
  return '当前首页已预设的快捷显示点';
});

const focusItems = computed(() => {
  if (primaryRole.value === 'STUDENT') {
    return [
      '查看实验室开放时间段，提前安排预约审核',
      '遇到时间冲突时直接使用推荐时间段',
      '按时到达与结束签到，保持良好的信用分',
      '优先在"我的预约"里查看这个审核状态',
    ];
  }

  if (primaryRole.value === 'TEACHER') {
    return [
      '优先提交教学预约，系统会按级别优先审核',
      '预约前先确认实验室设备与开放时间',
      '重点填写项目名称与参与人数等完整信息',
      '通过统计页面查看高峰时段，避免拥挤时间',
    ];
  }

  return [
    '预约期间学习与推荐相关',
    '实验室运行状态一览视图',
    '热力时间段与使用可视化',
    '耗材库存与设备维护提醒',
  ];
});
</script>

