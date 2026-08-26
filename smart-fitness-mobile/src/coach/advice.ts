import { asId, asNumber, recordOf } from '@/src/api/json';
import type { AdviceKind, AdvicePayload, AdviceSlot } from '@/src/types';

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}

function mapSlot(raw: unknown, index: number): AdviceSlot {
  const row = recordOf(raw);
  return {
    slot: String(row.slot ?? String.fromCharCode(65 + index)),
    pick: String(row.pick ?? ''),
    alternatives: stringList(row.alternatives ?? row.alts),
    sets: Number(row.sets ?? 0),
    reps: Number(row.reps ?? 0),
    loadKg: asNumber(row.loadKg ?? row.load_kg),
    rpeCap: asNumber(row.rpeCap ?? row.rpe_cap),
  };
}

export function normalizeAdvicePayload(raw: unknown): AdvicePayload {
  const row = recordOf(raw);
  const slotsRaw = Array.isArray(row.slots) ? row.slots : [];
  return {
    schemaVersion: Number(row.schemaVersion ?? row.schema_version ?? 1),
    kind: String(row.kind ?? 'SESSION') as AdviceKind,
    title: row.title == null ? undefined : String(row.title),
    rationale: row.rationale == null ? undefined : String(row.rationale),
    slots: slotsRaw.map(mapSlot),
  };
}

export function parseAdviceEvent(data: unknown): { adviceId: string; kind: string; payload: AdvicePayload } {
  const row = recordOf(data);
  const payloadRaw = row.payload ?? row;
  const payload = normalizeAdvicePayload(payloadRaw);
  return {
    adviceId: asId(row.adviceId ?? row.id),
    kind: String(row.kind ?? payload.kind ?? 'SESSION'),
    payload,
  };
}

export function toolLabel(data: unknown): string {
  const row = recordOf(data);
  const name = String(row.tool ?? row.name ?? '').toLowerCase();
  if (name.includes('observe') || name.includes('readiness')) {
    return '正在看你的恢复';
  }
  if (name.includes('retrieve') || name.includes('load') || name.includes('catalog')) {
    return '正在检索动作';
  }
  if (name) {
    return `工具：${name}`;
  }
  return '教练处理中';
}

export const parseAdvice = parseAdviceEvent;
export const labelForTool = toolLabel;
