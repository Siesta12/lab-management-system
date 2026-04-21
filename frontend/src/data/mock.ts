import type {
  ConsumableItem,
  DashboardData,
  DeviceItem,
  LabItem,
  LabRecommendation,
  ReservationItem,
  RuleItem,
  StatisticItem,
  TimeRecommendation,
  UserItem,
} from '../types';

export const dashboardData: DashboardData = {
  cards: [
    { label: '今日预约', value: '26', trend: '+8% 较昨日', tone: 'brand' },
    { label: '开放实验室', value: '18', trend: '3 间维护中', tone: 'success' },
    { label: '待审核申请', value: '7', trend: '教学预约优先', tone: 'warning' },
    { label: '低库存耗材', value: '5', trend: '需尽快补货', tone: 'accent' },
  ],
  alerts: [
    {
      title: '今日有 3 条冲突待处理',
      detail: '数据智能实验室与电机拖动实验室各有 1 条优先级冲突申请，建议优先审核课程实验。',
      tag: '冲突提醒',
    },
    {
      title: '2 间实验室进入维护窗口',
      detail: '嵌入式实验室 3 与网络工程实验室 1 今日有节次维护，请关注后续开放状态。',
      tag: '实验室状态',
    },
    {
      title: '低库存耗材需要补货',
      detail: '散热风扇与固态硬盘库存接近安全线，建议安排采购或领用确认。',
      tag: '耗材库存',
    },
  ],
  reservationTrend: [12, 18, 16, 22, 20, 27, 26],
  heatmap: [
    { label: '08:00-10:00', value: 92, suffix: '%' },
    { label: '10:00-12:00', value: 84, suffix: '%' },
    { label: '14:00-16:00', value: 71, suffix: '%' },
    { label: '16:00-18:00', value: 56, suffix: '%' },
  ],
  todayOverview: {
    total: 26,
    morning: 9,
    afternoon: 11,
    evening: 6,
    pending: 7,
    conflict: 3,
    upcoming: 5,
  },
  todayTimeline: [
    {
      timeRange: '08:00 - 09:35',
      labName: '数据智能实验室1',
      applicant: '张老师',
      type: '教学预约',
      status: '进行中',
      note: '课程实验优先',
    },
    {
      timeRange: '10:00 - 11:35',
      labName: '电机拖动实验室2',
      applicant: '李同学',
      type: '个人预约',
      status: '待审核',
      note: '与维护窗口相邻',
    },
    {
      timeRange: '14:00 - 15:35',
      labName: '嵌入式实验室3',
      applicant: '科研团队 Alpha',
      type: '科研预约',
      status: '即将开始',
      note: '已确认设备就绪',
    },
    {
      timeRange: '16:00 - 17:35',
      labName: '数据智能实验室2',
      applicant: '王同学',
      type: '个人预约',
      status: '冲突待处理',
      note: '已生成推荐方案',
    },
  ],
  pendingReservations: [
    {
      labName: '数据智能实验室1',
      applicant: '张老师',
      timeRange: '08:00 - 09:35',
      type: '教学预约',
      status: '优先待审核',
    },
    {
      labName: '电机拖动实验室3',
      applicant: '李同学',
      timeRange: '10:00 - 11:35',
      type: '个人预约',
      status: '待审核',
    },
    {
      labName: '嵌入式实验室2',
      applicant: '科研团队 Beta',
      timeRange: '14:00 - 15:35',
      type: '科研预约',
      status: '优先待审核',
    },
    {
      labName: '数据智能实验室3',
      applicant: '王同学',
      timeRange: '16:00 - 17:35',
      type: '个人预约',
      status: '待审核',
    },
  ],
};

export const users: UserItem[] = [
  { id: 1, username: 'admin', realName: '系统管理员', role: '管理员', department: '实验中心', status: '正常', creditScore: 100 },
  { id: 2, username: 'teacher_zhang', realName: '张老师', role: '教师', department: '计算机学院', status: '正常', creditScore: 98 },
  { id: 3, username: 'student_li', realName: '李同学', role: '学生', department: '软件工程', status: '正常', creditScore: 86 },
  { id: 4, username: 'student_wang', realName: '王同学', role: '学生', department: '网络工程', status: '禁用', creditScore: 62 },
];

export const labs: LabItem[] = [
  {
    id: 1,
    name: '计算机实验室 A',
    code: 'LAB-A101',
    location: '实验楼 A / 101',
    status: '开放',
    capacity: 48,
    manager: '张老师',
    nextAvailable: '今天 14:00',
  },
  {
    id: 2,
    name: '人工智能实验室',
    code: 'LAB-A203',
    location: '实验楼 A / 203',
    status: '维护',
    capacity: 36,
    manager: '周老师',
    nextAvailable: '维护后开放',
  },
  {
    id: 3,
    name: '嵌入式实验室',
    code: 'LAB-B305',
    location: '实验楼 B / 305',
    status: '开放',
    capacity: 32,
    manager: '陈老师',
    nextAvailable: '今天 10:00',
  },
];

export const reservations: ReservationItem[] = [
  {
    id: 1,
    applicant: '张老师',
    labName: '计算机实验室 A',
    date: '2026-04-20',
    timeRange: '08:00 - 10:00',
    type: '教学预约',
    priority: '高',
    status: '待审核',
  },
  {
    id: 2,
    applicant: '科研团队 Alpha',
    labName: '人工智能实验室',
    date: '2026-04-20',
    timeRange: '14:00 - 18:00',
    type: '科研预约',
    priority: '中',
    status: '已通过',
  },
  {
    id: 3,
    applicant: '李同学',
    labName: '嵌入式实验室',
    date: '2026-04-21',
    timeRange: '16:00 - 18:00',
    type: '个人预约',
    priority: '低',
    status: '已驳回',
  },
];

export const rules: RuleItem[] = [
  { id: 1, labName: '计算机实验室 A', weekday: '周一', timeRange: '08:00 - 12:00', audience: '教师 / 学生', maxHours: '4 小时', status: '启用' },
  { id: 2, labName: '计算机实验室 A', weekday: '周三', timeRange: '14:00 - 18:00', audience: '教师 / 学生', maxHours: '4 小时', status: '启用' },
  { id: 3, labName: '人工智能实验室', weekday: '周二', timeRange: '08:00 - 10:00', audience: '教师', maxHours: '2 小时', status: '停用' },
];

export const devices: DeviceItem[] = [
  { id: 1, name: '台式工作站', code: 'DEV-001', labName: '计算机实验室 A', brand: 'Lenovo', quantity: 48, available: 46, status: '正常' },
  { id: 2, name: '深度学习服务器', code: 'DEV-014', labName: '人工智能实验室', brand: 'Inspur', quantity: 4, available: 3, status: '维修中' },
  { id: 3, name: '开发板套件', code: 'DEV-031', labName: '嵌入式实验室', brand: 'STM32', quantity: 32, available: 28, status: '正常' },
];

export const consumables: ConsumableItem[] = [
  { id: 1, name: '打印纸', code: 'CON-001', labName: '计算机实验室 A', stock: 86, warningLine: 30, unit: '包', status: '充足' },
  { id: 2, name: '传感器模块', code: 'CON-014', labName: '嵌入式实验室', stock: 18, warningLine: 20, unit: '个', status: '预警' },
  { id: 3, name: '酒精棉片', code: 'CON-023', labName: '人工智能实验室', stock: 12, warningLine: 20, unit: '盒', status: '预警' },
];

export const usageStatistics: StatisticItem[] = [
  { label: '实验室平均使用率', value: 78, suffix: '%' },
  { label: '本周预约总次数', value: 126 },
  { label: '审核通过率', value: 91, suffix: '%' },
  { label: '违规记录数', value: 6 },
];

export const timeRecommendations: TimeRecommendation[] = [
  { range: '2026-04-20 10:00 - 12:00', note: '与当前课程安排衔接最好' },
  { range: '2026-04-20 14:00 - 16:00', note: '冲突最少，设备空闲度更高' },
  { range: '2026-04-21 08:00 - 10:00', note: '次日同类实验预约较少' },
];

export const labRecommendations: LabRecommendation[] = [
  { name: '嵌入式实验室', location: '实验楼 B / 305', reason: '同时间段可用，容量满足 32 人' },
  { name: '网络工程实验室', location: '实验楼 C / 201', reason: '设备配置接近当前需求，冲突率低' },
];

