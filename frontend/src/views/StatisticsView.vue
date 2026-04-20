<template>
  <section class="card-grid stat-grid">
    <article v-for="item in usageStatistics" :key="item.label" class="stat-card">
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}{{ item.suffix ?? '' }}</strong>
    </article>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="答辩展示" title="推荐演示顺序">
      <ol class="number-list">
        <li>从首页总览进入，先讲系统解决了什么问题。</li>
        <li>切到预约审核页，展示冲突检测和推荐时间段。</li>
        <li>切到开放规则页，说明预约为什么能被限制在固定时间段。</li>
        <li>最后用统计页展示使用率、热门时段和违规数据。</li>
      </ol>
    </BasePanel>

    <BasePanel tag="接口联调预留" title="前端已按后端模块拆分" panel-class="emphasis-panel">
      <ul class="bullet-list">
        <li><code>/users</code> 用于用户管理</li>
        <li><code>/labs</code> 用于实验室管理</li>
        <li><code>/lab-open-rules</code> 用于开放规则</li>
        <li><code>/reservations</code> 用于预约审核</li>
        <li><code>/statistics</code> 与 <code>/dashboard</code> 用于统计分析</li>
      </ul>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import BasePanel from '../components/BasePanel.vue';
import { usageStatistics } from '../data/mock';
</script>

