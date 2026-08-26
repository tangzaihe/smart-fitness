import type { Equipment } from '@/src/types';

export const GOAL_OPTIONS: { value: 'HYPERTROPHY' | 'FAT_LOSS' | 'STRENGTH' | 'REHAB'; label: string }[] = [
  { value: 'HYPERTROPHY', label: '增肌' },
  { value: 'FAT_LOSS', label: '减脂' },
  { value: 'STRENGTH', label: '力量' },
  { value: 'REHAB', label: '康复' },
];

export const EQUIPMENT_OPTIONS: { value: Equipment; label: string }[] = [
  { value: 'BARBELL', label: '杠铃' },
  { value: 'DUMBBELL', label: '哑铃' },
  { value: 'KETTLEBELL', label: '壶铃' },
  { value: 'SMITH', label: '史密斯' },
  { value: 'LEG_PRESS', label: '腿举' },
  { value: 'CABLE', label: '绳索' },
  { value: 'MACHINE', label: '固定器械' },
  { value: 'BODYWEIGHT', label: '自重' },
  { value: 'BAND', label: '弹力带' },
  { value: 'PULLUP_BAR', label: '引体杠' },
];

export const BODY_PART_OPTIONS: { value: string; label: string }[] = [
  { value: 'SHOULDER', label: '肩' },
  { value: 'ELBOW', label: '肘' },
  { value: 'WRIST', label: '腕' },
  { value: 'NECK', label: '颈' },
  { value: 'LOWER_BACK', label: '下背' },
  { value: 'KNEE', label: '膝' },
  { value: 'HIP', label: '髋' },
  { value: 'ANKLE', label: '踝' },
  { value: 'OTHER', label: '其他' },
];

export const MUSCLE_LABELS: Record<string, string> = {
  CHEST: '胸',
  BACK: '背',
  SHOULDER: '肩',
  BICEP: '肱二头',
  TRICEP: '肱三头',
  QUAD: '股四头',
  HAMSTRING: '腘绳',
  GLUTE: '臀',
  CALF: '小腿',
  CORE: '核心',
  FOREARM: '前臂',
};

export const EXERCISE_NAMES: Record<string, string> = {
  bb_back_squat: '杠铃深蹲',
  goblet_squat: '高脚杯深蹲',
  smith_squat: '史密斯深蹲',
  leg_press_45: '45度腿举',
  walking_lunge: '步行弓步',
  rd_deadlift: '罗马尼亚硬拉',
  db_rdl: '哑铃RDL',
  hip_thrust: '髋推',
  lying_leg_curl: '俯卧腿弯举',
  leg_extension: '腿屈伸',
  bb_bench: '杠铃卧推',
  db_bench: '哑铃卧推',
  push_up: '俯卧撑',
  machine_chest_press: '器械胸推',
  ohp_bb: '杠铃肩上推',
  ohp_db: '哑铃肩推',
  lateral_raise: '侧平举',
  pull_up: '引体向上',
  lat_pulldown: '高位下拉',
  bb_row: '杠铃划船',
  seated_cable_row: '坐姿划船',
  db_row: '单臂哑铃划船',
  bb_curl: '杠铃弯举',
  db_curl: '哑铃弯举',
  tricep_pushdown: '绳索下压',
  plank: '平板支撑',
  cable_crunch: '绳索卷腹',
  calf_raise: '提踵',
  farmer_carry: '农夫行走',
  face_pull: '面拉',
};

export function exerciseName(code: string | null | undefined): string {
  if (!code) {
    return '未知动作';
  }
  return EXERCISE_NAMES[code] ?? code;
}

export function muscleLabel(code: string): string {
  return MUSCLE_LABELS[code] ?? code;
}

export function goalLabel(goal: string | null | undefined): string {
  return GOAL_OPTIONS.find((item) => item.value === goal)?.label ?? goal ?? '未设置';
}

export const GOAL_ITEMS = GOAL_OPTIONS;
export const EQUIPMENT_ITEMS = EQUIPMENT_OPTIONS;
export const BODY_PART_ITEMS = BODY_PART_OPTIONS;
export const exerciseLabel = exerciseName;
export const muscleName = muscleLabel;
export const goalName = goalLabel;
