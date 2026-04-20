<template>
  <div class="shell">
    <aside class="sidebar">
      <div v-if="showBrandBlock" class="brand-block">
        <span class="brand-pill">精品设计</span>
        <h1>高校实验室管理系统</h1>
        <p>根据不同角色显示不同菜单，让学生端、管理员端功能回到各自正使用的环境里。</p>
      </div>

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
          active-class="active"
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

const pageTitle = computed(() => String(route.meta.title ?? '高校实验室管理系统'));
const pageDescription = computed(() => String(route.meta.description ?? ''));
const roleText = computed(() => getRoleLabels(auth.currentUser.value?.roleCodes).join(' / '));
const primaryRole = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));
const navItems = computed(() => getNavigationItems(auth.currentUser.value?.roleCodes));
const showHeroActions = computed(() => primaryRole.value === 'ADMIN');
const showBrandBlock = computed(() => primaryRole.value !== 'STUDENT');
const showRoleHint = computed(() => primaryRole.value !== 'STUDENT');

const roleHint = computed(() => {
  if (primaryRole.value === 'ADMIN') {
    return '当前是管理员视角，可管理用户、实验室、设备和统计信息';
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

