import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { getMe } from '@/src/api/athlete';
import { logout } from '@/src/api/auth';
import { getUsage } from '@/src/api/usage';
import { useSessionStore } from '@/src/auth/session-store';
import { EQUIPMENT_OPTIONS, goalLabel } from '@/src/catalog';
import { messageOf } from '@/src/format';
import { colors, space } from '@/src/theme';
import { Banner, Card, GhostButton, Muted, Screen, Title } from '@/src/ui';

export default function ProfileScreen() {
  const router = useRouter();
  const refreshToken = useSessionStore((s) => s.refreshToken);
  const quotaBlocked = useSessionStore((s) => s.quotaBlocked);
  const clear = useSessionStore((s) => s.clear);

  const meQuery = useQuery({ queryKey: ['me'], queryFn: getMe });
  const usageQuery = useQuery({ queryKey: ['usage'], queryFn: getUsage });
  const latest = usageQuery.data?.[0];

  async function onLogout() {
    try {
      await logout(refreshToken);
    } catch {
      // still clear local session
    }
    await clear();
    router.replace('/(auth)/login');
  }

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content}>
        <Title>我的</Title>
        {meQuery.error ? <Banner text={messageOf(meQuery.error)} /> : null}
        <Card>
          <Text style={styles.name}>{meQuery.data?.displayName ?? '运动员'}</Text>
          <Muted>目标 {goalLabel(meQuery.data?.goal)}</Muted>
          <Muted>每周 {meQuery.data?.weeklyMin ?? 0} 分钟</Muted>
          <Muted>
            器材{' '}
            {(meQuery.data?.equipment ?? [])
              .map((code) => EQUIPMENT_OPTIONS.find((item) => item.value === code)?.label ?? code)
              .join(' / ') || '未设置'}
          </Muted>
        </Card>
        <Card>
          <Title>近 7 日用量</Title>
          {quotaBlocked ? <Banner text="今日系统额度已用尽" tone="warning" /> : null}
          <Text style={styles.usage}>{latest?.totalTokens ?? 0} tokens</Text>
          <Muted>按日汇总，系统 Key 会计入配额。</Muted>
          {(usageQuery.data ?? []).map((row) => (
            <View key={`${row.usageDate}-${row.keySource}`} style={styles.row}>
              <Text style={styles.date}>{row.usageDate}</Text>
              <Text style={styles.tokens}>{row.totalTokens}</Text>
            </View>
          ))}
        </Card>
        <GhostButton label="退出登录" onPress={() => void onLogout()} />
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: space.md, paddingBottom: 40 },
  name: { color: colors.text, fontSize: 22, fontWeight: '700' },
  usage: { color: colors.accent, fontSize: 28, fontWeight: '800' },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  date: { color: colors.muted },
  tokens: { color: colors.text, fontWeight: '600' },
});
