import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { getReadiness } from '@/src/api/athlete';
import { decideAdvice } from '@/src/api/coach';
import { useSessionStore } from '@/src/auth/session-store';
import { AdviceCard } from '@/src/components/AdviceCard';
import { CoachRunProgress } from '@/src/components/CoachRunProgress';
import { ReadinessCard } from '@/src/components/ReadinessCard';
import { useCoachRun } from '@/src/coach/useCoachRun';
import { messageOf } from '@/src/format';
import { colors, space } from '@/src/theme';
import type { DecideAction } from '@/src/types';
import { Banner, GhostButton, Muted, PrimaryButton, Screen, Title } from '@/src/ui';

export default function CoachScreen() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const quotaBlocked = useSessionStore((s) => s.quotaBlocked);
  const onboarded = useSessionStore((s) => s.onboarded);
  const run = useCoachRun();
  const [actionError, setActionError] = useState<string | null>(null);

  const readinessQuery = useQuery({
    queryKey: ['readiness'],
    queryFn: getReadiness,
  });

  const startRun = run.start;
  const startedRef = run.started;

  useEffect(() => {
    if (!onboarded || quotaBlocked || startedRef.current) {
      return;
    }
    void startRun('OBSERVE');
  }, [onboarded, quotaBlocked, startRun, startedRef]);

  const runBusy = run.status === 'running' || run.status === 'tooling' || run.isGenerating;

  const decideMut = useMutation({
    mutationFn: (action: DecideAction) => {
      if (!run.adviceId) {
        throw new Error('还没有建议');
      }
      return decideAdvice(run.adviceId, action);
    },
    onSuccess: async (result) => {
      setActionError(null);
      await queryClient.invalidateQueries({ queryKey: ['session-current'] });
      await queryClient.invalidateQueries({ queryKey: ['readiness'] });
      if (result.sessionId) {
        router.push('/(tabs)/training');
      }
    },
    onError: (error) => setActionError(messageOf(error, '确认失败')),
  });

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Title>今日教练</Title>
          {readinessQuery.data ? (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>准备度 {readinessQuery.data.readiness}</Text>
            </View>
          ) : null}
        </View>
        {quotaBlocked || run.errorCode === 3005 ? (
          <Banner text="今日系统额度已用尽，教练输入已禁用。" tone="warning" />
        ) : null}
        {actionError ? <Banner text={actionError} /> : null}
        {readinessQuery.error ? <Banner text={messageOf(readinessQuery.error, '准备度加载失败')} /> : null}
        {run.errorMessage && run.errorCode !== 3005 ? <Banner text={run.errorMessage} /> : null}
        <ReadinessCard state={readinessQuery.data ?? null} />
        {(run.status === 'running' || run.status === 'tooling' || run.isGenerating) && !run.payload ? (
          <CoachRunProgress tools={run.tools} isGenerating={run.isGenerating} />
        ) : null}
        {run.payload && run.adviceId ? (
          <AdviceCard
            kind={run.kind ?? run.payload.kind}
            payload={run.payload}
            busy={decideMut.isPending}
            onAccept={() => decideMut.mutate('ACCEPT')}
            onRest={() => decideMut.mutate('REST')}
            onReject={() => decideMut.mutate('REJECT')}
          />
        ) : null}
        <View style={styles.composer}>
          <Muted>P0 暂不支持自由输入。用触发器再开一轮。</Muted>
          <PrimaryButton
            label={runBusy ? '回合进行中' : '再观察一次'}
            onPress={() => {
              setActionError(null);
              void run.start('OBSERVE');
            }}
            disabled={quotaBlocked || runBusy}
          />
          <GhostButton
            label="手动触发"
            onPress={() => void run.start('MANUAL')}
            disabled={quotaBlocked || runBusy}
          />
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: space.md, paddingBottom: 40 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  badge: {
    backgroundColor: colors.accentDim,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  badgeText: { color: colors.text, fontWeight: '700' },
  composer: { gap: space.sm, marginTop: space.sm },
});
