import type { ConsumableDto, PageData } from '../types';
import { get } from './http';

export function fetchConsumables(query: { labId?: number; pageNum?: number; pageSize?: number } = {}, token?: string): Promise<PageData<ConsumableDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 20));

  if (query.labId) {
    params.set('labId', String(query.labId));
  }

  return get<PageData<ConsumableDto>>(`/consumables?${params.toString()}`, token);
}

