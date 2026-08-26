export type Goal = 'HYPERTROPHY' | 'FAT_LOSS' | 'STRENGTH' | 'REHAB';

export type Equipment =
  | 'BARBELL'
  | 'DUMBBELL'
  | 'KETTLEBELL'
  | 'SMITH'
  | 'LEG_PRESS'
  | 'CABLE'
  | 'MACHINE'
  | 'BODYWEIGHT'
  | 'BAND'
  | 'PULLUP_BAR';

export type ConstraintType = 'PAIN' | 'INJURY' | 'MEDICAL' | 'TIME' | 'EQUIPMENT';

export type BodyPart =
  | 'SHOULDER'
  | 'ELBOW'
  | 'WRIST'
  | 'NECK'
  | 'LOWER_BACK'
  | 'KNEE'
  | 'HIP'
  | 'ANKLE'
  | 'OTHER';

export type AdviceKind = 'REST' | 'SESSION' | 'DELOAD';
export type DecideAction = 'ACCEPT' | 'REST' | 'REJECT';
export type CoachTrigger = 'OBSERVE' | 'MANUAL';

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface TokenVO {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  athleteId: string;
  onboarded: boolean;
}

export interface Preferences {
  liked: string[];
  disliked: string[];
  never: string[];
}

export interface ConstraintItem {
  type: ConstraintType;
  bodyPart?: string;
  severity?: number;
  startsOn?: string;
  endsOn?: string;
  notes?: string;
}

export interface UpdateAthleteRequest {
  displayName: string;
  sex?: string;
  birthDate?: string;
  heightCm?: number;
  goal: Goal;
  weeklyMin?: number;
  equipment: Equipment[];
  preferences?: Preferences;
  constraints?: ConstraintItem[];
}

export interface AthleteVO {
  athleteId: string;
  displayName: string;
  sex?: string | null;
  birthDate?: string | null;
  heightCm?: number | null;
  goal: string;
  weeklyMin?: number | null;
  equipment: string[];
  preferences?: unknown;
  onboarded: boolean;
  onboardedAt?: string | null;
}

export interface WellnessRequest {
  sleepHours: number;
  subjectiveFatigue: number;
}

export interface WellnessLog {
  id: string;
  athleteId: string;
  logDate: string;
  sleepHours: number;
  subjectiveFatigue: number;
}

export interface AthleticState {
  id: string;
  athleteId: string;
  asOf: string;
  readiness: number;
  recovery: number;
  fatigueByMuscle: string;
  sleepHours: number;
  source: string;
  calcVersion: string;
}

export interface AdviceSlot {
  slot: string;
  pick: string;
  alternatives: string[];
  sets: number;
  reps: number;
  loadKg: number | null;
  rpeCap: number | null;
}

export interface AdvicePayload {
  schemaVersion: number;
  kind: AdviceKind | string;
  title?: string;
  rationale?: string;
  slots: AdviceSlot[];
}

export interface AdviceEvent {
  adviceId: string;
  kind: string;
  payload: AdvicePayload;
}

export interface DecideResult {
  adviceId: string;
  status: string;
  sessionId: string;
  idempotent: boolean;
}

export interface SessionLog {
  id: string;
  athleteId: string;
  adviceId?: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED' | string;
  startedAt?: string;
  endedAt?: string | null;
  perceivedExertion?: number | null;
  notes?: string | null;
}

export interface SetLog {
  id: string;
  sessionId: string;
  exerciseCode: string;
  muscleGroup?: string;
  setIndex: number;
  reps?: number | null;
  loadKg?: number | null;
  rpe?: number | null;
  completed: boolean;
}

export interface CurrentSession {
  session: SessionLog;
  sets: SetLog[];
}

export interface LlmUsageDaily {
  id: string;
  athleteId: string;
  usageDate: string;
  keySource: string;
  callCount: number;
  failCount: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
}

export type CoachUiStatus = 'idle' | 'running' | 'tooling' | 'awaiting_confirm' | 'error';

export type AthleticStateView = AthleticState;
export type AdviceBody = AdvicePayload;
export type TokenPayload = TokenVO;
export type DailyUsage = LlmUsageDaily;
