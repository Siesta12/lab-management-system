<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="sidebar-card user-card">
        <div class="user-card-top">
          <h3>{{ auth.currentUser.value?.realName ?? '未登录' }}</h3>
          <span v-if="roleText" class="user-role-pill">{{ roleText }}</span>
        </div>

        <div class="user-meta">
          <div class="user-meta-row">
            <span class="user-meta-label">账号</span>
            <span class="user-meta-value">{{ auth.currentUser.value?.username ?? '--' }}</span>
          </div>
          <div v-if="auth.currentUser.value?.departmentId != null" class="user-meta-row">
            <span class="user-meta-label">学院ID</span>
            <span class="user-meta-value">{{ auth.currentUser.value?.departmentId }}</span>
          </div>

          <div v-if="showRoleHint" class="user-tip">
            {{ roleHint }}
          </div>
        </div>

        <button class="danger-btn user-logout-btn" type="button" @click="handleLogout">退出登录</button>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          exact-active-class="active"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.desc }}</small>
        </RouterLink>
      </nav>
    </aside>

    <main class="main-panel">
      <section class="hero-panel">
        <div>
          <span class="eyebrow">Campus Lab Console</span>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageDescription }}</p>
        </div>
        <div v-if="showHeroActions" class="hero-actions">
          <button class="ghost-btn" type="button">导出报表</button>
          <button class="primary-btn" type="button">新增记录</button>
        </div>
      </section>

      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { getNavigationItems, getPrimaryRole, getRoleLabels } from '../access';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const roleText = computed(() => getRoleLabels(auth.currentUser.value?.roleCodes).join(' / '));
const primaryRole = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));
const navItems = computed(() => getNavigationItems(auth.currentUser.value?.roleCodes));
const showHeroActions = computed(() => primaryRole.value === 'ADMIN');
const showRoleHint = computed(() => primaryRole.value !== 'STUDENT');

const pageTitle = computed(() => {
  if (route.name === 'labs') {
    return primaryRole.value === 'ADMIN' ? '实验室管理' : '实验室查询';
  }
  return String(route.meta.title ?? '高校实验室管理系统');
});

const pageDescription = computed(() => {
  if (route.name === 'labs') {
    return primaryRole.value === 'ADMIN'
      ? '筛选、维护并更新实验室基础信息与开放状态。'
      : '查询实验室状态、查看课表并提交预约申请。';
  }
  return String(route.meta.description ?? '');
});

const roleHint = computed(() => {
  if (primaryRole.value === 'ADMIN') {
    return '当前是学院管理员视角，只能管理本学院的数据';
  }

  if (primaryRole.value === 'TEACHER') {
    return '当前是教师视角，支持预约审核、设备查询和统计查询';
  }

  return '';
});

function handleLogout(): void {
  auth.logout();
  router.push('/login');
}
</script>
