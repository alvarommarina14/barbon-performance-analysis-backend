import { colors, fonts, radii, spacing } from '@court-proof/core';
import { StyleSheet, Text, View } from 'react-native';

/**
 * Phase 0 shell. The player home — name, current week, latest analysis — arrives in Phase 1.
 */
export default function HomeScreen() {
  return (
    <View style={styles.screen}>
      <Text style={styles.eyebrow}>Barbon Performance</Text>
      <Text style={styles.title}>COURT PROOF</Text>
      <Text style={styles.lede}>
        Phase 0 — foundations in place: build, contract pipeline and design tokens.
      </Text>
      <Text style={styles.pill}>READY</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    justifyContent: 'center',
    gap: spacing.md,
    paddingHorizontal: spacing.xl,
    backgroundColor: colors.black,
  },
  eyebrow: {
    color: colors.gray,
    fontSize: 12,
    letterSpacing: 2,
    textTransform: 'uppercase',
  },
  title: {
    color: colors.white,
    fontFamily: fonts.display,
    fontSize: 56,
    letterSpacing: 1,
  },
  lede: {
    color: colors.gray,
    fontSize: 15,
    lineHeight: 22,
  },
  pill: {
    alignSelf: 'flex-start',
    marginTop: spacing.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.xs + 2,
    borderRadius: radii.pill,
    backgroundColor: colors.accent,
    color: colors.black,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 1,
  },
});
