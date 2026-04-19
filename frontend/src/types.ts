export type NavKey =
  | 'overview'
  | 'users'
  | 'labs'
  | 'reservations'
  | 'rules'
  | 'devices'
  | 'consumables'
  | 'statistics';

export interface SummaryCard {
  label: string;
  value: string;
  trend: string;
  tone: 'brand' | 'accent' | 'success' | 'warning';
}

export interface ReservationAlert {
  title: string;
  detail: string;
  tag: string;
}

export interface UserItem {
  id: number;
  username: string;
  realName: string;
  role: string;
  department: string;
  status: '正常' | '禁用';
  creditScore: number;
}

export interface LabItem {
  id: number;
  name: string;
  code: string;
  location: string;
  status: '开放' | '维护' | '关闭';
  capacity: number;
  manager: string;
  nextAvailable: string;
}

export interface ReservationItem {
  id: number;
  applicant: string;
  labName: string;
  date: string;
  timeRange: string;
  type: '教学预约' | '科研预约' | '个人预约';
  priority: '高' | '中' | '低';
  status: '待审核' | '已通过' | '已驳回' | '已取消';
}

export interface RuleItem {
  id: number;
  labName: string;
  weekday: string;
  timeRange: string;
  audience: string;
  maxHours: string;
  status: '启用' | '停用';
}

export interface DeviceItem {
  id: number;
  name: string;
  code: string;
  labName: string;
  brand: string;
  quantity: number;
  available: number;
  status: '正常' | '维修中' | '停用';
}

export interface ConsumableItem {
  id: number;
  name: string;
  code: string;
  labName: string;
  stock: number;
  warningLine: number;
  unit: string;
  status: '充足' | '预警';
}

export interface StatisticItem {
  label: string;
  value: number;
  suffix?: string;
}

export interface TimeRecommendation {
  range: string;
  note: string;
}

export interface LabRecommendation {
  name: string;
  location: string;
  reason: string;
}

export interface DashboardData {
  cards: SummaryCard[];
  alerts: ReservationAlert[];
  reservationTrend: number[];
  heatmap: StatisticItem[];
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageData<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface AuthUser {
  id: number;
  username: string;
  realName: string;
  roleCodes: string[];
}

export interface LoginResponseData extends AuthUser {
  token: string;
}

export interface LabDto {
  id: number;
  departmentId?: number;
  labCode: string;
  labName: string;
  labType?: string;
  buildingName?: string;
  roomNo?: string;
  capacity: number;
  managerUserId?: number;
  openStatus: number;
  labStatus: number;
  description?: string;
  usageRule?: string;
}

export interface ReservationDto {
  id: number;
  reservationNo: string;
  labId: number;
  applicantUserId: number;
  approverUserId?: number;
  reservationType: number;
  priorityLevel: number;
  reservationDate: string;
  startTime: string;
  endTime: string;
  usagePurpose: string;
  courseOrProjectName?: string;
  participantCount: number;
  contactPhone?: string;
  status: number;
  rejectReason?: string;
}

export interface ConflictCheckData {
  conflict: boolean;
  conflictCount: number;
  message: string;
}

export interface TimeRecommendationDto {
  startTime: string;
  endTime: string;
  reason: string;
}

export interface LabRecommendationDto {
  labId: number;
  labCode: string;
  labName: string;
  buildingName?: string;
  roomNo?: string;
  startTime: string;
  endTime: string;
  reason: string;
}

export interface ReservationCreatePayload {
  labId: number;
  reservationType: number;
  reservationDate: string;
  startTime: string;
  endTime: string;
  usagePurpose: string;
  courseOrProjectName?: string;
  participantCount: number;
  contactPhone: string;
}
