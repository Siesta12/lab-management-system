<template>
  <section class="content-grid two-columns">
    <BasePanel tag="开放规则" title="固定时间段配置">
      <BaseTable :headers="['实验室', '星期', '时间段', '开放对象', '最长时长', '状态']">
        <tr v-for="item in rules" :key="item.id">
          <td>{{ item.labName }}</td>
          <td>{{ item.weekday }}</td>
          <td>{{ item.timeRange }}</td>
          <td>{{ item.audience }}</td>
          <td>{{ item.maxHours }}</td>
          <td><span :class="getBadgeClass(item.status)">{{ item.status }}</span></td>
        </tr>
      </BaseTable>
    </BasePanel>

    <BasePanel tag="规则编辑" title="新增开放规则" panel-class="form-panel">
      <form class="stack-form">
        <label>
          <span>实验室</span>
          <select>
            <option>计算机实验室 A</option>
            <option>人工智能实验室</option>
          </select>
        </label>
        <label>
          <span>星期</span>
          <select>
            <option>周一</option>
            <option>周二</option>
            <option>周三</option>
          </select>
        </label>
        <label><span>开始时间</span><input type="time" value="08:00" /></label>
        <label><span>结束时间</span><input type="time" value="10:00" /></label>
        <label><span>开放对象</span><input value="教师 / 学生" /></label>
        <button type="button" class="primary-btn wide">保存规则</button>
      </form>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { rules } from '../data/mock';
import { getBadgeClass } from '../utils/format';
</script>
