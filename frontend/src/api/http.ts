import type { ApiResponse } from '../types';

function resolveApiBaseUrl(): string {
  if (import.meta.env.DEV) {
    return '';
  }

  const envApiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim();

  if (typeof window === 'undefined') {
    return envApiBaseUrl || 'http://localhost:8080';
  }

  const currentHost = window.location.hostname;
  const isLocalHost = currentHost === 'localhost' || currentHost === '127.0.0.1';
  const envPointsToLocalHost = Boolean(envApiBaseUrl && /\/\/(localhost|127\.0\.0\.1)(:\d+)?/i.test(envApiBaseUrl));

  if (envApiBaseUrl && (!envPointsToLocalHost || isLocalHost)) {
    return envApiBaseUrl;
  }

  return `${window.location.protocol}//${currentHost}:8080`;
}

const API_BASE_URL = resolveApiBaseUrl();

export function getApiBaseUrl(): string {
  return API_BASE_URL || window.location.origin;
}

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
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: {
        ...buildHeaders(token, Boolean(init.body)),
        ...(init.headers ?? {}),
      },
    });
  } catch {
    throw new ApiError(0, `无法连接到服务端：${API_BASE_URL}`);
  }

  let body: ApiResponse<T> | null = null;
  try {
    body = (await response.json()) as ApiResponse<T>;
  } catch {
    body = null;
  }

  if (!response.ok) {
    throw new ApiError(response.status, body?.message ?? `请求失败：${response.status}`);
  }

  if (!body) {
    throw new ApiError(response.status, '服务端返回了无效响应');
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

export function put<T>(path: string, payload?: unknown, token?: string): Promise<T> {
  return request<T>(
    path,
    {
      method: 'PUT',
      body: payload ? JSON.stringify(payload) : undefined,
    },
    token,
  );
}


