export type NavKey =
  | 'overview'
  | 'users'
  | 'labs'
  | 'my-reservations'
  | 'devices'
  | 'consumables'
  | 'statistics'
  | 'my-credit'
  | 'profile'
  | 'home';

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
  status: '开放' | '关闭' | '维护';
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
  status: '待审核' | '已通过' | '已驳回' | '已取消' | '已完成';
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
  status: '正常' | '维修中' | '已使用';
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

export interface DashboardCardDto {
  label: string;
  value: string;
  trend: string;
  tone: 'brand' | 'accent' | 'success' | 'warning';
}

export interface DashboardAlertDto {
  title: string;
  detail: string;
  tag: string;
}

export interface DashboardSectionItemDto {
  primary: string;
  secondary: string;
  meta: string;
  statusLabel: string;
  tone: string;
}

export interface DashboardSectionDto {
  title: string;
  tag: string;
  items: DashboardSectionItemDto[];
}

export interface TodayReservationSummaryDto {
  total: number;
  morning: number;
  afternoon: number;
  evening: number;
  pending: number;
  conflict: number;
  upcoming: number;
}

export interface DashboardReservationItemDto {
  reservationId: number;
  reservationNo: string;
  labId: number;
  labName: string;
  applicantUserId: number;
  applicantName: string;
  applicantCreditScore: number;
  reservationType: number;
  priorityLevel: number;
  status: number;
  reservationDate: string;
  periodId: number;
  periodName: string;
  startTime: string;
  endTime: string;
  createdAt: string;
  timeRange?: string;
  typeLabel?: string;
  statusLabel?: string;
  note?: string;
}

export interface DashboardHeatmapDto {
  name: string;
  count: number;
  rate?: number | null;
}

export interface DashboardLabOccupancyDto {
  labType: string;
  occupiedSlots: number;
  totalOpenSlots: number;
  occupancyRate: number;
  labCount: number;
}

export interface AdminDashboardDto {
  cards: DashboardCardDto[];
  todayOverview: TodayReservationSummaryDto;
  todayTimeline: DashboardReservationItemDto[];
  pendingSections: DashboardSectionDto[];
  occupancyRates: DashboardLabOccupancyDto[];
  pendingReservations: DashboardReservationItemDto[];
}

export interface TodayReservationSummary {
  total: number;
  morning: number;
  afternoon: number;
  evening: number;
  pending: number;
  conflict: number;
  upcoming: number;
}

export interface TodayReservationItem {
  timeRange: string;
  labName: string;
  applicant: string;
  type: '教学预约' | '科研预约' | '个人预约' | '维护';
  status: '待审核' | '已通过' | '冲突待处理' | '即将开始' | '进行中';
  note?: string;
}

export interface PendingReservationItem {
  labName: string;
  applicant: string;
  timeRange: string;
  type: '教学预约' | '科研预约' | '个人预约';
  status: '待审核' | '优先待审核';
}

export interface DashboardData {
  cards: SummaryCard[];
  alerts: ReservationAlert[];
  reservationTrend: number[];
  heatmap: StatisticItem[];
  todayOverview: TodayReservationSummary;
  todayTimeline: TodayReservationItem[];
  pendingReservations: PendingReservationItem[];
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

export interface OptionItem {
  label: string;
  value: number;
}

export interface AuthUser {
  id: number;
  username: string;
  realName: string;
  roleCodes: string[];
  departmentId?: number | null;
}

export interface LoginResponseData extends AuthUser {
  token: string;
}

export interface DepartmentDto {
  id: number;
  departmentName: string;
  departmentCode: string;
  leaderName?: string;
  phone?: string;
  status: number;
}

export interface UserVO {
  id: number;
  departmentId?: number;
  username: string;
  realName: string;
  userNo: string;
  gender?: number;
  phone?: string;
  email?: string;
  creditScore: number;
  violationCount: number;
  status: number;
  roleIds: number[];
}

export interface UserProfileUpdatePayload {
  phone?: string;
  email?: string;
}

export interface PasswordUpdatePayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
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

export interface LabOpenRuleDto {
  id: number;
  labId: number;
  weekday: number;
  startTime: string;
  endTime: string;
  allowStudent: number;
  allowTeacher: number;
  maxReservationHours: number;
  status: number;
}

export interface ClassPeriodDto {
  id: number;
  periodNo: number;
  periodName: string;
  startTime: string;
  endTime: string;
}

export type ScheduleStatus =
  | 'FREE'
  | 'RESERVED'
  | 'PENDING'
  | 'PENDING_SELF'
  | 'PENDING_OTHERS'
  | 'MAINTENANCE'
  | 'CLOSED';

export interface ScheduleCellDto {
  periodId: number;
  status: ScheduleStatus;
  reservationId?: number;
  reservationNo?: string;
  reservationStatus?: number;
  maintenanceId?: number;
  maintenanceReason?: string;
  note?: string;
}

export interface ScheduleDayDto {
  date: string;
  weekday: number;
  cells: ScheduleCellDto[];
}

export interface LabScheduleDto {
  startDate: string;
  endDate: string;
  periods: ClassPeriodDto[];
  days: ScheduleDayDto[];
}

export interface DailyScheduleLabDto {
  labId: number;
  labName: string;
  cells: ScheduleCellDto[];
}

export interface DailyScheduleDto {
  date: string;
  weekday: number;
  periods: ClassPeriodDto[];
  labs: DailyScheduleLabDto[];
}

export interface LabMaintenanceDto {
  id: number;
  labId: number;
  maintenanceDate: string;
  weekday: number;
  periodId: number;
  periodName?: string;
  reason: string;
  status: number;
  operatorUserId: number;
}

export interface DeviceDto {
  id: number;
  labId: number;
  deviceName: string;
  deviceCode: string;
  brand?: string;
  modelNo?: string;
  quantity: number;
  availableQuantity: number;
  status: number;
  purchaseDate?: string;
  remark?: string;
}

export interface ConsumableDto {
  id: number;
  labId: number;
  consumableName: string;
  consumableCode: string;
  unit: string;
  stockQuantity: number;
  warningThreshold: number;
  remark?: string;
}

export interface ReservationDto {
  id: number;
  reservationNo: string;
  labId: number;
  applicantUserId: number;
  applicantName?: string;
  approverUserId?: number;
  reservationType: number;
  priorityLevel: number;
  usagePurpose: string;
  courseOrProjectName?: string;
  participantCount: number;
  contactPhone?: string;
  status: number;
  rejectReason?: string;
  checkInTime?: string;
  checkOutTime?: string;
  createdAt?: string;
  updatedAt?: string;
  slots: ReservationSlotDto[];
}

export interface ReservationSlotDto {
  id: number;
  reservationId: number;
  reservationDate: string;
  weekday: number;
  periodId: number;
  periodNo: number;
  periodName: string;
  slotStatus: number;
}

export interface ReservationAuditLogDto {
  id: number;
  reservationId: number;
  auditUserId: number;
  auditAction: number;
  auditComment?: string;
  createdAt: string;
}

export interface ReservationConflictReservationDto {
  reservationId: number;
  reservationNo: string;
  applicantUserId: number;
  applicantName: string;
  reservationType: number;
  priorityLevel: number;
  status: number;
  createdAt: string;
  usagePurpose?: string;
  courseOrProjectName?: string;
}

export interface ReservationConflictSlotDto {
  labId: number;
  labName: string;
  reservationDate: string;
  weekday: number;
  periodId: number;
  periodName: string;
  startTime?: string;
  endTime?: string;
  conflictCount: number;
  reservations: ReservationConflictReservationDto[];
}

export interface ViolationRecordDto {
  id: number;
  userId: number;
  reservationId?: number;
  violationType: number;
  scoreChange: number;
  remark: string;
  createdAt: string;
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
  priorityLevel?: number;
  usagePurpose: string;
  courseOrProjectName?: string;
  participantCount: number;
  contactPhone: string;
  slots: Array<{ reservationDate: string; periodId: number }>;
}

export interface SlotRecommendationItem {
  labId: number;
  labName: string;
  reservationDate: string;
  periodId: number;
  periodName?: string;
  recommendationReason: string;
}

export interface ReservationApplyResponse {
  submitted: boolean;
  currentStatus: string;
  conflict: boolean;
  conflictNote?: string;
  reservation?: ReservationDto;
  recommendations: SlotRecommendationItem[];
}
