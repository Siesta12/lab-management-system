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
  { to: '/admin', label: '系统概览', desc: '首页面板与运行概况', roles: ['ADMIN'] },
  { to: '/admin/users', label: '用户管理', desc: '账号、角色、状态与信用分', roles: ['ADMIN'] },
  { to: '/admin/labs', label: '实验室管理', desc: '信息、设备预约与管理', roles: ['ADMIN'] },
  { to: '/admin/reservations', label: '预约管理', desc: '分页查看与审核单', roles: ['ADMIN'] },
  { to: '/admin/devices', label: '设备管理', desc: '设备库存与状态', roles: ['ADMIN'] },
  { to: '/admin/consumables', label: '耗材管理', desc: '库存管理与统计', roles: ['ADMIN'] },
  { to: '/admin/statistics', label: '统计分析', desc: '使用情况与统计', roles: ['ADMIN'] },
  { to: '/admin/profile', label: '个人资料', desc: '个人资料与密码', roles: ['ADMIN'] },
];

const teacherNavigation: NavigationItem[] = [
  { to: '/admin/teacher-home', label: '首页', desc: '今日预约与提示', roles: ['TEACHER'] },
  { to: '/admin/labs', label: '实验室预约', desc: '选择实验室并提交预约', roles: ['TEACHER'] },
  { to: '/admin/my-reservations', label: '我的预约', desc: '预约详情与记录', roles: ['TEACHER'] },
  { to: '/admin/profile', label: '个人信息', desc: '个人资料与密码', roles: ['TEACHER'] },
];

const studentNavigation: NavigationItem[] = [
  { to: '/admin/home', label: '首页', desc: '预约推荐与快速入口', roles: ['STUDENT'] },
  { to: '/admin/labs', label: '实验室查询', desc: '申请预约查询', roles: ['STUDENT'] },
  { to: '/admin/my-reservations', label: '我的预约', desc: '预约详情与记录', roles: ['STUDENT'] },
  { to: '/admin/my-credit', label: '我的信用', desc: '信用记录与违纪记录', roles: ['STUDENT'] },
  { to: '/admin/profile', label: '个人资料', desc: '个人资料与密码', roles: ['STUDENT'] },
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
    if (role === 'ADMIN') return '学院管理员';
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

  if (primaryRole === 'ADMIN') return '/admin';
  if (primaryRole === 'TEACHER') return '/admin/teacher-home';
  if (primaryRole === 'STUDENT') return '/admin/home';

  return '/login';
}
