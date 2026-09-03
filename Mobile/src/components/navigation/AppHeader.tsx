import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useNavigation, useRoute } from "@react-navigation/native";
import type { NativeStackNavigationProp } from "@react-navigation/native-stack";

import { BrandLockup } from "../brand/BrandLockup";
import { NavigationDrawer } from "./NavigationDrawer";
import { useAuth } from "../../context/AuthContext";
import type { RootStackParamList } from "../../navigation/types";
import { theme } from "../../styles/theme";

type Navigation = NativeStackNavigationProp<RootStackParamList>;
type AuthenticatedRoute = "Dashboard" | "Books";

type AppHeaderProps = {
  navigationDisabled?: boolean;
};

export function AppHeader({ navigationDisabled = false }: AppHeaderProps) {
  const navigation = useNavigation<Navigation>();
  const route = useRoute();
  const { session, signOut } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  function navigateTo(screen: AuthenticatedRoute) {
    if (navigationDisabled) return;
    setMenuOpen(false);
    navigation.navigate(screen);
  }

  function handleSignOut() {
    if (navigationDisabled) return;
    setMenuOpen(false);
    void signOut();
  }

  return (
    <>
      <View style={styles.header}>
        <BrandLockup />

        <View style={styles.actions}>
          <Pressable
            accessibilityLabel="Abrir menu principal"
            accessibilityRole="button"
            hitSlop={4}
            onPress={() => setMenuOpen(true)}
            style={({ pressed }) => [
              styles.iconButton,
              pressed && styles.pressed,
            ]}
          >
            <View style={styles.menuLine} />
            <View style={styles.menuLine} />
            <View style={styles.menuLine} />
          </Pressable>

          <Pressable
            accessibilityLabel={`Abrir menu de ${session?.nome || "usuário"}`}
            accessibilityRole="button"
            hitSlop={4}
            onPress={() => setMenuOpen(true)}
            style={({ pressed }) => [
              styles.avatar,
              pressed && styles.pressed,
            ]}
          >
            <Text style={styles.avatarText}>
              {(session?.nome || "U").slice(0, 1).toUpperCase()}
            </Text>
          </Pressable>
        </View>
      </View>

      <NavigationDrawer
        activeRoute={route.name}
        onClose={() => setMenuOpen(false)}
        onNavigate={navigateTo}
        onSignOut={handleSignOut}
        open={menuOpen}
        navigationDisabled={navigationDisabled}
        session={session}
      />
    </>
  );
}

const styles = StyleSheet.create({
  header: {
    minHeight: 72,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 12,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
    backgroundColor: theme.colors.surface,
  },
  actions: {
    flexDirection: "row",
    alignItems: "center",
    gap: theme.spacing.xs,
    marginLeft: theme.spacing.sm,
  },
  iconButton: {
    width: 44,
    height: 44,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: theme.radius.md,
  },
  menuLine: {
    width: 21,
    height: 2,
    marginVertical: 2.5,
    borderRadius: 2,
    backgroundColor: theme.colors.text,
  },
  avatar: {
    width: 42,
    height: 42,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.surfaceMuted,
  },
  avatarText: {
    color: theme.colors.text,
    fontSize: 16,
    fontWeight: "800",
  },
  pressed: {
    opacity: 0.65,
  },
});
