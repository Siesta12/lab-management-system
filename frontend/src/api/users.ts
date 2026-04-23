import type {
  PageData,
  PasswordUpdatePayload,
  UserImportResult,
  UserCreatePayload,
  UserProfileUpdatePayload,
  UserUpdatePayload,
  UserVO,
  ViolationRecordDto,
} from '../types';
import { ApiError, del, get, getApiBaseUrl, patch, post, put } from './http';

export interface UserQuery {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  realName?: string;
  departmentId?: number;
  roleCode?: string;
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
  if (query.roleCode) {
    params.set('roleCode', query.roleCode);
  }
  if (query.status !== undefined) {
    params.set('status', String(query.status));
  }
  return get<PageData<UserVO>>(`/users?${params.toString()}`, token);
}

export function fetchMyProfile(token: string): Promise<UserVO> {
  return get<UserVO>('/users/profile', token);
}

export function fetchUserById(id: number, token: string): Promise<UserVO> {
  return get<UserVO>(`/users/${id}`, token);
}

export function createUser(payload: UserCreatePayload, token: string): Promise<UserVO> {
  return post<UserVO>('/users', payload, token);
}

export function updateUser(id: number, payload: UserUpdatePayload, token: string): Promise<UserVO> {
  return put<UserVO>(`/users/${id}`, payload, token);
}

export function deleteUser(id: number, token: string): Promise<void> {
  return del<void>(`/users/${id}`, token);
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

async function parseJsonResponse<T>(response: Response): Promise<T> {
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, payload?.message ?? `请求失败：${response.status}`);
  }
  if (!payload) {
    throw new ApiError(response.status, '服务端返回了无效响应');
  }
  if (payload.code !== 200) {
    throw new ApiError(payload.code, payload.message || '请求失败');
  }
  return payload.data as T;
}

export async function importUsers(file: File, token: string): Promise<UserImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${getApiBaseUrl()}/admin/users/import`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: formData,
  });
  return parseJsonResponse<UserImportResult>(response);
}

export async function downloadImportTemplate(token: string): Promise<Blob> {
  const response = await fetch(`${getApiBaseUrl()}/admin/users/import/template`, {
    method: 'GET',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new ApiError(response.status, payload?.message ?? `请求失败：${response.status}`);
  }
  return response.blob();
}
