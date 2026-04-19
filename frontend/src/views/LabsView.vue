<template>
  <section class="content-grid two-columns">
    <BasePanel tag="实验室列表" title="实验室基础信息" :note="`共 ${labsState.total} 条`">
      <div class="toolbar">
        <input v-model="keyword" placeholder="输入实验室名称筛选" />
        <button type="button" class="ghost-btn" @click="loadLabs">查询</button>
      </div>
      <p v-if="message" class="info-text">{{ message }}</p>
      <BaseTable :headers="['名称', '编号', '位置', '状态', '容量', '负责人']">
        <tr v-for="lab in displayLabs" :key="lab.id">
          <td>{{ lab.name }}</td>
          <td>{{ lab.code }}</td>
          <td>{{ lab.location }}</td>
          <td><span :class="getBadgeClass(lab.status)">{{ lab.status }}</span></td>
          <td>{{ lab.capacity }}</td>
          <td>{{ lab.manager }}</td>
        </tr>
      </BaseTable>
    </BasePanel>

    <BasePanel tag="可预约信息" title="下一可用时间" panel-class="card-stack">
      <div v-for="lab in displayLabs.slice(0, 4)" :key="lab.id" class="lab-card">
        <div>
          <h4>{{ lab.name }}</h4>
          <p>{{ lab.location }}</p>
        </div>
        <strong>{{ lab.nextAvailable }}</strong>
      </div>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchLabs } from '../api/labs';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { labs as fallbackLabs } from '../data/mock';
import { useAuthStore } from '../stores/auth';
import type { LabDto, LabItem } from '../types';
import { getBadgeClass } from '../utils/format';

const auth = useAuthStore();
const keyword = ref('');
const message = ref('');
const labsState = ref<{ list: LabItem[]; total: number }>({
  list: fallbackLabs,
  total: fallbackLabs.length,
});

const displayLabs = computed(() => labsState.value.list);

function mapLab(dto: LabDto): LabItem {
  return {
    id: dto.id,
    name: dto.labName,
    code: dto.labCode,
    location: [dto.buildingName, dto.roomNo].filter(Boolean).join(' / ') || '位置待补充',
    status: dto.labStatus === 2 ? '维护' : dto.openStatus === 0 ? '关闭' : '开放',
    capacity: dto.capacity,
    manager: dto.managerUserId ? `用户 #${dto.managerUserId}` : '待分配',
    nextAvailable: dto.openStatus === 0 ? '当前关闭' : '可根据开放规则预约',
  };
}

async function loadLabs(): Promise<void> {
  try {
    const data = await fetchLabs({ labName: keyword.value || undefined }, auth.token.value || undefined);
    labsState.value = {
      list: data.list.map(mapLab),
      total: data.total,
    };
    message.value = '已切换为后端实时数据。';
  } catch (error) {
    message.value = error instanceof Error ? `后端请求失败，当前显示演示数据：${error.message}` : '后端请求失败，当前显示演示数据。';
    labsState.value = {
      list: fallbackLabs.filter((item) => item.name.includes(keyword.value)),
      total: fallbackLabs.filter((item) => item.name.includes(keyword.value)).length,
    };
  }
}

onMounted(() => {
  void loadLabs();
});
</script>
