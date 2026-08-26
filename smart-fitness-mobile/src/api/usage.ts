import { apiRequest } from '@/src/api/client';
import { asId, recordOf } from '@/src/api/json';
import type { LlmUsageDaily } from '@/src/types';

export async function getUsage(): Promise<LlmUsageDaily[]> {
  const data = await apiRequest<unknown>('/v1/me/usage');
  const rows = Array.isArray(data) ? data : [];
  return rows.map((item) => {
    const row = recordOf(item);
    return {
      id: asId(row.id),
      athleteId: asId(row.athleteId),
      usageDate: String(row.usageDate ?? ''),
      keySource: String(row.keySource ?? ''),
      callCount: Number(row.callCount ?? 0),
      failCount: Number(row.failCount ?? 0),
      promptTokens: Number(row.promptTokens ?? 0),
      completionTokens: Number(row.completionTokens ?? 0),
      totalTokens: Number(row.totalTokens ?? 0),
    };
  });
}

export const fetchUsage = getUsage;
