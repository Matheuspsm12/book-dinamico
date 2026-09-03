import { Pressable, StyleSheet, Text, View } from "react-native";

import { theme } from "../../styles/theme";

type NavigationItemProps = {
  label: string;
  icon: string;
  active?: boolean;
  disabled?: boolean;
  destructive?: boolean;
  onPress?: () => void;
};

export function NavigationItem({
  label,
  icon,
  active = false,
  disabled = false,
  destructive = false,
  onPress,
}: NavigationItemProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled, selected: active }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.item,
        active && styles.activeItem,
        disabled && styles.disabledItem,
        pressed && styles.pressedItem,
      ]}
    >
      <View style={styles.copy}>
        <Text
          importantForAccessibility="no"
          style={[
            styles.icon,
            active && styles.activeText,
            destructive && styles.destructiveText,
          ]}
        >
          {icon}
        </Text>
        <Text
          style={[
            styles.label,
            active && styles.activeText,
            destructive && styles.destructiveText,
          ]}
        >
          {label}
        </Text>
      </View>
      {disabled && <Text style={styles.pending}>Em breve</Text>}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  item: {
    minHeight: 48,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: theme.spacing.sm,
    borderRadius: theme.radius.md,
  },
  activeItem: {
    backgroundColor: theme.colors.brand,
  },
  disabledItem: {
    opacity: 0.45,
  },
  pressedItem: {
    opacity: 0.7,
  },
  copy: {
    flexDirection: "row",
    alignItems: "center",
  },
  icon: {
    width: 32,
    color: theme.colors.textMuted,
    fontSize: 20,
    textAlign: "center",
  },
  label: {
    color: theme.colors.text,
    fontSize: 15,
    fontWeight: "700",
  },
  activeText: {
    color: theme.colors.surface,
  },
  destructiveText: {
    color: theme.colors.brand,
  },
  pending: {
    color: theme.colors.textMuted,
    fontSize: 11,
  },
});
