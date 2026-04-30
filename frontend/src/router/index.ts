import { createRouter, createWebHistory } from 'vue-router';
import { getFirstAccessiblePath, hasRouteAccess, type AppRole } from '../access';
import AdminLayout from '../layouts/AdminLayout.vue';
import ConsumablesView from '../views/ConsumablesView.vue';
import DevicesView from '../views/DevicesView.vue';
import LabsView from '../views/LabsView.vue';
import LoginView from '../views/LoginView.vue';
import MyCreditView from '../views/MyCreditView.vue';
import MyReservationsView from '../views/MyReservationsView.vue';
import OverviewView from '../views/OverviewView.vue';
import ProfileView from '../views/ProfileView.vue';
import ReservationsView from '../views/ReservationsView.vue';
import StatisticsView from '../views/StatisticsView.vue';
import StudentHomeView from '../views/StudentHomeView.vue';
import CheckinView from '../views/checkin/CheckinView.vue';
import TeacherHomeView from '../views/TeacherHomeView.vue';
import UsersView from '../views/UsersView.vue';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/admin',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        public: true,
        title: '登录',
      },
    },
    {
      path: '/checkin',
      name: 'checkin',
      component: CheckinView,
      meta: {
        public: true,
        title: '实验室签到',
      },
    },
    {
      path: '/admin',
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
            roles: ['ADMIN'] satisfies AppRole[],
          },
        },
        {
          path: 'teacher-home',
          name: 'teacher-home',
          component: TeacherHomeView,
          meta: {
            title: '教师首页',
            roles: ['TEACHER'] satisfies AppRole[],
          },
        },
        {
          path: 'home',
          name: 'student-home',
          component: StudentHomeView,
          meta: {
            title: '学生首页',
            roles: ['STUDENT'] satisfies AppRole[],
          },
        },
        {
          path: 'users',
          name: 'users',
          component: UsersView,
          meta: {
            title: '用户管理',
            roles: ['ADMIN'] satisfies AppRole[],
          },
        },
        {
          path: 'labs',
          name: 'labs',
          component: LabsView,
          meta: {
            title: '实验室查询',
            roles: ['ADMIN', 'TEACHER', 'STUDENT'] satisfies AppRole[],
          },
        },
        {
          path: 'my-reservations',
          name: 'my-reservations',
          component: MyReservationsView,
          meta: {
            title: '我的预约',
            roles: ['TEACHER', 'STUDENT'] satisfies AppRole[],
          },
        },
        {
          path: 'reservations',
          name: 'reservations',
          component: ReservationsView,
          meta: {
            title: '预约管理',
            roles: ['ADMIN'] satisfies AppRole[],
          },
        },
        {
          path: 'devices',
          name: 'devices',
          component: DevicesView,
          meta: {
            title: '设备管理',
            roles: ['ADMIN', 'TEACHER', 'STUDENT'] satisfies AppRole[],
          },
        },
        {
          path: 'consumables',
          name: 'consumables',
          component: ConsumablesView,
          meta: {
            title: '耗材管理',
            roles: ['ADMIN'] satisfies AppRole[],
          },
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: StatisticsView,
          meta: {
            title: '统计分析',
            roles: ['ADMIN'] satisfies AppRole[],
          },
        },
        {
          path: 'my-credit',
          name: 'my-credit',
          component: MyCreditView,
          meta: {
            title: '我的信用',
            roles: ['STUDENT'] satisfies AppRole[],
          },
        },
        {
          path: 'profile',
          name: 'profile',
          component: ProfileView,
          meta: {
            title: '个人中心',
            roles: ['ADMIN', 'TEACHER', 'STUDENT'] satisfies AppRole[],
          },
        },
      ],
    },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();

  async function ensureCurrentUser(): Promise<boolean> {
    if (!auth.isAuthenticated.value) {
      return false;
    }

    if (auth.currentUser.value || auth.state.loading) {
      return Boolean(auth.currentUser.value);
    }

    try {
      await auth.loadProfile();
      return Boolean(auth.currentUser.value);
    } catch {
      return false;
    }
  }

  if (to.meta.public) {
    if (to.path === '/login') {
      const hasUser = await ensureCurrentUser();
      if (hasUser) {
        const targetPath = getFirstAccessiblePath(auth.currentUser.value?.roleCodes);
        if (targetPath !== to.path) {
          return targetPath;
        }
      }
    }
    return true;
  }

  const hasUser = await ensureCurrentUser();
  if (!hasUser) {
    return '/login';
  }

  const currentUser = auth.currentUser.value;
  if (!currentUser) {
    return '/login';
  }

  const roles = to.meta.roles as AppRole[] | undefined;
  if (!hasRouteAccess(currentUser.roleCodes, roles)) {
    const targetPath = getFirstAccessiblePath(currentUser.roleCodes);
    if (targetPath !== to.path) {
      return targetPath;
    }
  }

  return true;
});

export default router;
