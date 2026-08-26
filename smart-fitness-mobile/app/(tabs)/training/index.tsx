import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { abandonSession, completeSession, getCurrentSession, patchSet } from '@/src/api/session';
import { exerciseName, muscleLabel } from '@/src/catalog';
import { messageOf } from '@/src/format';
import { colors, space } from '@/src/theme';
import type { SetLog } from '@/src/types';
import { Banner, Card, Field, GhostButton, Muted, PrimaryButton, Screen, Title } from '@/src/ui';

function groupSets(sets: SetLog[]): { code: string; items: SetLog[] }[] {
  const map = new Map<string, SetLog[]>();
  for (const set of sets) {
    const list = map.get(set.exerciseCode) ?? [];
    list.push(set);
    map.set(set.exerciseCode, list);
  }
  return [...map.entries()].map(([code, items]) => ({ code, items }));
}

export default function TrainingScreen() {
  const queryClient = useQueryClient();
  const [reps, setReps] = useState('');
  const [loadKg, setLoadKg] = useState('');
  const [rpe, setRpe] = useState('');
  const [error, setError] = useState<string | null>(null);

  const currentQuery = useQuery({
    queryKey: ['session-current'],
    queryFn: getCurrentSession,
  });

  const current = currentQuery.data;
  const groups = useMemo(() => groupSets(current?.sets ?? []), [current]);
  const active = current?.sets.find((set) => !set.completed) ?? null;

  const patchMut = useMutation({
    mutationFn: () => {
      if (!current || !active) {
        throw new Error('没有进行中的组');
      }
      return patchSet(current.session.id, active.id, {
        reps: Number(reps || active.reps || 0),
        loadKg: Number(loadKg || active.loadKg || 0),
        rpe: rpe ? Number(rpe) : undefined,
        completed: true,
      });
    },
    onSuccess: async () => {
      setError(null);
      setReps('');
      setLoadKg('');
      setRpe('');
      await queryClient.invalidateQueries({ queryKey: ['session-current'] });
    },
    onError: (err) => setError(messageOf(err, '记组失败')),
  });

  const finishMut = useMutation({
    mutationFn: () => completeSession(current!.session.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['session-current'] });
      await queryClient.invalidateQueries({ queryKey: ['readiness'] });
    },
    onError: (err) => setError(messageOf(err, '结束失败')),
  });

  const dropMut = useMutation({
    mutationFn: () => abandonSession(current!.session.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['session-current'] });
    },
    onError: (err) => setError(messageOf(err, '放弃失败')),
  });

  if (currentQuery.isLoading) {
    return (
      <Screen>
        <Muted>加载当前课次…</Muted>
      </Screen>
    );
  }

  if (!current) {
    return (
      <Screen>
        <Title>训练</Title>
        <Muted>还没有进行中的课。先去教练页确认建议。</Muted>
        {currentQuery.error ? <Banner text={messageOf(currentQuery.error)} /> : null}
      </Screen>
    );
  }

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <Title>训练中</Title>
        <Card>
          <Muted>教练建议只读。重量由你填写，不经过 LLM。</Muted>
        </Card>
        {error ? <Banner text={error} /> : null}
        {groups.map((group) => (
          <Card key={group.code}>
            <Text style={styles.ex}>{exerciseName(group.code)}</Text>
            <Muted>{muscleLabel(group.items[0]?.muscleGroup ?? '')}</Muted>
            {group.items.map((set) => {
              const isActive = active?.id === set.id;
              return (
                <View key={set.id} style={[styles.setRow, isActive && styles.activeRow]}>
                  <Text style={styles.setIdx}>组 {set.setIndex}</Text>
                  <Text style={styles.setVal}>
                    {set.completed
                      ? `${set.reps ?? '-'} × ${set.loadKg ?? '-'}kg  RPE ${set.rpe ?? '-'}`
                      : isActive
                        ? '进行中'
                        : `${set.reps ?? '-'} × ${set.loadKg ?? '-'}kg`}
                  </Text>
                  <Text style={styles.setState}>{set.completed ? '完成' : isActive ? '当前' : '待做'}</Text>
                </View>
              );
            })}
          </Card>
        ))}
        {active ? (
          <Card>
            <Title>完成本组</Title>
            <Field label="次数" value={reps} onChangeText={setReps} keyboardType="number-pad" placeholder={String(active.reps ?? '')} />
            <Field label="重量 kg" value={loadKg} onChangeText={setLoadKg} keyboardType="decimal-pad" placeholder={String(active.loadKg ?? 0)} />
            <Field label="RPE" value={rpe} onChangeText={setRpe} keyboardType="decimal-pad" />
            <PrimaryButton label="完成本组" onPress={() => patchMut.mutate()} loading={patchMut.isPending} />
          </Card>
        ) : (
          <Muted>全部组已记完，可以结束本课。</Muted>
        )}
        <PrimaryButton label="结束本课" onPress={() => finishMut.mutate()} loading={finishMut.isPending} />
        <GhostButton label="放弃本课" onPress={() => dropMut.mutate()} disabled={dropMut.isPending} />
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: space.md, paddingBottom: 40 },
  ex: { color: colors.text, fontSize: 18, fontWeight: '700' },
  setRow: { flexDirection: 'row', justifyContent: 'space-between', gap: 8, paddingVertical: 6 },
  activeRow: { backgroundColor: colors.accentDim, borderRadius: 8, paddingHorizontal: 8 },
  setIdx: { color: colors.accent, width: 48, fontWeight: '700' },
  setVal: { color: colors.text, flex: 1 },
  setState: { color: colors.muted, width: 40, textAlign: 'right' },
});
