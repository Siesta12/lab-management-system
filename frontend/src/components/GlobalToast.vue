<template>
  <teleport to="body">
    <div v-if="toast.visible" class="toast" :style="toastStyle" role="status" aria-live="polite">
      {{ toast.text }}
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useGlobalToast } from '../composables/useGlobalToast';

const { toast } = useGlobalToast();
const toastStyle = computed<Record<string, string>>(() => {
  if (toast.type === 'success') {
    return {
      padding: '14px 28px',
      borderRadius: '18px',
      background: 'linear-gradient(180deg, #2fd18a 0%, #18b977 100%)',
      borderColor: 'rgba(16, 185, 129, 0.28)',
      color: '#ffffff',
      boxShadow: '0 18px 34px rgba(16, 185, 129, 0.28)',
      fontSize: '16px',
      letterSpacing: '0.02em',
    };
  }

  return {
    padding: '14px 28px',
    borderRadius: '18px',
    background: 'linear-gradient(180deg, #f87171 0%, #ef4444 100%)',
    borderColor: 'rgba(239, 68, 68, 0.24)',
    color: '#ffffff',
    boxShadow: '0 18px 34px rgba(239, 68, 68, 0.24)',
    fontSize: '16px',
    letterSpacing: '0.02em',
  };
});
</script>

<style scoped>
.toast {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.18);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 22px 42px rgba(15, 23, 42, 0.22);
  font-size: 14px;
  font-weight: 650;
  color: rgba(15, 23, 42, 0.94);
  max-width: min(640px, 92vw);
  text-align: center;
  backdrop-filter: blur(10px);
  animation: toast-pop 160ms ease-out;
}

@keyframes toast-pop {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-8px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}
</style>
