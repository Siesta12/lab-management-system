<template>
  <div class="conflict-group-list">
    <div v-if="loading" class="state-block">正在加载冲突预约...</div>
    <div v-else-if="!groups.length" class="state-block">{{ emptyText }}</div>
    <ConflictGroupCard
      v-for="group in groups"
      v-else
      :key="group.key"
      :group="group"
      :status-filter="statusFilter"
      @view="$emit('view', $event)"
      @review="$emit('review', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import ConflictGroupCard from './ConflictGroupCard.vue';
import type { ConflictGroupViewModel } from '../../utils/adminReservations';

defineProps<{
  groups: ConflictGroupViewModel[];
  loading: boolean;
  emptyText: string;
  statusFilter: string;
}>();

defineEmits<{
  (event: 'view', id: number): void;
  (event: 'review', id: number): void;
}>();
</script>

<style scoped>
.conflict-group-list {
  display: grid;
  gap: 14px;
}

.state-block {
  padding: 40px 18px;
  text-align: center;
  color: #64748b;
  border: 1px dashed rgba(148, 163, 184, 0.3);
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.74);
}
</style>
