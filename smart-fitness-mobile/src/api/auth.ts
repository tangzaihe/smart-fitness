import { apiRequest } from '@/src/api/client';
import type { TokenVO } from '@/src/types';

export function register(email: string, password: string): Promise<TokenVO> {
  return apiRequest<TokenVO>('/v1/auth/register', {
    method: 'POST',
    auth: false,
    body: { email, password },
  });
}

export function login(account: string, password: string): Promise<TokenVO> {
  return apiRequest<TokenVO>('/v1/auth/login', {
    method: 'POST',
    auth: false,
    body: { account, password },
  });
}

export function logout(refreshToken?: string | null): Promise<void> {
  return apiRequest<void>('/v1/auth/logout', {
    method: 'POST',
    body: refreshToken ? { refreshToken } : {},
  });
}

export const signIn = login;
export const signUp = register;
export const signOut = logout;
