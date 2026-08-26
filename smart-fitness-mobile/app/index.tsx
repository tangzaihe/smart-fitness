import { Redirect } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';
import { useSessionStore } from '@/src/auth/session-store';
import { colors } from '@/src/theme';

export default function Index() {
  const ready = useSessionStore((s) => s.ready);
  const accessToken = useSessionStore((s) => s.accessToken);
  const onboarded = useSessionStore((s) => s.onboarded);

  if (!ready) {
    return (
      <View style={{ flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }
  if (!accessToken) {
    return <Redirect href="/(auth)/login" />;
  }
  if (!onboarded) {
    return <Redirect href="/onboarding" />;
  }
  return <Redirect href="/(tabs)/coach" />;
}
