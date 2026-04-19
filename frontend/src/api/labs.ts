import type { LabDto, PageData } from '../types';
import { get } from './http';

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
