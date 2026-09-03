import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { AppHeader } from "../components/navigation/AppHeader";
import { useAuth } from "../context/AuthContext";
import { friendlyApiError, isUnauthorized } from "../services/api";
import { contarUsuarios, listarDocumentos } from "../services/dashboard-service";
import { theme } from "../styles/theme";
import type { DocumentoResponse } from "../types/api";

type Counts = { documentos: number; pendentes: number; aprovados: number; rejeitados: number };

export function DashboardScreen() {
  const { session, signOut } = useAuth();
  const [documents, setDocuments] = useState<DocumentoResponse[]>([]);
  const [counts, setCounts] = useState<Counts>({ documentos: 0, pendentes: 0, aprovados: 0, rejeitados: 0 });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (refresh = false) => {
    refresh ? setRefreshing(true) : setLoading(true);
    setError(null);
    try {
      const documentsResult = await listarDocumentos();
      let userCounts = { pendentes: 0, aprovados: 0, rejeitados: 0 };
      if (session?.role === "ADMIN") {
        const [pendentes, aprovados, rejeitados] = await Promise.all([
          contarUsuarios("PENDENTE"), contarUsuarios("APROVADO"), contarUsuarios("REJEITADO"),
        ]);
        userCounts = { pendentes, aprovados, rejeitados };
      }
      setDocuments(documentsResult);
      setCounts({ documentos: documentsResult.length, ...userCounts });
    } catch (cause) {
      if (isUnauthorized(cause)) { await signOut(); return; }
      setError(friendlyApiError(cause, "Não foi possível carregar o dashboard."));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [session?.role, signOut]);

  useEffect(() => { void load(); }, [load]);

  return (
    <SafeAreaView edges={["top", "left", "right"]} style={styles.safeArea}>
      <AppHeader />
      <ScrollView contentContainerStyle={styles.content} refreshControl={<RefreshControl colors={[theme.colors.brand]} onRefresh={() => void load(true)} refreshing={refreshing} tintColor={theme.colors.brand} />}>
        <Text style={styles.title} accessibilityRole="header">Dashboard</Text>
        <Text style={styles.subtitle}>Visão geral do portal Books Claro.</Text>
        <Text style={styles.sectionTitle}>Usuários</Text>
        {error && <View accessibilityLiveRegion="polite" style={styles.error}><Text style={styles.errorText}>{error}</Text></View>}
        <View style={styles.grid}>
          <StatCard label="Usuários aprovados" value={loading ? "…" : counts.aprovados} />
          <StatCard label="Usuários pendentes" value={loading ? "…" : counts.pendentes} />
          <StatCard label="Usuários rejeitados" value={loading ? "…" : counts.rejeitados} />
          <StatCard label="Documentos publicados" value={loading ? "…" : counts.documentos} />
        </View>
        <Text style={styles.sectionTitle}>Catálogo atual de documentos</Text>
        {loading ? <View style={styles.loading}><ActivityIndicator color={theme.colors.brand} size="large" /><Text style={styles.muted}>Carregando…</Text></View> : documents.length === 0 ? <Text style={styles.muted}>Nenhum documento publicado ainda.</Text> : documents.slice(0, 3).map((document) => <View key={document.id} style={styles.document}><Text style={styles.documentType}>{document.extensao}</Text><Text style={styles.documentName} numberOfLines={1}>{document.nome}</Text></View>)}
      </ScrollView>
    </SafeAreaView>
  );
}

function StatCard({ label, value }: { label: string; value: number | string }) {
  return <View style={styles.statCard}><Text style={styles.statLabel}>{label.toUpperCase()}</Text><Text style={styles.statValue}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: theme.colors.background },
  content: { padding: theme.spacing.md, paddingBottom: theme.spacing.xl },
  title: { color: theme.colors.brand, fontSize: 34, fontWeight: "900" },
  subtitle: { marginTop: theme.spacing.xs, color: theme.colors.textMuted, fontSize: 16, lineHeight: 22 },
  sectionTitle: { marginTop: theme.spacing.xl, marginBottom: theme.spacing.sm, color: theme.colors.text, fontSize: 16, fontWeight: "800" },
  grid: { gap: theme.spacing.sm },
  statCard: { minHeight: 126, justifyContent: "space-between", padding: theme.spacing.md, borderWidth: 1, borderColor: theme.colors.border, borderRadius: theme.radius.lg, backgroundColor: theme.colors.surface, shadowColor: "#000", shadowOffset: { width: 0, height: 3 }, shadowOpacity: 0.06, shadowRadius: 6, elevation: 2 },
  statLabel: { color: theme.colors.textMuted, fontSize: 11, fontWeight: "800", letterSpacing: 0.8 },
  statValue: { color: theme.colors.text, fontSize: 42, fontWeight: "900" },
  document: { marginBottom: theme.spacing.sm, padding: theme.spacing.md, borderRadius: theme.radius.md, backgroundColor: theme.colors.surface },
  documentType: { color: theme.colors.brand, fontSize: 11, fontWeight: "900", letterSpacing: 1 },
  documentName: { marginTop: theme.spacing.xs, color: theme.colors.text, fontSize: 16, fontWeight: "800" },
  loading: { alignItems: "center", padding: theme.spacing.xl },
  muted: { marginTop: theme.spacing.sm, color: theme.colors.textMuted, fontSize: 14 },
  error: { marginBottom: theme.spacing.md, padding: theme.spacing.md, borderWidth: 1, borderColor: "#FECDCA", borderRadius: theme.radius.md, backgroundColor: theme.colors.errorSurface },
  errorText: { color: theme.colors.error, fontSize: 13, lineHeight: 18 },
});
