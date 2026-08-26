import { fetch as expoFetch } from 'expo/fetch';
import { ApiError } from '@/src/api/errors';
import { parseApiJson, recordOf } from '@/src/api/json';
import { useSessionStore } from '@/src/auth/session-store';
import type { ApiResult, TokenVO } from '@/src/types';

export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

interface RequestOptions {
  method?: Method;
  body?: unknown;
  auth?: boolean;
  retry?: boolean;
  accept?: string;
}

let refreshPromise: Promise<boolean> | null = null;

function streamingFetch(url: string, init: RequestInit): Promise<Response> {
  return expoFetch(url, init) as unknown as Promise<Response>;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET';
  const headers: Record<string, string> = {
    Accept: options.accept ?? 'application/json',
  };
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (options.auth !== false) {
    const token = useSessionStore.getState().accessToken;
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }
  const response = await streamingFetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const text = await response.text();
  const parsed = text ? parseApiJson(text) : {};
  const result = parsed as ApiResult<T>;
  if (result && typeof result === 'object' && 'code' in result) {
    if (result.code === 0) {
      return result.data;
    }
    const error = new ApiError(result.code, result.message, response.status);
    if ((error.code === 2001 || error.code === 2002) && options.auth !== false && options.retry !== false) {
      const refreshed = await refreshTokens();
      if (refreshed) {
        return apiRequest<T>(path, { ...options, retry: false });
      }
      await useSessionStore.getState().clear();
    }
    if (error.code === 3005) {
      useSessionStore.getState().setQuotaBlocked(true);
    }
    if (error.code === 3001) {
      await useSessionStore.getState().setOnboarded(false);
    }
    throw error;
  }
  if (!response.ok) {
    throw new ApiError(5001, text || response.statusText, response.status);
  }
  return parsed as T;
}

export async function refreshTokens(): Promise<boolean> {
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = (async () => {
    const refreshToken = useSessionStore.getState().refreshToken;
    if (!refreshToken) {
      return false;
    }
    try {
      const data = await apiRequest<TokenVO>('/v1/auth/refresh', {
        method: 'POST',
        auth: false,
        body: { refreshToken },
      });
      await useSessionStore.getState().applyTokens(data);
      return true;
    } catch {
      return false;
    }
  })();
  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

export async function openSse(path: string): Promise<Response> {
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
  };
  const token = useSessionStore.getState().accessToken;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  let response = await streamingFetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers,
  });
  if (response.status === 401) {
    const ok = await refreshTokens();
    if (ok) {
      const retryHeaders: Record<string, string> = {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${useSessionStore.getState().accessToken ?? ''}`,
      };
      response = await streamingFetch(`${API_BASE_URL}${path}`, { method: 'POST', headers: retryHeaders });
    }
  }
  const contentType = response.headers.get('content-type') ?? '';
  if (!response.ok || !contentType.includes('text/event-stream')) {
    const text = await response.text();
    const parsed = text ? recordOf(parseApiJson(text)) : {};
    const code = typeof parsed.code === 'number' ? parsed.code : 5001;
    const message = typeof parsed.message === 'string' ? parsed.message : '教练流开启失败';
    if (code === 3005) {
      useSessionStore.getState().setQuotaBlocked(true);
    }
    if (code === 2001 || code === 2002) {
      await useSessionStore.getState().clear();
    }
    if (code === 3001) {
      await useSessionStore.getState().setOnboarded(false);
    }
    throw new ApiError(code, message, response.status);
  }
  return response;
}

export const requestApi = apiRequest;
export const startSse = openSse;
