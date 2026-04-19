<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand-block">
        <span class="brand-pill">毕业设计</span>
        <h1>高校实验室管理系统</h1>
        <p>基于开题报告拆分的前端工作台，覆盖预约、审核、规则、设备与统计分析。</p>
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

      <div class="sidebar-card user-card">
        <h3>{{ auth.currentUser.value?.realName ?? '未登录' }}</h3>
        <p>{{ auth.currentUser.value?.username ?? '请先登录后使用系统' }}</p>
        <p v-if="roleText">角色：{{ roleText }}</p>
        <button class="ghost-btn wide" type="button" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <main class="main-panel">
      <section class="hero-panel">
        <div>
          <span class="eyebrow">Campus Lab Console</span>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageDescription }}</p>
        </div>
        <div class="hero-actions">
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
import { useAuthStore } from '../stores/auth';

const navItems = [
  { to: '/', label: '系统总览', desc: '首页看板与运行提醒' },
  { to: '/users', label: '用户管理', desc: '账号、角色、信誉分' },
  { to: '/labs', label: '实验室管理', desc: '状态、容量、位置' },
  { to: '/reservations', label: '预约审核', desc: '申请、审核、推荐' },
  { to: '/rules', label: '开放规则', desc: '固定时间段与开放范围' },
  { to: '/devices', label: '设备管理', desc: '设备台账与状态' },
  { to: '/consumables', label: '耗材管理', desc: '库存预警与补给' },
  { to: '/statistics', label: '统计分析', desc: '使用率与热门时段' },
];

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const pageTitle = computed(() => String(route.meta.title ?? '高校实验室管理系统'));
const pageDescription = computed(() => String(route.meta.description ?? ''));
const roleText = computed(() => auth.currentUser.value?.roleCodes.join(' / ') ?? '');

function handleLogout(): void {
  auth.logout();
  router.push('/login');
}
</script>
