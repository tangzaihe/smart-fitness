import { colors, radius, space } from '@/src/theme';
import type { ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
  type ViewStyle,
} from 'react-native';

export function Screen({ children, style }: { children: ReactNode; style?: ViewStyle }) {
  return <View style={[styles.screen, style]}>{children}</View>;
}

export function Card({ children, style }: { children: ReactNode; style?: ViewStyle }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function Title({ children }: { children: ReactNode }) {
  return <Text style={styles.title}>{children}</Text>;
}

export function Muted({ children }: { children: ReactNode }) {
  return <Text style={styles.muted}>{children}</Text>;
}

export function Field({
  label,
  ...props
}: TextInputProps & { label: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        placeholderTextColor={colors.muted}
        style={styles.input}
        autoCapitalize="none"
        {...props}
      />
    </View>
  );
}

export function PrimaryButton({
  label,
  onPress,
  disabled,
  loading,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  loading?: boolean;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={[styles.primary, (disabled || loading) && styles.disabled]}
    >
      {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryText}>{label}</Text>}
    </Pressable>
  );
}

export function GhostButton({
  label,
  onPress,
  disabled,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable onPress={onPress} disabled={disabled} style={[styles.ghost, disabled && styles.disabled]}>
      <Text style={styles.ghostText}>{label}</Text>
    </Pressable>
  );
}

export function Chip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected?: boolean;
  onPress?: () => void;
}) {
  return (
    <Pressable onPress={onPress} style={[styles.chip, selected && styles.chipOn]}>
      <Text style={[styles.chipText, selected && styles.chipTextOn]}>{label}</Text>
    </Pressable>
  );
}

export function Banner({ text, tone = 'danger' }: { text: string; tone?: 'danger' | 'warning' }) {
  return (
    <View style={[styles.banner, tone === 'warning' && styles.bannerWarn]}>
      <Text style={styles.bannerText}>{text}</Text>
    </View>
  );
}

export const PrimaryBtn = PrimaryButton;
export const GhostBtn = GhostButton;
export const AlertBanner = Banner;

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.bg,
    padding: space.md,
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: radius.lg,
    padding: space.md,
    gap: space.sm,
  },
  title: {
    color: colors.text,
    fontSize: 22,
    fontWeight: '700',
  },
  muted: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 20,
  },
  field: {
    gap: 6,
  },
  label: {
    color: colors.muted,
    fontSize: 13,
  },
  input: {
    backgroundColor: colors.cardAlt,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: radius.md,
    color: colors.text,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  primary: {
    backgroundColor: colors.accent,
    borderRadius: radius.md,
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryText: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 16,
  },
  ghost: {
    borderColor: colors.accent,
    borderWidth: 1,
    borderRadius: radius.md,
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
  },
  ghostText: {
    color: colors.accent,
    fontWeight: '600',
  },
  disabled: {
    opacity: 0.5,
  },
  chip: {
    backgroundColor: colors.chip,
    borderRadius: radius.pill,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderWidth: 1,
    borderColor: colors.border,
  },
  chipOn: {
    backgroundColor: colors.accentDim,
    borderColor: colors.accent,
  },
  chipText: {
    color: colors.muted,
    fontSize: 13,
  },
  chipTextOn: {
    color: colors.text,
    fontWeight: '600',
  },
  banner: {
    backgroundColor: '#3F1D27',
    borderRadius: radius.md,
    padding: space.sm,
  },
  bannerWarn: {
    backgroundColor: '#3A2E12',
  },
  bannerText: {
    color: colors.text,
  },
});
