import { putWellnessToday, updateMe } from '@/src/api/athlete';
import { useSessionStore } from '@/src/auth/session-store';
import { BODY_PART_OPTIONS, EQUIPMENT_OPTIONS, GOAL_OPTIONS } from '@/src/catalog';
import { messageOf } from '@/src/format';
import { colors, space } from '@/src/theme';
import type { Equipment, Goal } from '@/src/types';
import { Banner, Chip, Field, Muted, PrimaryButton, Screen, Title } from '@/src/ui';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

export default function OnboardingScreen() {
  const router = useRouter();
  const setOnboarded = useSessionStore((s) => s.setOnboarded);
  const [displayName, setDisplayName] = useState('');
  const [goal, setGoal] = useState<Goal>('HYPERTROPHY');
  const [weeklyMin, setWeeklyMin] = useState('180');
  const [equipment, setEquipment] = useState<Equipment[]>(['BARBELL', 'DUMBBELL', 'BODYWEIGHT']);
  const [liked, setLiked] = useState('free_weight');
  const [painPart, setPainPart] = useState<string | null>(null);
  const [painNotes, setPainNotes] = useState('');
  const [sleepHours, setSleepHours] = useState('7.5');
  const [fatigue, setFatigue] = useState('4');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function toggleEquipment(value: Equipment) {
    setEquipment((current) =>
      current.includes(value) ? current.filter((item) => item !== value) : [...current, value],
    );
  }

  async function submit() {
    setError(null);
    if (!displayName.trim()) {
      setError('请填写显示名');
      return;
    }
    if (equipment.length === 0) {
      setError('至少选择一种器材');
      return;
    }
    setLoading(true);
    try {
      await updateMe({
        displayName: displayName.trim(),
        goal,
        weeklyMin: Number(weeklyMin) || 180,
        equipment,
        preferences: {
          liked: liked.trim() ? liked.split(/[,，\s]+/).filter(Boolean) : [],
          disliked: [],
          never: [],
        },
        constraints: painPart
          ? [
              {
                type: 'PAIN',
                bodyPart: painPart,
                severity: 3,
                notes: painNotes.trim() || undefined,
              },
            ]
          : [],
      });
      await putWellnessToday({
        sleepHours: Number(sleepHours),
        subjectiveFatigue: Number(fatigue),
      });
      await setOnboarded(true);
      router.replace('/(tabs)/coach');
    } catch (err) {
      setError(messageOf(err, '建档失败'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <Title>先让教练认识你</Title>
        <Muted>目标、器材和伤痛会写入约束，下一回合 Observe 会读到。</Muted>
        {error ? <Banner text={error} /> : null}
        <Field label="怎么称呼你" value={displayName} onChangeText={setDisplayName} autoCapitalize="words" />
        <Text style={styles.label}>目标</Text>
        <View style={styles.wrap}>
          {GOAL_OPTIONS.map((item) => (
            <Chip key={item.value} label={item.label} selected={goal === item.value} onPress={() => setGoal(item.value)} />
          ))}
        </View>
        <Field label="每周可练（分钟）" value={weeklyMin} onChangeText={setWeeklyMin} keyboardType="number-pad" />
        <Text style={styles.label}>器材</Text>
        <View style={styles.wrap}>
          {EQUIPMENT_OPTIONS.map((item) => (
            <Chip
              key={item.value}
              label={item.label}
              selected={equipment.includes(item.value)}
              onPress={() => toggleEquipment(item.value)}
            />
          ))}
        </View>
        <Field label="偏好标签（逗号分隔，可填 free_weight）" value={liked} onChangeText={setLiked} />
        <Text style={styles.label}>伤痛部位（可选）</Text>
        <View style={styles.wrap}>
          {BODY_PART_OPTIONS.map((item) => (
            <Chip
              key={item.value}
              label={item.label}
              selected={painPart === item.value}
              onPress={() => setPainPart((cur) => (cur === item.value ? null : item.value))}
            />
          ))}
        </View>
        {painPart ? <Field label="伤痛说明" value={painNotes} onChangeText={setPainNotes} /> : null}
        <Title>今日状态</Title>
        <Field label="睡眠小时" value={sleepHours} onChangeText={setSleepHours} keyboardType="decimal-pad" />
        <Field label="主观疲劳 1-10" value={fatigue} onChangeText={setFatigue} keyboardType="number-pad" />
        <PrimaryButton label="开始第一次教练回合" onPress={submit} loading={loading} />
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: space.md, paddingBottom: 48 },
  label: { color: colors.muted, marginBottom: -4 },
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
});
