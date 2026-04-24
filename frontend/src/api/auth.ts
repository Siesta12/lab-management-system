import type { AuthUser, LoginResponseData } from '../types';
import { get, post } from './http';

export interface LoginPayload {
  userNo: string;
  password: string;
}

export function login(payload: LoginPayload): Promise<LoginResponseData> {
  return post<LoginResponseData>('/auth/login', payload);
}

export function fetchCurrentUser(token: string): Promise<AuthUser> {
  return get<AuthUser>('/auth/me', token);
}

