import { apiRequest } from '@/src/api/client';
import { asId, recordOf } from '@/src/api/json';
import type { AthleticState, AthleteVO, UpdateAthleteRequest, WellnessLog, WellnessRequest } from '@/src/types';

function mapAthlete(raw: unknown): AthleteVO {
  const row = recordOf(raw);
  return {
    athleteId: asId(row.athleteId),
    displayName: String(row.displayName ?? ''),
    sex: (row.sex as string | null) ?? null,
    birthDate: (row.birthDate as string | null) ?? null,
    heightCm: row.heightCm == null ? null : Number(row.heightCm),
    goal: String(row.goal ?? ''),
    weeklyMin: row.weeklyMin == null ? null : Number(row.weeklyMin),
    equipment: Array.isArray(row.equipment) ? row.equipment.map(String) : [],
    preferences: row.preferences,
    onboarded: Boolean(row.onboarded),
    onboardedAt: (row.onboardedAt as string | null) ?? null,
  };
}

export async function getMe(): Promise<AthleteVO> {
  return mapAthlete(await apiRequest('/v1/athlete/me'));
}

export async function updateMe(body: UpdateAthleteRequest): Promise<AthleteVO> {
  return mapAthlete(await apiRequest('/v1/athlete/me', { method: 'PUT', body }));
}

export function putWellnessToday(body: WellnessRequest): Promise<WellnessLog> {
  return apiRequest<WellnessLog>('/v1/wellness/today', { method: 'PUT', body });
}

export async function getReadiness(): Promise<AthleticState> {
  const row = recordOf(await apiRequest('/v1/readiness/current'));
  return {
    id: asId(row.id),
    athleteId: asId(row.athleteId),
    asOf: String(row.asOf ?? ''),
    readiness: Number(row.readiness ?? 0),
    recovery: Number(row.recovery ?? 0),
    fatigueByMuscle: String(row.fatigueByMuscle ?? '{}'),
    sleepHours: Number(row.sleepHours ?? 0),
    source: String(row.source ?? ''),
    calcVersion: String(row.calcVersion ?? ''),
  };
}

export const fetchMe = getMe;
export const saveMe = updateMe;
export const saveWellness = putWellnessToday;
export const fetchReadiness = getReadiness;
export const loadAthlete = getMe;
