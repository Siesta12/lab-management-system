<template>
  <section class="login-shell">
    <div class="login-intro">
      <span class="brand-pill">SpringBoot + Vue3</span>
      <h1>高校实验室管理系统</h1>
      <p>先把登录、实验室、预约这条主链路接起来，后面我们再逐页替换成真实接口。</p>
      <ul class="bullet-list login-points">
        <li>JWT 登录鉴权</li>
        <li>实验室列表查询</li>
        <li>预约列表与冲突检测</li>
      </ul>
    </div>

    <div class="login-card">
      <h2>登录系统</h2>
      <p>默认请求后端 <code>/api/auth/login</code></p>
      <form class="stack-form" @submit.prevent="handleSubmit">
        <label>
          <span>用户名</span>
          <input v-model="form.username" placeholder="请输入用户名" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="form.password" type="password" placeholder="请输入密码" />
        </label>
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        <button type="submit" class="primary-btn wide" :disabled="auth.state.loading">
          {{ auth.state.loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const form = reactive({
  username: 'admin',
  password: '123456',
});

const errorMessage = ref('');

async function handleSubmit(): Promise<void> {
  errorMessage.value = '';
  try {
    await auth.signIn(form);
    await auth.loadProfile();
    await router.push('/');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试';
  }
}
</script>
