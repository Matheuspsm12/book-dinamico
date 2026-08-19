import { StyleSheet, Text, View } from "react-native";

import ClaroLogo from "../../../assets/logo_claro.svg";
import ClaroSymbol from "../../../assets/logo_symbol_c.svg";
import { theme } from "../../styles/theme";

type BrandLockupProps = {
  variant?: "header" | "drawer" | "login";
};

const dimensions = {
  header: { symbolWidth: 46, symbolHeight: 47, dividerHeight: 34 },
  drawer: { symbolWidth: 48, symbolHeight: 49, dividerHeight: 38 },
} as const;

export function BrandLockup({ variant = "header" }: BrandLockupProps) {
  if (variant === "login") {
    return (
      <View accessibilityRole="header" style={styles.login}>
        <ClaroLogo
          accessibilityLabel="Claro"
          height={69}
          width={188}
        />
        <Text style={styles.loginSubtitle}>BOOK DINÂMICO</Text>
      </View>
    );
  }

  const size = dimensions[variant];

  return (
    <View
      accessibilityLabel="Books Claro — Book Dinâmico"
      accessibilityRole="header"
      style={styles.lockup}
    >
      <View
        style={{ height: size.symbolHeight, width: size.symbolWidth }}
      >
        <ClaroSymbol height="100%" width="100%" />
      </View>

      <View
        importantForAccessibility="no"
        style={[styles.divider, { height: size.dividerHeight }]}
      />

      <View style={styles.copy}>
        <Text numberOfLines={1} style={styles.title}>
          BOOKS CLARO
        </Text>
        <Text numberOfLines={1} style={styles.subtitle}>
          BOOK DINÂMICO
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  lockup: {
    flexDirection: "row",
    alignItems: "center",
    flexShrink: 1,
  },
  divider: {
    width: 1,
    marginHorizontal: 14,
    backgroundColor: theme.colors.border,
  },
  copy: {
    flexShrink: 1,
    justifyContent: "center",
  },
  title: {
    color: theme.colors.text,
    fontSize: 16,
    fontWeight: "900",
    letterSpacing: 0.7,
    lineHeight: 20,
  },
  subtitle: {
    marginTop: 3,
    color: theme.colors.textMuted,
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 1.3,
    lineHeight: 14,
  },
  login: {
    alignItems: "center",
  },
  loginSubtitle: {
    marginTop: theme.spacing.xs,
    color: theme.colors.textMuted,
    fontSize: 13,
    fontWeight: "700",
    letterSpacing: 1.5,
  },
});
