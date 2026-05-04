import type {
  ConsumableDto,
  ConsumableSavePayload,
  ConsumableStockUpdatePayload,
  ConsumableUsageDto,
  ConsumableUsageQuery,
  PageData,
} from '../types';
import { del, get, patch, post, put } from './http';

export interface ConsumableQuery {
  labId?: number;
  labType?: string;
  pageNum?: number;
  pageSize?: number;
  consumableName?: string;
  consumableCode?: string;
  status?: number;
  warningOnly?: boolean;
}

function buildConsumableQuery(query: ConsumableQuery): string {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 20));
  if (query.labId) params.set('labId', String(query.labId));
  if (query.labType) params.set('labType', query.labType);
  if (query.consumableName) params.set('consumableName', query.consumableName);
  if (query.consumableCode) params.set('consumableCode', query.consumableCode);
  if (query.status !== undefined && query.status !== null) params.set('status', String(query.status));
  if (query.warningOnly) params.set('warningOnly', 'true');
  return params.toString();
}

function buildUsageQuery(query: ConsumableUsageQuery): string {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.status !== undefined && query.status !== null) params.set('status', String(query.status));
  if (query.labId) params.set('labId', String(query.labId));
  if (query.keyword) params.set('keyword', query.keyword);
  return params.toString();
}

export function fetchConsumables(query: ConsumableQuery = {}, token?: string): Promise<PageData<ConsumableDto>> {
  return get<PageData<ConsumableDto>>(`/consumables?${buildConsumableQuery(query)}`, token);
}

export function fetchConsumableOptions(labId: number, token?: string): Promise<ConsumableDto[]> {
  return get<ConsumableDto[]>(`/consumables/options?labId=${labId}`, token);
}

export function createConsumable(payload: ConsumableSavePayload, token: string): Promise<ConsumableDto> {
  return post<ConsumableDto>('/consumables', payload, token);
}

export function updateConsumable(id: number, payload: ConsumableSavePayload, token: string): Promise<ConsumableDto> {
  return put<ConsumableDto>(`/consumables/${id}`, payload, token);
}

export function deleteConsumable(id: number, token: string): Promise<void> {
  return del<void>(`/consumables/${id}`, token);
}

export function updateConsumableStock(
  id: number,
  payload: ConsumableStockUpdatePayload,
  token: string,
): Promise<ConsumableDto> {
  return patch<ConsumableDto>(`/consumables/${id}/stock`, payload, token);
}

export function fetchConsumableUsages(
  query: ConsumableUsageQuery = {},
  token: string,
): Promise<PageData<ConsumableUsageDto>> {
  return get<PageData<ConsumableUsageDto>>(`/consumables/usages?${buildUsageQuery(query)}`, token);
}
