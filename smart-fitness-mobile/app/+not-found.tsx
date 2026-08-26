import { Link, Stack } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { colors } from '@/src/theme';

export default function NotFoundScreen() {
  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: '未找到' }} />
      <Text style={styles.title}>页面不存在</Text>
      <Link href="/" style={styles.link}>
        <Text style={styles.linkText}>回到首页</Text>
      </Link>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.bg, padding: 20 },
  title: { color: colors.text, fontSize: 20, fontWeight: '700' },
  link: { marginTop: 16 },
  linkText: { color: colors.accent },
});
