import { muscleLabel } from '@/src/catalog';
import { colors } from '@/src/theme';
import type { AthleticState } from '@/src/types';
import { Card, Muted } from '@/src/ui';
import { StyleSheet, Text, View } from 'react-native';

function parseFatigue(raw: string): Record<string, number> {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const out: Record<string, number> = {};
    for (const [key, value] of Object.entries(parsed)) {
      const n = Number(value);
      if (Number.isFinite(n) && n > 0) {
        out[key] = n;
      }
    }
    return out;
  } catch {
    return {};
  }
}

function fatigueLevel(values: Record<string, number>): string {
  const max = Math.max(0, ...Object.values(values));
  if (max >= 7) {
    return '高';
  }
  if (max >= 4) {
    return '中';
  }
  return '低';
}

export function ReadinessCard({ state }: { state: AthleticState | null }) {
  if (!state) {
    return (
      <Card>
        <Muted>还没有准备度快照。先填今日睡眠和疲劳。</Muted>
      </Card>
    );
  }
  const fatigue = parseFatigue(state.fatigueByMuscle);
  const top = Object.entries(fatigue)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 2)
    .map(([k]) => muscleLabel(k))
    .join(' / ');
  return (
    <Card>
      <View style={styles.row}>
        <Text style={styles.score}>{state.readiness}</Text>
        <Text style={styles.over}>/ 100</Text>
      </View>
      <View style={styles.grid}>
        <View style={styles.cell}>
          <Muted>睡眠</Muted>
          <Text style={styles.metric}>{state.sleepHours}h</Text>
        </View>
        <View style={styles.cell}>
          <Muted>疲劳 {top || '—'}</Muted>
          <Text style={styles.metric}>{fatigueLevel(fatigue)}</Text>
        </View>
        <View style={styles.cell}>
          <Muted>恢复</Muted>
          <Text style={styles.metric}>{state.recovery}</Text>
        </View>
      </View>
      <Muted>
        规则计算 {state.calcVersion} · {state.source === 'RULE' ? '无设备数据' : state.source}
      </Muted>
    </Card>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 6,
  },
  score: {
    color: colors.accent,
    fontSize: 40,
    fontWeight: '800',
  },
  over: {
    color: colors.muted,
    fontSize: 18,
    marginBottom: 6,
  },
  grid: {
    flexDirection: 'row',
    gap: 12,
  },
  cell: {
    flex: 1,
    gap: 4,
  },
  metric: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '600',
  },
});

export const ReadinessCardView = ReadinessCard;
