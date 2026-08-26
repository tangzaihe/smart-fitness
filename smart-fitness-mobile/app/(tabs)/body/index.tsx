import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { getReadiness, putWellnessToday } from '@/src/api/athlete';
import { muscleLabel } from '@/src/catalog';
import { ReadinessCard } from '@/src/components/ReadinessCard';
import { messageOf } from '@/src/format';
import { colors, space } from '@/src/theme';
import { Banner, Card, Field, Muted, PrimaryButton, Screen, Title } from '@/src/ui';

function parseFatigue(raw: string): Record<string, number> {
  try {
    return JSON.parse(raw) as Record<string, number>;
  } catch {
    return {};
  }
}

export default function BodyScreen() {
  const queryClient = useQueryClient();
  const [sleepHours, setSleepHours] = useState('7.5');
  const [fatigue, setFatigue] = useState('4');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const readinessQuery = useQuery({
    queryKey: ['readiness'],
    queryFn: getReadiness,
  });

  const saveMut = useMutation({
    mutationFn: () =>
      putWellnessToday({
        sleepHours: Number(sleepHours),
        subjectiveFatigue: Number(fatigue),
      }),
    onSuccess: async () => {
      setError(null);
      setMessage('今日状态已保存，准备度已重算。');
      await queryClient.invalidateQueries({ queryKey: ['readiness'] });
    },
    onError: (err) => {
      setMessage(null);
      setError(messageOf(err, '保存失败'));
    },
  });

  const fatigueMap = parseFatigue(readinessQuery.data?.fatigueByMuscle ?? '{}');

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <Title>身体</Title>
        {error ? <Banner text={error} /> : null}
        {message ? <Banner text={message} tone="warning" /> : null}
        <ReadinessCard state={readinessQuery.data ?? null} />
        <Card>
          <Title>肌群疲劳</Title>
          {Object.keys(fatigueMap).length === 0 ? <Muted>暂无疲劳数据</Muted> : null}
          {Object.entries(fatigueMap)
            .sort((a, b) => b[1] - a[1])
            .map(([muscle, value]) => (
              <View key={muscle} style={styles.row}>
                <Text style={styles.muscle}>{muscleLabel(muscle)}</Text>
                <Text style={styles.value}>{value}</Text>
              </View>
            ))}
        </Card>
        <Card>
          <Title>今日 wellness</Title>
          <Field label="睡眠小时" value={sleepHours} onChangeText={setSleepHours} keyboardType="decimal-pad" />
          <Field label="主观疲劳 1-10" value={fatigue} onChangeText={setFatigue} keyboardType="number-pad" />
          <PrimaryButton label="保存并重算准备度" onPress={() => saveMut.mutate()} loading={saveMut.isPending} />
        </Card>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: space.md, paddingBottom: 40 },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  muscle: { color: colors.text },
  value: { color: colors.accent, fontWeight: '700' },
});
