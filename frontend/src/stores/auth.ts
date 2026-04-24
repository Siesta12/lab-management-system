import { computed, reactive } from 'vue';
import { fetchCurrentUser, login, type LoginPayload } from '../api/auth';
import type { AuthUser } from '../types';

const TOKEN_KEY = 'lab-system-token';

const state = reactive<{
  token: string;
  user: AuthUser | null;
  loading: boolean;
}>({
  token: localStorage.getItem(TOKEN_KEY) ?? '',
  user: null,
  loading: false,
});

async function signIn(payload: LoginPayload): Promise<void> {
  state.loading = true;
  try {
    const data = await login(payload);
    state.token = data.token;
    localStorage.setItem(TOKEN_KEY, data.token);
    state.user = {
      id: data.id,
      userNo: data.userNo,
      realName: data.realName,
      roleCodes: data.roleCodes,
      departmentId: data.departmentId,
    };
  } finally {
    state.loading = false;
  }
}

async function loadProfile(): Promise<void> {
  if (!state.token) {
    state.user = null;
    return;
  }

  state.loading = true;
  try {
    state.user = await fetchCurrentUser(state.token);
  } catch (error) {
    clearAuth();
    throw error;
  } finally {
    state.loading = false;
  }
}

function clearAuth(): void {
  state.token = '';
  state.user = null;
  localStorage.removeItem(TOKEN_KEY);
}

export function useAuthStore() {
  return {
    state,
    token: computed(() => state.token),
    currentUser: computed(() => state.user),
    isAuthenticated: computed(() => Boolean(state.token)),
    signIn,
    loadProfile,
    logout: clearAuth,
  };
}

