import { createRouter, createWebHistory } from 'vue-router';
import AdminLayout from '../layouts/AdminLayout.vue';
import ConsumablesView from '../views/ConsumablesView.vue';
import DevicesView from '../views/DevicesView.vue';
import LabsView from '../views/LabsView.vue';
import LoginView from '../views/LoginView.vue';
import OverviewView from '../views/OverviewView.vue';
import ReservationsView from '../views/ReservationsView.vue';
import RulesView from '../views/RulesView.vue';
import StatisticsView from '../views/StatisticsView.vue';
import UsersView from '../views/UsersView.vue';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        public: true,
        title: '登录',
        description: '请输入账号密码进入实验室管理系统。',
      },
    },
    {
      path: '/',
      component: AdminLayout,
      meta: {
        requiresAuth: true,
      },
      children: [
        {
          path: '',
          name: 'overview',
          component: OverviewView,
          meta: {
            title: '系统总览',
            description: '把首页看板做成答辩时一打开就能讲故事的页面：运行状态、冲突提醒、趋势图、热门时段都在一屏里。',
          },
        },
        {
          path: 'users',
          name: 'users',
          component: UsersView,
          meta: {
            title: '用户管理',
            description: '这里对应认证与用户管理模块，重点展示角色、状态、部门和信誉分。',
          },
        },
        {
          path: 'labs',
          name: 'labs',
          component: LabsView,
          meta: {
            title: '实验室管理',
            description: '实验室模块突出基础信息、开放状态、容量与下一可用时间，方便和预约模块联动。',
          },
        },
        {
          path: 'reservations',
          name: 'reservations',
          component: ReservationsView,
          meta: {
            title: '预约审核',
            description: '预约模块是系统主线，页面里直接体现冲突检查、推荐时间段、推荐实验室和审核优先级。',
          },
        },
        {
          path: 'rules',
          name: 'rules',
          component: RulesView,
          meta: {
            title: '开放规则',
            description: '固定时间段和开放规则是预约判断的基础，前端先把规则列表和配置卡片做好。',
          },
        },
        {
          path: 'devices',
          name: 'devices',
          component: DevicesView,
          meta: {
            title: '设备管理',
            description: '设备管理页展示设备台账、可用数量、维修状态，适合后面接搜索和状态修改接口。',
          },
        },
        {
          path: 'consumables',
          name: 'consumables',
          component: ConsumablesView,
          meta: {
            title: '耗材管理',
            description: '耗材模块突出库存预警和库存调整入口，和你后端的库存流水表设计是能对上的。',
          },
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: StatisticsView,
          meta: {
            title: '统计分析',
            description: '统计模块服务答辩展示，重点看使用率、预约次数、热门时段和违规情况。',
          },
        },
      ],
    },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();

  if (to.meta.public) {
    if (auth.isAuthenticated.value && to.path === '/login') {
      return '/';
    }
    return true;
  }

  if (!auth.isAuthenticated.value) {
    return '/login';
  }

  if (!auth.currentUser.value && !auth.state.loading) {
    try {
      await auth.loadProfile();
    } catch {
      return '/login';
    }
  }

  return true;
});

export default router;
