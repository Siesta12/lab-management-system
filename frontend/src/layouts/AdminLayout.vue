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
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, RouterView, useRouter } from 'vue-router';
import { getNavigationItems, getPrimaryRole, getRoleLabels } from '../access';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const roleText = computed(() => getRoleLabels(auth.currentUser.value?.roleCodes).join(' / '));
const primaryRole = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));
const navItems = computed(() => getNavigationItems(auth.currentUser.value?.roleCodes));
const showRoleHint = computed(() => primaryRole.value !== 'STUDENT');

const roleHint = computed(() => {
  if (primaryRole.value === 'ADMIN') {
    return '当前是学院管理员视角，只能管理本学院的数据';
  }

  if (primaryRole.value === 'TEACHER') {
    return '当前是教师视角，仅显示首页、实验室预约、我的预约和个人信息';
  }

  return '';
});

function handleLogout(): void {
  auth.logout();
  router.push('/login');
}
</script>
