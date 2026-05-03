import type { PageData, ViolationRecordDto } from '../types';
import { get } from './http';

export interface MyViolationQuery {
  pageNum?: number;
  pageSize?: number;
  violationType?: number;
  scoreDirection?: number;
}

export function fetchMyViolations(token: string, query: MyViolationQuery = {}): Promise<PageData<ViolationRecordDto>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.violationType !== undefined && query.violationType !== null) {
    params.set('violationType', String(query.violationType));
  }
  if (query.scoreDirection !== undefined && query.scoreDirection !== null) {
    params.set('scoreDirection', String(query.scoreDirection));
  }
  return get<PageData<ViolationRecordDto>>(`/violations/mine?${params.toString()}`, token);
}
