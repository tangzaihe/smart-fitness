import { register } from '@/src/api/auth';
import { useSessionStore } from '@/src/auth/session-store';
import { messageOf } from '@/src/format';
import { space } from '@/src/theme';
import { Banner, Field, Muted, PrimaryButton, Screen, Title } from '@/src/ui';
import { Link, useRouter } from 'expo-router';
import { useState } from 'react';
import { StyleSheet } from 'react-native';

export default function RegisterScreen() {
  const router = useRouter();
  const applyTokens = useSessionStore((s) => s.applyTokens);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit() {
    setError(null);
    setLoading(true);
    try {
      const tokens = await register(email.trim(), password);
      await applyTokens(tokens);
      router.replace('/onboarding');
    } catch (err) {
      setError(messageOf(err, '注册失败'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <Title>注册</Title>
      <Muted>密码至少 8 位，需包含字母和数字。</Muted>
      {error ? <Banner text={error} /> : null}
      <Field label="邮箱" value={email} onChangeText={setEmail} autoComplete="email" keyboardType="email-address" />
      <Field label="密码" value={password} onChangeText={setPassword} secureTextEntry />
      <PrimaryButton label="创建账号" onPress={submit} loading={loading} disabled={!email || password.length < 8} />
      <Link href="/(auth)/login" style={styles.link}>
        已有账号，去登录
      </Link>
    </Screen>
  );
}

const styles = StyleSheet.create({
  link: { marginTop: space.md, color: '#8B9BB4' },
});
