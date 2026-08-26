import { apiRequest } from '@/src/api/client';
import { asId, asNumber, recordOf } from '@/src/api/json';
import type { CurrentSession, SessionLog, SetLog } from '@/src/types';

function mapSession(raw: unknown): SessionLog {
  const row = recordOf(raw);
  return {
    id: asId(row.id),
    athleteId: asId(row.athleteId),
    adviceId: row.adviceId == null ? undefined : asId(row.adviceId),
    status: String(row.status ?? ''),
    startedAt: row.startedAt == null ? undefined : String(row.startedAt),
    endedAt: row.endedAt == null ? null : String(row.endedAt),
    perceivedExertion: asNumber(row.perceivedExertion),
    notes: row.notes == null ? null : String(row.notes),
  };
}

function mapSet(raw: unknown): SetLog {
  const row = recordOf(raw);
  return {
    id: asId(row.id),
    sessionId: asId(row.sessionId),
    exerciseCode: String(row.exerciseCode ?? ''),
    muscleGroup: row.muscleGroup == null ? undefined : String(row.muscleGroup),
    setIndex: Number(row.setIndex ?? 0),
    reps: asNumber(row.reps),
    loadKg: asNumber(row.loadKg),
    rpe: asNumber(row.rpe),
    completed: Boolean(row.completed),
  };
}

export async function getCurrentSession(): Promise<CurrentSession | null> {
  const data = await apiRequest<unknown>('/v1/sessions/current');
  if (data == null) {
    return null;
  }
  const row = recordOf(data);
  if (!row.session) {
    return null;
  }
  return {
    session: mapSession(row.session),
    sets: Array.isArray(row.sets) ? row.sets.map(mapSet) : [],
  };
}

export async function getSession(id: string): Promise<CurrentSession> {
  const row = recordOf(await apiRequest(`/v1/sessions/${id}`));
  return {
    session: mapSession(row.session),
    sets: Array.isArray(row.sets) ? row.sets.map(mapSet) : [],
  };
}

export function patchSet(
  sessionId: string,
  setId: string,
  body: { reps?: number; loadKg?: number; rpe?: number; completed?: boolean; exerciseCode?: string },
): Promise<SetLog> {
  return apiRequest(`/v1/sessions/${sessionId}/sets/${setId}`, { method: 'PATCH', body }).then(mapSet);
}

export function completeSession(sessionId: string, body?: { perceivedExertion?: number; notes?: string }): Promise<SessionLog> {
  return apiRequest(`/v1/sessions/${sessionId}/complete`, { method: 'POST', body: body ?? {} }).then(mapSession);
}

export function abandonSession(sessionId: string): Promise<SessionLog> {
  return apiRequest(`/v1/sessions/${sessionId}/abandon`, { method: 'POST' }).then(mapSession);
}

export const fetchCurrentSession = getCurrentSession;
export const finishSession = completeSession;
export const dropSession = abandonSession;
