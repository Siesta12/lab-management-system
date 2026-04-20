import type { DeviceDto, PageData } from '../types';
import { get } from './http';

export function fetchDevices(query: { labId?: number; pageNum?: number; pageSize?: number } = {}, token?: string): Promise<PageData<DeviceDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 20));

  if (query.labId) {
    params.set('labId', String(query.labId));
  }

  return get<PageData<DeviceDto>>(`/devices?${params.toString()}`, token);
}
