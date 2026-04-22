import type { DailyScheduleDto, LabDto, LabMaintenanceDto, LabScheduleDto, OptionItem, PageData } from '../types';
import { get, patch, post, put } from './http';

export interface LabQuery {
  pageNum?: number;
  pageSize?: number;
  labId?: number;
  labName?: string;
  labCode?: string;
  labType?: string;
  departmentId?: number;
  openStatus?: number;
  labStatus?: number;
}

export function fetchLabs(query: LabQuery = {}, token?: string): Promise<PageData<LabDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));

  if (query.labId !== undefined) {
    params.set('labId', String(query.labId));
  }
  if (query.labName) {
    params.set('labName', query.labName);
  }
  if (query.labCode) {
    params.set('labCode', query.labCode);
  }
  if (query.labType) {
    params.set('labType', query.labType);
  }
  if (query.departmentId) {
    params.set('departmentId', String(query.departmentId));
  }
  if (query.openStatus !== undefined) {
    params.set('openStatus', String(query.openStatus));
  }
  if (query.labStatus !== undefined) {
    params.set('labStatus', String(query.labStatus));
  }

  return get<PageData<LabDto>>(`/labs?${params.toString()}`, token);
}

export function fetchLabById(id: number, token?: string): Promise<LabDto> {
  return get<LabDto>(`/labs/${id}`, token);
}

export function fetchLabOptions(
  query: { openStatus?: number; departmentId?: number } = {},
  token?: string,
): Promise<OptionItem[]> {
  const params = new URLSearchParams();
  if (query.openStatus !== undefined) {
    params.set('openStatus', String(query.openStatus));
  }
  if (query.departmentId !== undefined) {
    params.set('departmentId', String(query.departmentId));
  }
  const suffix = params.toString() ? `?${params.toString()}` : '';
  return get<OptionItem[]>(`/labs/options${suffix}`, token);
}

export function updateLab(id: number, payload: Partial<LabDto>, token: string): Promise<LabDto> {
  return put<LabDto>(`/labs/${id}`, payload, token);
}

export function updateLabOpenStatus(id: number, status: number, token: string): Promise<void> {
  return patch<void>(`/labs/${id}/open-status`, { status }, token);
}

export function updateLabStatus(id: number, status: number, token: string): Promise<void> {
  return patch<void>(`/labs/${id}/lab-status`, { status }, token);
}

export function fetchLabSchedule(labId: number, token: string, startDate?: string): Promise<LabScheduleDto> {
  const params = new URLSearchParams();
  if (startDate) {
    params.set('startDate', startDate);
  }
  params.set('_t', Date.now().toString());
  const suffix = params.toString() ? `?${params.toString()}` : '';
  return get<LabScheduleDto>(`/labs/${labId}/schedule${suffix}`, token);
}

export function fetchDailySchedule(date: string, token: string): Promise<DailyScheduleDto> {
  const params = new URLSearchParams();
  params.set('date', date);
  return get<DailyScheduleDto>(`/labs/schedule/daily?${params.toString()}`, token);
}

export function fetchLabMaintenance(labId: number, token: string): Promise<LabMaintenanceDto[]> {
  return get<LabMaintenanceDto[]>(`/labs/${labId}/maintenance`, token);
}

export function createLabMaintenance(
  labId: number,
  payload: { maintenanceDate: string; periodIds: number[]; reason: string },
  token: string,
): Promise<LabMaintenanceDto[]> {
  return post<LabMaintenanceDto[]>(`/labs/${labId}/maintenance`, payload, token);
}

export function cancelLabMaintenance(labId: number, maintenanceId: number, token: string): Promise<void> {
  return put<void>(`/labs/${labId}/maintenance/${maintenanceId}/cancel`, undefined, token);
}
