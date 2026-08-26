import { create } from 'zustand';
import { storageDelete, storageGet, storageSet } from '@/src/auth/storage';
import type { TokenVO } from '@/src/types';

const ACCESS = 'sf.accessToken';
const REFRESH = 'sf.refreshToken';
const ATHLETE = 'sf.athleteId';
const ONBOARDED = 'sf.onboarded';

interface SessionState {
  ready: boolean;
  accessToken: string | null;
  refreshToken: string | null;
  athleteId: string | null;
  onboarded: boolean;
  quotaBlocked: boolean;
  hydrate: () => Promise<void>;
  applyTokens: (tokens: TokenVO) => Promise<void>;
  setOnboarded: (value: boolean) => Promise<void>;
  setQuotaBlocked: (value: boolean) => void;
  clear: () => Promise<void>;
}

export const useSessionStore = create<SessionState>((set) => ({
  ready: false,
  accessToken: null,
  refreshToken: null,
  athleteId: null,
  onboarded: false,
  quotaBlocked: false,
  hydrate: async () => {
    const [accessToken, refreshToken, athleteId, onboarded] = await Promise.all([
      storageGet(ACCESS),
      storageGet(REFRESH),
      storageGet(ATHLETE),
      storageGet(ONBOARDED),
    ]);
    set({
      ready: true,
      accessToken,
      refreshToken,
      athleteId,
      onboarded: onboarded === '1',
    });
  },
  applyTokens: async (tokens) => {
    await Promise.all([
      storageSet(ACCESS, tokens.accessToken),
      storageSet(REFRESH, tokens.refreshToken),
      storageSet(ATHLETE, tokens.athleteId),
      storageSet(ONBOARDED, tokens.onboarded ? '1' : '0'),
    ]);
    set({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      athleteId: tokens.athleteId,
      onboarded: tokens.onboarded,
    });
  },
  setOnboarded: async (value) => {
    await storageSet(ONBOARDED, value ? '1' : '0');
    set({ onboarded: value });
  },
  setQuotaBlocked: (value) => set({ quotaBlocked: value }),
  clear: async () => {
    await Promise.all([storageDelete(ACCESS), storageDelete(REFRESH), storageDelete(ATHLETE), storageDelete(ONBOARDED)]);
    set({
      accessToken: null,
      refreshToken: null,
      athleteId: null,
      onboarded: false,
      quotaBlocked: false,
    });
  },
}));

export const useAuthStore = useSessionStore;
