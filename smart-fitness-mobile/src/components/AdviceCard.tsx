import { exerciseName } from '@/src/catalog';
import { colors, space } from '@/src/theme';
import type { AdvicePayload } from '@/src/types';
import { Card, GhostButton, Muted, PrimaryButton, Title } from '@/src/ui';
import { StyleSheet, Text, View } from 'react-native';

export function AdviceCard({
  kind,
  payload,
  busy,
  onAccept,
  onRest,
  onReject,
}: {
  kind: string;
  payload: AdvicePayload;
  busy?: boolean;
  onAccept: () => void;
  onRest: () => void;
  onReject: () => void;
}) {
  const rest = kind === 'REST' || payload.kind === 'REST';
  return (
    <Card style={styles.card}>
      <Text style={styles.kicker}>{rest ? '建议 · 休息' : '建议 · 训练'}</Text>
      <Title>{payload.title || (rest ? '今天休息' : '今日训练')}</Title>
      {payload.rationale ? <Muted>{payload.rationale}</Muted> : null}
      {payload.slots.map((slot) => (
        <View key={`${slot.slot}-${slot.pick}`} style={styles.slot}>
          <Text style={styles.pick}>
            {exerciseName(slot.pick)}  {slot.sets}×{slot.reps}
            {slot.loadKg != null ? ` @ ${slot.loadKg}kg` : ''}
            {slot.rpeCap != null ? `  RPE ≤ ${slot.rpeCap}` : ''}
          </Text>
          {slot.alternatives.length > 0 ? (
            <Muted>备选：{slot.alternatives.map(exerciseName).join('、')}</Muted>
          ) : null}
        </View>
      ))}
      <View style={styles.actions}>
        {rest ? null : <PrimaryButton label="采纳并开始" onPress={onAccept} loading={busy} />}
        <GhostButton label={rest ? '确认休息' : '只要休息'} onPress={onRest} disabled={busy} />
        <GhostButton label="忽略" onPress={onReject} disabled={busy} />
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: {
    borderLeftWidth: 3,
    borderLeftColor: colors.accent,
  },
  kicker: {
    color: colors.accent,
    fontWeight: '700',
  },
  slot: {
    gap: 4,
    paddingTop: 4,
  },
  pick: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '600',
  },
  actions: {
    gap: space.sm,
    marginTop: space.sm,
  },
});

export const AdviceCardView = AdviceCard;
