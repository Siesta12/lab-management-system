<template>
  <section class="card-grid metrics-grid">
    <article v-for="card in dashboardData.cards" :key="card.label" class="metric-card" :class="card.tone">
      <span>{{ card.label }}</span>
      <strong>{{ card.value }}</strong>
      <small>{{ card.trend }}</small>
    </article>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="预约趋势" title="近七天预约波动" note="单位：次" panel-class="chart-panel">
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

    <BasePanel tag="运行提醒" title="今日重点事项">
      <div class="alert-list">
        <div v-for="item in dashboardData.alerts" :key="item.title" class="alert-item">
          <span class="alert-tag">{{ item.tag }}</span>
          <h4>{{ item.title }}</h4>
          <p>{{ item.detail }}</p>
        </div>
      </div>
    </BasePanel>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="热门时段" title="实验室使用热力">
      <div class="heat-list">
        <div v-for="item in dashboardData.heatmap" :key="item.label" class="heat-row">
          <span>{{ item.label }}</span>
          <div class="heat-bar"><i :style="{ width: `${item.value}%` }"></i></div>
          <strong>{{ formatPercent(item.value) }}</strong>
        </div>
      </div>
    </BasePanel>

    <BasePanel tag="毕业设计亮点" title="当前首页已预留的答辩展示点" panel-class="emphasis-panel">
      <ul class="bullet-list">
        <li>预约冲突预警和推荐策略</li>
        <li>实验室运行状态一屏总览</li>
        <li>热门时间段与使用率可视化</li>
        <li>耗材预警与设备维护提醒</li>
      </ul>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import BasePanel from '../components/BasePanel.vue';
import { dashboardData } from '../data/mock';
import { chartBars, formatPercent } from '../utils/format';

const bars = computed(() => chartBars(dashboardData.reservationTrend));
</script>
