export type AppRole = 'ADMIN' | 'TEACHER' | 'STUDENT';

export interface NavigationItem {
  to: string;
  label: string;
  desc: string;
  roles: AppRole[];
}

const ROLE_ALIASES: Record<AppRole, string[]> = {
  ADMIN: ['ADMIN', 'ROLE_ADMIN'],
  TEACHER: ['TEACHER', 'ROLE_TEACHER'],
  STUDENT: ['STUDENT', 'ROLE_STUDENT'],
};

const adminNavigation: NavigationItem[] = [
  { to: '/', label: '系统总览', desc: '首页看板与运行提醒', roles: ['ADMIN'] },
  { to: '/daily-schedule', label: '每日课表总览', desc: '按日期查看所有实验室节次状态', roles: ['ADMIN'] },
  { to: '/users', label: '用户管理', desc: '账号、角色、状态与信用分', roles: ['ADMIN'] },
  { to: '/labs', label: '实验室管理', desc: '详情、课表预约与维护', roles: ['ADMIN'] },
  { to: '/reservations', label: '预约管理', desc: '分页查看与审批预约单', roles: ['ADMIN'] },
  { to: '/devices', label: '设备管理', desc: '设备台账与状态', roles: ['ADMIN'] },
  { to: '/consumables', label: '耗材管理', desc: '库存预警与补给', roles: ['ADMIN'] },
  { to: '/statistics', label: '统计分析', desc: '使用情况与趋势', roles: ['ADMIN'] },
  { to: '/profile', label: '个人中心', desc: '个人资料与密码', roles: ['ADMIN'] },
];

const teacherNavigation: NavigationItem[] = [
  { to: '/teacher-home', label: '首页', desc: '教学科研预约概览', roles: ['TEACHER'] },
  { to: '/labs', label: '实验室查询', desc: '课表式节次预约入口', roles: ['TEACHER'] },
  { to: '/my-reservations', label: '我的预约', desc: '预约单与节次明细', roles: ['TEACHER'] },
  { to: '/statistics', label: '统计分析', desc: '使用情况与趋势', roles: ['TEACHER'] },
  { to: '/profile', label: '个人中心', desc: '个人资料与密码', roles: ['TEACHER'] },
];

const studentNavigation: NavigationItem[] = [
  { to: '/home', label: '首页', desc: '预约提醒与快捷入口', roles: ['STUDENT'] },
  { to: '/labs', label: '实验室查询', desc: '课表式节次预约入口', roles: ['STUDENT'] },
  { to: '/my-reservations', label: '我的预约', desc: '预约单与节次明细', roles: ['STUDENT'] },
  { to: '/my-credit', label: '我的信用', desc: '信用分与违规记录', roles: ['STUDENT'] },
  { to: '/profile', label: '个人中心', desc: '个人资料与密码', roles: ['STUDENT'] },
];

export function normalizeRoleCodes(roleCodes: string[] | undefined): AppRole[] {
  if (!roleCodes?.length) {
    return [];
  }

  const upperCodes = roleCodes.map((item) => item.toUpperCase());
  return (Object.keys(ROLE_ALIASES) as AppRole[]).filter((role) =>
    ROLE_ALIASES[role].some((alias) => upperCodes.includes(alias)),
  );
}

export function getPrimaryRole(roleCodes: string[] | undefined): AppRole | null {
  const normalized = normalizeRoleCodes(roleCodes);
  return normalized[0] ?? null;
}

export function hasRouteAccess(roleCodes: string[] | undefined, roles: AppRole[] | undefined): boolean {
  if (!roles?.length) {
    return true;
  }

  const normalized = normalizeRoleCodes(roleCodes);
  return roles.some((role) => normalized.includes(role));
}

export function getRoleLabels(roleCodes: string[] | undefined): string[] {
  const normalized = normalizeRoleCodes(roleCodes);

  return normalized.map((role) => {
    if (role === 'ADMIN') return '管理员';
    if (role === 'TEACHER') return '教师';
    return '学生';
  });
}

export function getNavigationItems(roleCodes: string[] | undefined): NavigationItem[] {
  const primaryRole = getPrimaryRole(roleCodes);

  if (primaryRole === 'ADMIN') return adminNavigation;
  if (primaryRole === 'TEACHER') return teacherNavigation;
  if (primaryRole === 'STUDENT') return studentNavigation;

  return [];
}

export function getFirstAccessiblePath(roleCodes: string[] | undefined): string {
  const primaryRole = getPrimaryRole(roleCodes);

  if (primaryRole === 'ADMIN') return '/';
  if (primaryRole === 'TEACHER') return '/teacher-home';
  if (primaryRole === 'STUDENT') return '/home';

  return '/login';
}

