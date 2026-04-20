import type { PageData, ViolationRecordDto } from '../types';
import { get } from './http';

export function fetchMyViolations(token: string, pageNum = 1, pageSize = 20): Promise<PageData<ViolationRecordDto>> {
  return get<PageData<ViolationRecordDto>>(`/violations/mine?pageNum=${pageNum}&pageSize=${pageSize}`, token);
}
