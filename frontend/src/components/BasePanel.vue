<template>
  <article class="panel" :class="panelClass">
    <div class="panel-head" v-if="title">
      <div>
        <h3>{{ title }}</h3>
      </div>
    </div>
    <slot />
  </article>
</template>

<script setup lang="ts">
defineProps<{
  tag?: string;
  title?: string;
  note?: string;
  panelClass?: string;
}>();
</script>
