import type {
  DeviceDto,
  DeviceRepairCreatePayload,
  DeviceRepairDto,
  DeviceRepairStatusUpdatePayload,
  DeviceSavePayload,
  PageData,
} from '../types';
import { del, get, patch, post, put } from './http';

export interface DeviceQuery {
  labId?: number;
  pageNum?: number;
  pageSize?: number;
  deviceName?: string;
  deviceCode?: string;
  status?: number;
}

export interface DeviceRepairQuery {
  labId?: number;
  deviceId?: number;
  status?: number;
  pageNum?: number;
  pageSize?: number;
}

export function fetchDevices(query: DeviceQuery = {}, token?: string): Promise<PageData<DeviceDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 20));

  if (query.labId !== undefined) {
    params.set('labId', String(query.labId));
  }
  if (query.deviceName) {
    params.set('deviceName', query.deviceName);
  }
  if (query.deviceCode) {
    params.set('deviceCode', query.deviceCode);
  }
  if (query.status !== undefined) {
    params.set('status', String(query.status));
  }

  return get<PageData<DeviceDto>>(`/devices?${params.toString()}`, token);
}

export function fetchDeviceById(id: number, token?: string): Promise<DeviceDto> {
  return get<DeviceDto>(`/devices/${id}`, token);
}

export function createDevice(payload: DeviceSavePayload, token: string): Promise<DeviceDto> {
  return post<DeviceDto>('/devices', payload, token);
}

export function updateDevice(id: number, payload: DeviceSavePayload, token: string): Promise<DeviceDto> {
  return put<DeviceDto>(`/devices/${id}`, payload, token);
}

export function deleteDevice(id: number, token: string): Promise<void> {
  return del<void>(`/devices/${id}`, token);
}

export function updateDeviceStatus(id: number, status: number, token: string): Promise<void> {
  return patch<void>(`/devices/${id}/status`, { status }, token);
}

export function fetchDeviceRepairs(query: DeviceRepairQuery = {}, token?: string): Promise<PageData<DeviceRepairDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 20));

  if (query.labId !== undefined) {
    params.set('labId', String(query.labId));
  }
  if (query.deviceId !== undefined) {
    params.set('deviceId', String(query.deviceId));
  }
  if (query.status !== undefined) {
    params.set('status', String(query.status));
  }

  return get<PageData<DeviceRepairDto>>(`/device-repairs?${params.toString()}`, token);
}

export function createDeviceRepair(payload: DeviceRepairCreatePayload, token: string): Promise<DeviceRepairDto> {
  return post<DeviceRepairDto>('/device-repairs', payload, token);
}

export function updateDeviceRepairStatus(
  id: number,
  payload: DeviceRepairStatusUpdatePayload,
  token: string,
): Promise<DeviceRepairDto> {
  return patch<DeviceRepairDto>(`/device-repairs/${id}/status`, payload, token);
}
