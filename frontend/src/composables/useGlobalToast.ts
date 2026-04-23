import { reactive, readonly } from 'vue';

type ToastType = 'success' | 'error';

const state = reactive<{
  visible: boolean;
  type: ToastType;
  text: string;
  timer?: number;
}>({
  visible: false,
  type: 'success',
  text: '',
});

function showToast(type: ToastType, text: string, durationMs = 1800): void {
  if (state.timer) {
    window.clearTimeout(state.timer);
  }
  state.visible = true;
  state.type = type;
  state.text = text;
  state.timer = window.setTimeout(() => {
    state.visible = false;
    state.text = '';
    state.timer = undefined;
  }, durationMs);
}

function hideToast(): void {
  if (state.timer) {
    window.clearTimeout(state.timer);
    state.timer = undefined;
  }
  state.visible = false;
  state.text = '';
}

export function useGlobalToast() {
  return {
    toast: readonly(state),
    showToast,
    hideToast,
  };
}
