import { ApiError, get, getApiBaseUrl } from './http';

export interface StatisticsQuery {
  startDate?: string;
  endDate?: string;
  labId?: number | string;
  labType?: string;
  status?: number | string;
  reservationType?: number | string;
  exportType?: string;
}

export interface StatisticsOption {
  label: string;
  value: string;
}

export interface StatisticsOptions {
  labs: StatisticsOption[];
  reservationStatuses: StatisticsOption[];
  reservationTypes: StatisticsOption[];
  exportTypes: StatisticsOption[];
}

export interface ChartItem {
  name: string;
  value: number;
  rate: number;
}

export interface RankItem {
  id?: number | null;
  name: string;
  secondary?: string;
  value: number;
  rate?: number;
}

export interface OverviewStats {
  monthReservationCount: number;
  todayReservationCount: number;
  pendingReservationCount: number;
  labUsageRate: number;
  brokenDeviceCount: number;
  repairingDeviceCount: number;
  lowStockConsumableCount: number;
  monthViolationCount: number;
  averageCreditScore: number;
  lowCreditUserCount: number;
}

export interface ReservationStats {
  totalCount: number;
  pendingCount: number;
  completedCount: number;
  completionRate: number;
  trend: ChartItem[];
  statusDistribution: ChartItem[];
  typeDistribution: ChartItem[];
  applicantRoleDistribution: ChartItem[];
  labTypeDistribution: ChartItem[];
}

export interface LabUsageStats {
  usageRate: number;
  totalOccupiedSlots: number;
  highUsageLabCount: number;
  idleLabCount: number;
  reservationRanking: RankItem[];
  typeUsageRates: RankItem[];
  highUsageLabs: RankItem[];
  idleLabs: RankItem[];
  timeHeat: ChartItem[];
}

export interface DeviceStats {
  totalCount: number;
  normalCount: number;
  repairingCount: number;
  disabledCount: number;
  repairOrderCount: number;
  categoryLabel: string;
  statusDistribution: ChartItem[];
  categoryDistribution: ChartItem[];
  repairTrend: ChartItem[];
  labDeviceCounts: RankItem[];
  abnormalDevices: RankItem[];
}

export interface ConsumableStats {
  totalTypeCount: number;
  lowStockCount: number;
  monthInQuantity: number;
  monthOutQuantity: number;
  lowStockRate: number;
  inTrend: ChartItem[];
  outTrend: ChartItem[];
  consumptionRanking: RankItem[];
  labUsageRanking: RankItem[];
  warningList: RankItem[];
}

export interface CreditStats {
  monthLateCount: number;
  monthNoShowCount: number;
  averageCreditScore: number;
  lowCreditUserCount: number;
  violationTypeDistribution: ChartItem[];
  violationTrend: ChartItem[];
  lateTrend: ChartItem[];
  noShowTrend: ChartItem[];
  creditScoreDistribution: ChartItem[];
  roleViolationDistribution: ChartItem[];
  reservationTypeDistribution: ChartItem[];
  timeSegmentDistribution: ChartItem[];
  lowCreditUsers: RankItem[];
  violationRanking: RankItem[];
}

function buildQuery(query: StatisticsQuery = {}): string {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value) !== '') {
      params.set(key, String(value));
    }
  });
  const search = params.toString();
  return search ? `?${search}` : '';
}

export function fetchStatisticsOptions(token: string): Promise<StatisticsOptions> {
  return get<StatisticsOptions>('/statistics/admin/options', token);
}

export function fetchStatisticsOverview(query: StatisticsQuery, token: string): Promise<OverviewStats> {
  return get<OverviewStats>(`/statistics/admin/overview${buildQuery(query)}`, token);
}

export function fetchReservationStatistics(query: StatisticsQuery, token: string): Promise<ReservationStats> {
  return get<ReservationStats>(`/statistics/admin/reservations${buildQuery(query)}`, token);
}

export function fetchLabUsageStatistics(query: StatisticsQuery, token: string): Promise<LabUsageStats> {
  return get<LabUsageStats>(`/statistics/admin/labs/usage${buildQuery(query)}`, token);
}

export function fetchDeviceStatistics(query: StatisticsQuery, token: string): Promise<DeviceStats> {
  return get<DeviceStats>(`/statistics/admin/devices${buildQuery(query)}`, token);
}

export function fetchConsumableStatistics(query: StatisticsQuery, token: string): Promise<ConsumableStats> {
  return get<ConsumableStats>(`/statistics/admin/consumables${buildQuery(query)}`, token);
}

export function fetchCreditStatistics(query: StatisticsQuery, token: string): Promise<CreditStats> {
  return get<CreditStats>(`/statistics/admin/credit${buildQuery(query)}`, token);
}

function fallbackExportFilename(exportType?: string): string {
  const timestamp = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
  const labelMap: Record<string, string> = {
    reservation: '预约数据',
    labUsage: '实验室使用统计',
    device: '设备统计',
    consumable: '耗材统计数据',
    creditViolation: '信用违规数据',
  };
  return `${labelMap[exportType || ''] || '统计导出数据'}_${timestamp}.xlsx`;
}

function resolveDownloadFilename(disposition: string, exportType?: string): string {
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }
  const plainMatch = disposition.match(/filename="?([^";]+)"?/i);
  if (plainMatch?.[1]) {
    return decodeURIComponent(plainMatch[1]);
  }
  return fallbackExportFilename(exportType);
}

export async function downloadStatisticsExport(query: StatisticsQuery, token: string): Promise<void> {
  const response = await fetch(`${getApiBaseUrl()}/statistics/admin/export${buildQuery(query)}`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    let message = `导出失败：${response.status}`;
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message || message;
    } catch {
      // Keep HTTP fallback message.
    }
    throw new ApiError(response.status, message);
  }

  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition') || '';
  const filename = resolveDownloadFilename(disposition, query.exportType);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
