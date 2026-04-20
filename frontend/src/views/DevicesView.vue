<template>
  <BasePanel tag="设备台账" title="设备状态与可用数量">
    <BaseTable :headers="['设备名称', '编号', '实验室', '品牌', '总数', '可用', '状态']">
      <tr v-for="item in devices" :key="item.id">
        <td>{{ item.name }}</td>
        <td>{{ item.code }}</td>
        <td>{{ item.labName }}</td>
        <td>{{ item.brand }}</td>
        <td>{{ item.quantity }}</td>
        <td>{{ item.available }}</td>
        <td><span :class="getBadgeClass(item.status)">{{ item.status }}</span></td>
      </tr>
    </BaseTable>
  </BasePanel>
</template>

<script setup lang="ts">
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { devices } from '../data/mock';
import { getBadgeClass } from '../utils/format';
</script>

