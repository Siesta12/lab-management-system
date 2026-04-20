import type { DailyScheduleDto, LabDto, LabMaintenanceDto, LabScheduleDto, PageData } from '../types';
import { get, post, put } from './http';

export interface LabQuery {
  pageNum?: number;
  pageSize?: number;
  labName?: string;
}

export function fetchLabs(query: LabQuery = {}, token?: string): Promise<PageData<LabDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));

  if (query.labName) {
    params.set('labName', query.labName);
  }

  return get<PageData<LabDto>>(`/labs?${params.toString()}`, token);
}

export function fetchLabById(id: number, token?: string): Promise<LabDto> {
  return get<LabDto>(`/labs/${id}`, token);
}

export function fetchLabSchedule(labId: number, token: string, startDate?: string): Promise<LabScheduleDto> {
  const params = new URLSearchParams();
  if (startDate) {
    params.set('startDate', startDate);
  }
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

