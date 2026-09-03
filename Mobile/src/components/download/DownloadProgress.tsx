import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import { theme } from "../../styles/theme";
import type { DownloadState } from "../../types/download";

type DownloadProgressProps = {
  state: DownloadState;
  onCancel: () => void;
};

export function DownloadProgress({ state, onCancel }: DownloadProgressProps) {
  const canCancel =
    state.status === "preparing" || state.status === "downloading";
  const cancelling = state.status === "cancelling";
  const progressPercentage =
    state.progress == null ? null : Math.round(state.progress * 100);

  return (
    <View accessibilityLiveRegion="polite" style={styles.container}>
      <View style={styles.statusRow}>
        {(state.status === "preparing" ||
          state.status === "downloading" ||
          state.status === "sharing" ||
          cancelling) && <ActivityIndicator color={theme.colors.surface} />}
        <Text style={styles.statusText}>{state.message}</Text>
      </View>

      {(state.status === "downloading" || state.status === "cancelling") && (
        <>
          <View
            accessibilityLabel="Progresso do download"
            accessibilityRole="progressbar"
            accessibilityValue={{
              min: 0,
              max: 100,
              now: progressPercentage ?? undefined,
              text: state.message ?? undefined,
            }}
            style={styles.progressTrack}
          >
            <View
              style={[
                styles.progressFill,
                { width: `${progressPercentage ?? 0}%` },
              ]}
            />
          </View>
          <Text style={styles.bytesText}>
            {formatBytes(state.bytesWritten)} de {formatBytes(state.totalBytes)}
          </Text>
        </>
      )}

      {(canCancel || cancelling) && (
        <Pressable
          accessibilityLabel="Cancelar download"
          accessibilityRole="button"
          accessibilityState={{ disabled: cancelling }}
          disabled={cancelling}
          onPress={onCancel}
          style={({ pressed }) => [
            styles.cancelButton,
            pressed && styles.cancelButtonPressed,
            cancelling && styles.disabled,
          ]}
        >
          <Text style={styles.cancelText}>
            {cancelling ? "CANCELANDO…" : "CANCELAR"}
          </Text>
        </Pressable>
      )}
    </View>
  );
}

export function formatBytes(bytes: number) {
  if (!Number.isFinite(bytes) || bytes <= 0) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

const styles = StyleSheet.create({
  container: {
    marginTop: theme.spacing.lg,
  },
  statusRow: {
    minHeight: 24,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: theme.spacing.sm,
  },
  statusText: {
    flexShrink: 1,
    color: theme.colors.surface,
    fontSize: 13,
    fontWeight: "800",
    textAlign: "center",
  },
  progressTrack: {
    height: 8,
    marginTop: theme.spacing.md,
    overflow: "hidden",
    borderRadius: theme.radius.full,
    backgroundColor: "rgba(255,255,255,0.3)",
  },
  progressFill: {
    height: "100%",
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.surface,
  },
  bytesText: {
    marginTop: theme.spacing.xs,
    color: theme.colors.surface,
    fontSize: 11,
    fontWeight: "700",
    textAlign: "center",
    opacity: 0.88,
  },
  cancelButton: {
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    marginTop: theme.spacing.md,
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.8)",
    borderRadius: theme.radius.sm,
  },
  cancelButtonPressed: {
    backgroundColor: "rgba(255,255,255,0.12)",
  },
  cancelText: {
    color: theme.colors.surface,
    fontSize: 12,
    fontWeight: "900",
    letterSpacing: 1,
  },
  disabled: {
    opacity: 0.55,
  },
});
