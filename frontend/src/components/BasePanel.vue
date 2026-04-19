<template>
  <article class="panel" :class="panelClass">
    <div class="panel-head" v-if="title || note || tag">
      <div>
        <span v-if="tag" class="panel-tag">{{ tag }}</span>
        <h3 v-if="title">{{ title }}</h3>
      </div>
      <span v-if="note" class="panel-note">{{ note }}</span>
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
