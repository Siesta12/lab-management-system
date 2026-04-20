import type {
  PageData,
  PasswordUpdatePayload,
  UserProfileUpdatePayload,
  UserVO,
  ViolationRecordDto,
} from '../types';
import { get, patch, put } from './http';

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
