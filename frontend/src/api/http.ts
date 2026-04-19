import type { ApiResponse } from '../types';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api';

export class ApiError extends Error {
  code: number;

  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

function buildHeaders(token?: string, hasBody = false): HeadersInit {
  const headers: Record<string, string> = {};

  if (hasBody) {
    headers['Content-Type'] = 'application/json';
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...buildHeaders(token, Boolean(init.body)),
      ...(init.headers ?? {}),
    },
  });

  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok) {
    throw new ApiError(response.status, body?.message ?? '请求失败');
  }

  if (body.code !== 200) {
    throw new ApiError(body.code, body.message || '请求失败');
  }

  return body.data;
}

export function get<T>(path: string, token?: string): Promise<T> {
  return request<T>(path, { method: 'GET' }, token);
}

export function post<T>(path: string, payload?: unknown, token?: string): Promise<T> {
  return request<T>(
    path,
    {
      method: 'POST',
      body: payload ? JSON.stringify(payload) : undefined,
    },
    token,
  );
}

export function patch<T>(path: string, payload?: unknown, token?: string): Promise<T> {
  return request<T>(
    path,
    {
      method: 'PATCH',
      body: payload ? JSON.stringify(payload) : undefined,
    },
    token,
  );
}
