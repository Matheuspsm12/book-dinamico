import { useState } from "react";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { BrandLockup } from "../components/brand/BrandLockup";
import { useAuth } from "../context/AuthContext";
import { theme } from "../styles/theme";

export function LoginScreen() {
  const { signIn } = useAuth();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [focused, setFocused] = useState<"email" | "senha" | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    if (!email.trim() || !senha) {
      setError("Informe seu usuário ou e-mail e a senha.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await signIn(email, senha);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível entrar.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.decorativeCircle} importantForAccessibility="no" />
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <ScrollView
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
        >
          <BrandLockup variant="login" />

          <View style={styles.card}>
            <Text style={styles.heading} accessibilityRole="header">Login</Text>
            <Text style={styles.description}>
              Entre com seu usuário e senha para acessar os books disponíveis.
            </Text>

            <Text style={styles.label}>Nome de usuário ou e-mail</Text>
            <TextInput
              accessibilityLabel="Nome de usuário ou e-mail"
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              onBlur={() => setFocused(null)}
              onChangeText={setEmail}
              onFocus={() => setFocused("email")}
              returnKeyType="next"
              style={[styles.input, focused === "email" && styles.inputFocused]}
              value={email}
            />

            <Text style={styles.label}>Senha</Text>
            <View style={[styles.passwordRow, focused === "senha" && styles.inputFocused]}>
              <TextInput
                accessibilityLabel="Senha"
                autoCapitalize="none"
                autoComplete="current-password"
                onBlur={() => setFocused(null)}
                onChangeText={setSenha}
                onFocus={() => setFocused("senha")}
                onSubmitEditing={() => void handleSubmit()}
                returnKeyType="done"
                secureTextEntry={!mostrarSenha}
                style={styles.passwordInput}
                value={senha}
              />
              <Pressable
                accessibilityLabel={mostrarSenha ? "Esconder senha" : "Mostrar senha"}
                accessibilityRole="button"
                hitSlop={8}
                onPress={() => setMostrarSenha((current) => !current)}
                style={({ pressed }) => [styles.showPassword, pressed && styles.pressed]}
              >
                <Text style={styles.showPasswordText}>{mostrarSenha ? "Ocultar" : "Mostrar"}</Text>
              </Pressable>
            </View>

            {error && (
              <View style={styles.errorBox} accessibilityLiveRegion="polite">
                <Text style={styles.errorText}>{error}</Text>
              </View>
            )}

            <Pressable
              accessibilityRole="button"
              disabled={submitting}
              onPress={() => void handleSubmit()}
              style={({ pressed }) => [
                styles.primaryButton,
                pressed && styles.primaryButtonPressed,
                submitting && styles.disabled,
              ]}
            >
              {submitting ? (
                <ActivityIndicator color={theme.colors.surface} accessibilityLabel="Entrando" />
              ) : (
                <Text style={styles.primaryButtonText}>Entrar</Text>
              )}
            </Pressable>
          </View>

          <Text style={styles.footer}>TCIAGROUP.COM • BOOK DINÂMICO</Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  safeArea: { flex: 1, backgroundColor: theme.colors.background },
  decorativeCircle: {
    position: "absolute",
    top: -120,
    right: -110,
    width: 280,
    height: 280,
    borderRadius: 140,
    backgroundColor: theme.colors.brand,
    opacity: 0.1,
  },
  content: {
    flexGrow: 1,
    justifyContent: "center",
    padding: theme.spacing.lg,
  },
  card: {
    width: "100%",
    maxWidth: 480,
    alignSelf: "center",
    marginTop: theme.spacing.xl,
    padding: theme.spacing.lg,
    borderRadius: theme.radius.lg,
    backgroundColor: theme.colors.surface,
    shadowColor: "#000000",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.12,
    shadowRadius: 18,
    elevation: 5,
  },
  heading: { color: theme.colors.text, fontSize: 26, fontWeight: "800" },
  description: {
    marginTop: theme.spacing.sm,
    marginBottom: theme.spacing.lg,
    color: theme.colors.textMuted,
    fontSize: 14,
    lineHeight: 20,
  },
  label: {
    marginBottom: 6,
    color: theme.colors.text,
    fontSize: 13,
    fontWeight: "700",
  },
  input: {
    minHeight: 48,
    marginBottom: theme.spacing.md,
    paddingHorizontal: 14,
    borderWidth: 1,
    borderColor: theme.colors.border,
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.surface,
    color: theme.colors.text,
    fontSize: 16,
  },
  inputFocused: { borderColor: theme.colors.brand, borderWidth: 2 },
  passwordRow: {
    minHeight: 48,
    flexDirection: "row",
    alignItems: "center",
    marginBottom: theme.spacing.md,
    borderWidth: 1,
    borderColor: theme.colors.border,
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.surface,
  },
  passwordInput: {
    minHeight: 48,
    flex: 1,
    paddingHorizontal: 14,
    color: theme.colors.text,
    fontSize: 16,
  },
  showPassword: {
    minWidth: 72,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: theme.spacing.sm,
  },
  showPasswordText: { color: theme.colors.brand, fontSize: 12, fontWeight: "700" },
  pressed: { opacity: 0.65 },
  errorBox: {
    marginBottom: theme.spacing.md,
    padding: 12,
    borderWidth: 1,
    borderColor: "#FECDCA",
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.errorSurface,
  },
  errorText: { color: theme.colors.error, fontSize: 13, lineHeight: 18 },
  primaryButton: {
    minHeight: 50,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.action,
  },
  primaryButtonPressed: { backgroundColor: "#3F3F46" },
  primaryButtonText: { color: theme.colors.surface, fontSize: 16, fontWeight: "800" },
  disabled: { opacity: 0.55 },
  footer: {
    marginTop: theme.spacing.lg,
    color: theme.colors.textMuted,
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 1.2,
    textAlign: "center",
  },
});
