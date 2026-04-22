<template>
  <section class="login-page">
    <div class="login-panel">
      <div class="login-hero">
        <div class="hero-top">
          <span class="brand-badge">实验室预约管理平台</span>
        </div>

        <div class="hero-main">
          <h1>高校实验室管理系统</h1>
          <p>统一管理实验室、设备与预约流程，提升使用效率与管理规范性。</p>
        </div>

        <div class="hero-info">
          <div class="info-item">
            <strong>实验室管理</strong>
            <span>支持实验室信息维护与状态管理</span>
          </div>
          <div class="info-item">
            <strong>在线预约</strong>
            <span>支持预约申请、审核与记录查询</span>
          </div>
          <div class="info-item">
            <strong>权限控制</strong>
            <span>基于角色区分管理员、教师与学生功能</span>
          </div>
        </div>
      </div>

      <div class="login-card">
        <div class="card-header">
          <h2>账号登录</h2>
          <p>请输入用户名和密码进入系统</p>
        </div>

        <form class="login-form" @submit.prevent="handleSubmit">
          <label class="form-item">
            <span>用户名</span>
            <input
              v-model.trim="form.username"
              placeholder="请输入用户名"
              autocomplete="username"
              required
            />
          </label>

          <label class="form-item">
            <span>密码</span>
            <input
              v-model.trim="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
            />
          </label>

          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
          <p v-if="statusMessage" class="status-text">{{ statusMessage }}</p>

          <button
              type="submit"
              class="login-btn"
              :disabled="submitting || !canSubmit"
          >
            {{ submitting ? '登录中...' : '登录' }}
          </button>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { getFirstAccessiblePath } from '../access';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const form = reactive({
  username: '',
  password: '',
});

const errorMessage = ref('');
const statusMessage = ref('');
const submitting = ref(false);
const canSubmit = computed(() => Boolean(form.username.trim()) && Boolean(form.password.trim()));

async function handleSubmit(): Promise<void> {
  errorMessage.value = '';
  statusMessage.value = '准备提交登录请求...';

  if (!form.username.trim()) {
    errorMessage.value = '请输入用户名';
    statusMessage.value = '';
    return;
  }

  if (!form.password.trim()) {
    errorMessage.value = '请输入密码';
    statusMessage.value = '';
    return;
  }

  try {
    submitting.value = true;
    statusMessage.value = '正在请求登录接口...';
    await auth.signIn(form);
    const roleCodes = auth.currentUser.value?.roleCodes ?? [];
    const targetPath = getFirstAccessiblePath(roleCodes);
    statusMessage.value = `登录成功，角色=${roleCodes.join(',') || '空'}，目标=${targetPath}`;
    await router.push(targetPath);
    statusMessage.value = '';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试';
    statusMessage.value = '';
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.status-text {
  margin: 0;
  color: #2563eb;
  font-size: 13px;
  line-height: 1.5;
}
</style>
