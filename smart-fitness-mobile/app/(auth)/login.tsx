import { login, register } from '@/src/api/auth';
import { useSessionStore } from '@/src/auth/session-store';
import { messageOf } from '@/src/format';
import { colors, space } from '@/src/theme';
import { Banner, Field, PrimaryButton, Screen, Title } from '@/src/ui';
import { Link, useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

export default function LoginScreen() {
  const router = useRouter();
  const applyTokens = useSessionStore((s) => s.applyTokens);
  const [account, setAccount] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(mode: 'login' | 'register') {
    setError(null);
    setLoading(true);
    try {
      const tokens =
        mode === 'register'
          ? await register(account.trim(), password)
          : await login(account.trim(), password);
      await applyTokens(tokens);
      router.replace(tokens.onboarded ? '/(tabs)/coach' : '/onboarding');
    } catch (err) {
      setError(messageOf(err, '登录失败'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.hero}>
            <Text style={styles.kicker}>SMART FITNESS</Text>
            <Title>先登录，再让教练认识你</Title>
          </View>
          {error ? <Banner text={error} /> : null}
          <Field label="邮箱 / 手机" value={account} onChangeText={setAccount} autoComplete="email" />
          <Field
            label="密码"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="password"
          />
          <PrimaryButton label="登录" onPress={() => submit('login')} loading={loading} disabled={!account || !password} />
          <Pressable onPress={() => submit('register')} disabled={loading || !account || !password}>
            <Text style={styles.link}>没有账号？用当前邮箱注册</Text>
          </Pressable>
          <Link href="/(auth)/register" style={styles.alt}>
            去完整注册页
          </Link>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { gap: space.md, paddingTop: 48 },
  hero: { gap: 8, marginBottom: 8 },
  kicker: { color: colors.accent, fontWeight: '800', letterSpacing: 1 },
  link: { color: colors.accent, textAlign: 'center', marginTop: 8 },
  alt: { color: colors.muted, textAlign: 'center' },
});
