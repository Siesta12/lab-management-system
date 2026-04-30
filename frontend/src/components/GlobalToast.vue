<template>
  <teleport to="body">
    <div v-if="toast.visible" class="toast" :class="toastClass" role="status" aria-live="polite">
      {{ toast.text }}
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useGlobalToast } from '../composables/useGlobalToast';

const { toast } = useGlobalToast();

const successTextPattern = /成功|已创建|已更新|已提交|已完成|已保存|已删除|设置成功|复制成功|导入完成/;
const toastClass = computed(() => ({
  success: toast.type === 'success' || successTextPattern.test(toast.text),
  error: toast.type === 'error' && !successTextPattern.test(toast.text),
}));
</script>

<style scoped>
.toast {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  padding: 14px 28px;
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.18);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 22px 42px rgba(15, 23, 42, 0.22);
  font-size: 16px;
  font-weight: 650;
  color: rgba(15, 23, 42, 0.94);
  max-width: min(640px, 92vw);
  text-align: center;
  backdrop-filter: blur(10px);
  animation: toast-pop 160ms ease-out;
}

.toast.success {
  border-color: rgba(16, 185, 129, 0.28);
  background: linear-gradient(180deg, #2fd18a 0%, #18b977 100%);
  color: #ffffff;
  box-shadow: 0 18px 34px rgba(16, 185, 129, 0.28);
}

.toast.error {
  border-color: rgba(239, 68, 68, 0.24);
  background: linear-gradient(180deg, #f87171 0%, #ef4444 100%);
  color: #ffffff;
  box-shadow: 0 18px 34px rgba(239, 68, 68, 0.24);
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
