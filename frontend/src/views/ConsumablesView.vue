<template>
  <section class="content-grid two-columns">
    <BasePanel tag="耗材库存" title="库存与预警状态">
      <BaseTable :headers="['耗材名称', '编号', '实验室', '库存', '预警线', '单位', '状态']">
        <tr v-for="item in consumables" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ item.code }}</td>
          <td>{{ item.labName }}</td>
          <td>{{ item.stock }}</td>
          <td>{{ item.warningLine }}</td>
          <td>{{ item.unit }}</td>
          <td><span :class="getBadgeClass(item.status)">{{ item.status }}</span></td>
        </tr>
      </BaseTable>
    </BasePanel>

    <BasePanel tag="库存调整" title="录入耗材变更" panel-class="form-panel">
      <form class="stack-form">
        <label>
          <span>耗材</span>
          <select>
            <option>传感器模块</option>
            <option>酒精棉片</option>
          </select>
        </label>
        <label>
          <span>变更类型</span>
          <select>
            <option>入库</option>
            <option>出库</option>
            <option>调整</option>
          </select>
        </label>
        <label><span>变更后库存</span><input value="32" /></label>
        <label><span>备注</span><textarea rows="4">实验课补充采购入库</textarea></label>
        <button type="button" class="primary-btn wide">保存库存调整</button>
      </form>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { consumables } from '../data/mock';
import { getBadgeClass } from '../utils/format';
</script>
