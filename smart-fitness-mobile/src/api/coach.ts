import { apiRequest } from '@/src/api/client';
import { asId, recordOf } from '@/src/api/json';
import type { DecideAction, DecideResult } from '@/src/types';

export async function decideAdvice(adviceId: string, action: DecideAction, note?: string): Promise<DecideResult> {
  const row = recordOf(
    await apiRequest(`/v1/advice/${adviceId}/decide`, {
      method: 'POST',
      body: { action, note },
    }),
  );
  return {
    adviceId: asId(row.adviceId),
    status: String(row.status ?? ''),
    sessionId: asId(row.sessionId),
    idempotent: Boolean(row.idempotent),
  };
}

export const confirmAdvice = decideAdvice;
