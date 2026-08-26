import { colors, space } from '@/src/theme';
import { Card, Muted } from '@/src/ui';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

const STEPS = ['正在看你的恢复', '正在检索动作', '正在生成处方'] as const;

export function CoachRunProgress({
  tools,
  isGenerating,
}: {
  tools: string[];
  isGenerating: boolean;
}) {
  const hasRetrieve = tools.some((tool) => tool.includes('检索'));
  const activeStep = isGenerating ? 2 : hasRetrieve ? 1 : 0;

  return (
    <Card style={styles.card}>
      <Text style={styles.heading}>教练分析中</Text>
      <View style={styles.steps}>
        {STEPS.map((step, index) => {
          const done = index < activeStep;
          const current = index === activeStep;
          return (
            <View key={step} style={styles.stepRow}>
              <View style={[styles.dot, done && styles.dotDone, current && styles.dotCurrent]}>
                {current ? (
                  <ActivityIndicator size="small" color={colors.accent} />
                ) : (
                  <Text style={[styles.dotText, done && styles.dotTextDone]}>{done ? '✓' : index + 1}</Text>
                )}
              </View>
              <Text style={[styles.stepLabel, current && styles.stepLabelCurrent, done && styles.stepLabelDone]}>
                {step}
              </Text>
            </View>
          );
        })}
      </View>
      {isGenerating ? <Muted>根据准备度和动作库组合今日训练，请稍候…</Muted> : null}
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { gap: space.sm },
  heading: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '700',
  },
  steps: { gap: 10 },
  stepRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  dot: {
    width: 28,
    height: 28,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.cardAlt,
  },
  dotCurrent: {
    borderColor: colors.accent,
    backgroundColor: colors.accentDim,
  },
  dotDone: {
    borderColor: colors.success,
    backgroundColor: '#14301F',
  },
  dotText: {
    color: colors.muted,
    fontSize: 12,
    fontWeight: '700',
  },
  dotTextDone: {
    color: colors.success,
  },
  stepLabel: {
    color: colors.muted,
    fontSize: 15,
  },
  stepLabelCurrent: {
    color: colors.text,
    fontWeight: '600',
  },
  stepLabelDone: {
    color: colors.text,
  },
});
