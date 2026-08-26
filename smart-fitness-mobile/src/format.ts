import { ApiError } from '@/src/api/errors';

export function messageOf(error: unknown, fallback = '请求失败'): string {
  if (error instanceof ApiError) {
    return error.displayMessage;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

export function isQuotaError(error: unknown): boolean {
  return error instanceof ApiError && error.code === 3005;
}

export const errorMessage = messageOf;
export const isQuotaBlocked = isQuotaError;
