import type {
  PageData,
  PasswordUpdatePayload,
  UserProfileUpdatePayload,
  UserVO,
  ViolationRecordDto,
} from '../types';
import { get, patch, put } from './http';

export interface UserQuery {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  realName?: string;
  departmentId?: number;
  status?: number;
}

export function fetchUsers(query: UserQuery = {}, token: string): Promise<PageData<UserVO>> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.username) {
    params.set('username', query.username);
  }
  if (query.realName) {
    params.set('realName', query.realName);
  }
  if (query.departmentId !== undefined) {
    params.set('departmentId', String(query.departmentId));
  }
  if (query.status !== undefined) {
    params.set('status', String(query.status));
  }
  return get<PageData<UserVO>>(`/users?${params.toString()}`, token);
}

export function fetchMyProfile(token: string): Promise<UserVO> {
  return get<UserVO>('/users/profile', token);
}

export function updateMyProfile(payload: UserProfileUpdatePayload, token: string): Promise<UserVO> {
  return put<UserVO>('/users/profile', payload, token);
}

export function updateMyPassword(payload: PasswordUpdatePayload, token: string): Promise<void> {
  return patch<void>('/users/password', payload, token);
}

export function fetchUserViolations(id: number, token: string, pageNum = 1, pageSize = 20): Promise<PageData<ViolationRecordDto>> {
  return get<PageData<ViolationRecordDto>>(`/users/${id}/violations?pageNum=${pageNum}&pageSize=${pageSize}`, token);
}

