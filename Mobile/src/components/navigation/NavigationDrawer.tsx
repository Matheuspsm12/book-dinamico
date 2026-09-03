import { Modal, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { BrandLockup } from "../brand/BrandLockup";
import { NavigationItem } from "./NavigationItem";
import { theme } from "../../styles/theme";
import type { AuthSession } from "../../types/api";

type AuthenticatedRoute = "Dashboard" | "Books";

type NavigationDrawerProps = {
  activeRoute: string;
  open: boolean;
  session: AuthSession | null;
  onClose: () => void;
  onNavigate: (route: AuthenticatedRoute) => void;
  onSignOut: () => void;
  navigationDisabled?: boolean;
};

export function NavigationDrawer({
  activeRoute,
  open,
  session,
  onClose,
  onNavigate,
  onSignOut,
  navigationDisabled = false,
}: NavigationDrawerProps) {
  const isAdmin = session?.role === "ADMIN";

  return (
    <Modal
      animationType="fade"
      onRequestClose={onClose}
      statusBarTranslucent
      transparent
      visible={open}
    >
      <View style={styles.modalRoot}>
        <SafeAreaView
          accessibilityViewIsModal
          edges={["top", "bottom"]}
          style={styles.drawer}
        >
          <View style={styles.header}>
            <BrandLockup variant="drawer" />
            <Pressable
              accessibilityLabel="Fechar menu"
              accessibilityRole="button"
              hitSlop={4}
              onPress={onClose}
              style={({ pressed }) => [
                styles.closeButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.closeText}>×</Text>
            </Pressable>
          </View>

          <View style={styles.profile}>
            <Text style={styles.profileName}>{session?.nome || "Usuário"}</Text>
            <Text style={styles.profileRole}>
              {isAdmin ? "Administrador" : "Usuário"}
            </Text>
          </View>

          <Text style={styles.sectionLabel}>NAVEGAÇÃO</Text>
          {navigationDisabled && (
            <View accessibilityLiveRegion="polite" style={styles.lockNotice}>
              <Text style={styles.lockNoticeText}>
                Cancele ou conclua o download para navegar ou sair.
              </Text>
            </View>
          )}
          <NavigationItem
            active={activeRoute === "Dashboard"}
            disabled={navigationDisabled}
            icon="▦"
            label="Dashboard"
            onPress={() => onNavigate("Dashboard")}
          />
          <NavigationItem
            active={activeRoute === "Books"}
            disabled={navigationDisabled}
            icon="⇩"
            label="Books"
            onPress={() => onNavigate("Books")}
          />

          {isAdmin && (
            <>
              <Text style={styles.sectionLabel}>PORTAL</Text>
              <NavigationItem disabled icon="⇧" label="Upload Book" />
              <NavigationItem disabled icon="◷" label="Histórico" />
              <NavigationItem disabled icon="☷" label="Processamentos" />
              <NavigationItem disabled icon="♧" label="Gerenciar usuários" />
            </>
          )}

          <View style={styles.spacer} />
          <NavigationItem
            destructive
            disabled={navigationDisabled}
            icon="↪"
            label="Sair"
            onPress={onSignOut}
          />
        </SafeAreaView>

        <Pressable
          accessibilityLabel="Fechar menu"
          accessibilityRole="button"
          onPress={onClose}
          style={styles.backdrop}
        />
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  modalRoot: {
    flex: 1,
    flexDirection: "row",
  },
  drawer: {
    width: "88%",
    maxWidth: 360,
    paddingHorizontal: theme.spacing.md,
    paddingBottom: theme.spacing.md,
    backgroundColor: theme.colors.surface,
    elevation: 16,
  },
  backdrop: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.42)",
  },
  header: {
    minHeight: 64,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingBottom: theme.spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
  },
  closeButton: {
    width: 44,
    height: 44,
    alignItems: "center",
    justifyContent: "center",
    marginLeft: theme.spacing.xs,
  },
  closeText: {
    color: theme.colors.textMuted,
    fontSize: 30,
    lineHeight: 32,
  },
  pressed: {
    opacity: 0.65,
  },
  profile: {
    marginVertical: theme.spacing.md,
    padding: theme.spacing.md,
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.surfaceMuted,
  },
  profileName: {
    color: theme.colors.text,
    fontSize: 16,
    fontWeight: "800",
  },
  profileRole: {
    marginTop: 2,
    color: theme.colors.textMuted,
    fontSize: 12,
  },
  sectionLabel: {
    marginTop: theme.spacing.sm,
    marginBottom: theme.spacing.xs,
    color: theme.colors.textMuted,
    fontSize: 11,
    fontWeight: "800",
    letterSpacing: 1,
  },
  lockNotice: {
    marginBottom: theme.spacing.sm,
    padding: theme.spacing.sm,
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.errorSurface,
  },
  lockNoticeText: {
    color: theme.colors.error,
    fontSize: 12,
    lineHeight: 17,
  },
  spacer: {
    flex: 1,
    minHeight: theme.spacing.lg,
  },
});
