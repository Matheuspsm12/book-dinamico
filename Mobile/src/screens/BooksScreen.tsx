import { useCallback, useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from "react-native";
import {
  useNavigation,
  usePreventRemove,
  type NavigationAction,
} from "@react-navigation/native";
import type { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { SafeAreaView } from "react-native-safe-area-context";

import { DownloadProgress, formatBytes } from "../components/download/DownloadProgress";
import { AppHeader } from "../components/navigation/AppHeader";
import { useAuth } from "../context/AuthContext";
import { useDocumentDownload } from "../hooks/useDocumentDownload";
import type { RootStackParamList } from "../navigation/types";
import { SessionExpiredError } from "../services/document-download-service";
import { listarDocumentos } from "../services/documentos-service";
import { friendlyApiError, isUnauthorized } from "../services/api";
import { theme } from "../styles/theme";
import type { DocumentoResponse } from "../types/api";
import type { DownloadState } from "../types/download";

type Navigation = NativeStackNavigationProp<RootStackParamList>;

export function BooksScreen() {
  const navigation = useNavigation<Navigation>();
  const { session, signOut } = useAuth();
  const [documents, setDocuments] = useState<DocumentoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const download = useDocumentDownload();
  const pendingNavigationRef = useRef<NavigationAction | null>(null);

  const loadDocuments = useCallback(
    async (refresh = false) => {
      refresh ? setRefreshing(true) : setLoading(true);
      setError(null);
      try {
        setDocuments(await listarDocumentos());
      } catch (cause) {
        if (isUnauthorized(cause)) {
          await signOut();
          return;
        }
        setError(friendlyApiError(cause, "Não foi possível carregar os documentos."));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [signOut],
  );

  useEffect(() => {
    void loadDocuments();
  }, [loadDocuments]);

  usePreventRemove(download.isBusy, ({ data }) => {
    Alert.alert(
      "Download em andamento",
      "Para sair desta tela, o download atual precisa ser cancelado.",
      [
        { text: "Continuar download", style: "cancel" },
        {
          text: "Cancelar e sair",
          style: "destructive",
          onPress: () => {
            pendingNavigationRef.current = data.action;
            void download.cancel();
          },
        },
      ],
    );
  });

  useEffect(() => {
    if (download.isBusy || !pendingNavigationRef.current) return;
    const action = pendingNavigationRef.current;
    pendingNavigationRef.current = null;
    navigation.dispatch(action);
  }, [download.isBusy, navigation]);

  async function handleDownload(document: DocumentoResponse) {
    const outcome = await download.start(document);
    if (outcome.status === "failed") {
      if (outcome.error instanceof SessionExpiredError) {
        Alert.alert("Sessão expirada", "Entre novamente para continuar.");
        await signOut();
        return;
      }
      Alert.alert(
        "Não foi possível baixar",
        outcome.error.message,
      );
      download.reset();
      return;
    }

    if (outcome.status === "completed" || outcome.status === "cancelled") {
      setTimeout(download.reset, 1600);
    }
  }

  return (
    <SafeAreaView style={styles.safeArea} edges={["top", "left", "right"]}>
      <AppHeader navigationDisabled={download.isBusy} />
      <FlatList
        contentContainerStyle={styles.content}
        data={documents}
        extraData={download.state}
        keyExtractor={(item) => String(item.id)}
        refreshControl={
          <RefreshControl
            colors={[theme.colors.brand]}
            enabled={!download.isBusy}
            onRefresh={() => {
              if (!download.isBusy) void loadDocuments(true);
            }}
            refreshing={refreshing}
            tintColor={theme.colors.brand}
          />
        }
        ListHeaderComponent={
          <View>
            <View style={styles.userCard}>
              <Text style={styles.greeting}>Olá, {session?.nome || "usuário"}</Text>
              <Text style={styles.userMeta}>{session?.role || "Perfil não informado"}</Text>
            </View>

            <Text style={styles.heading} accessibilityRole="header">Books disponíveis</Text>
            <Text style={styles.description}>
              Baixe a versão mais recente e escolha onde abrir, salvar ou compartilhar.
            </Text>

            {error && (
              <View style={styles.errorBox} accessibilityLiveRegion="polite">
                <Text style={styles.errorText}>{error}</Text>
                <Pressable
                  accessibilityRole="button"
                  onPress={() => void loadDocuments()}
                  style={({ pressed }) => [styles.retryButton, pressed && styles.pressed]}
                >
                  <Text style={styles.retryText}>Tentar novamente</Text>
                </Pressable>
              </View>
            )}
          </View>
        }
        ListEmptyComponent={
          loading ? (
            <View style={styles.stateBox} accessibilityLabel="Carregando documentos">
              <ActivityIndicator color={theme.colors.brand} size="large" />
              <Text style={styles.stateText}>Carregando documentos…</Text>
            </View>
          ) : !error ? (
            <View style={styles.stateBox}>
              <Text style={styles.emptyTitle}>Nenhum documento disponível</Text>
              <Text style={styles.stateText}>Aguarde uma publicação do administrador.</Text>
            </View>
          ) : null
        }
        renderItem={({ item }) => (
          <BookCard
            document={item}
            downloadState={
              download.state.documentId === item.id ? download.state : null
            }
            downloadsLocked={download.isBusy}
            onCancel={() => void download.cancel()}
            onDownload={() => void handleDownload(item)}
          />
        )}
        ItemSeparatorComponent={() => <View style={styles.separator} />}
        ListFooterComponent={
          <Text style={styles.footer}>CONECTANDO DADOS, IMPULSIONANDO RESULTADOS</Text>
        }
      />
    </SafeAreaView>
  );
}

function BookCard({
  document,
  downloadState,
  downloadsLocked,
  onCancel,
  onDownload,
}: {
  document: DocumentoResponse;
  downloadState: DownloadState | null;
  downloadsLocked: boolean;
  onCancel: () => void;
  onDownload: () => void;
}) {
  const active = downloadState != null && downloadState.status !== "idle";
  const disabled = downloadsLocked || active;

  return (
    <View style={styles.bookCard}>
      <View style={styles.cardCopy}>
        <Text style={styles.bookName}>{document.nome}</Text>
        <Text style={styles.bookType}>
          {document.tipo === "EXCEL" ? "EXCEL" : "POWER POINT"} · {document.extensao} · {formatBytes(document.tamanhoBytes)}
        </Text>
        <Text style={styles.bookDescription} numberOfLines={2}>
          {document.descricao || "Documento publicado no Book Dinâmico."}
        </Text>
        <Text style={styles.updatedAt}>Atualizado em {formatDate(document.dataAtualizacao)}</Text>
      </View>

      {active && downloadState ? (
        <DownloadProgress onCancel={onCancel} state={downloadState} />
      ) : (
        <Pressable
          accessibilityLabel={`Baixar ${document.nome}, ${formatBytes(document.tamanhoBytes)}`}
          accessibilityRole="button"
          accessibilityState={{ disabled }}
          disabled={disabled}
          onPress={onDownload}
          style={({ pressed }) => [
            styles.downloadButton,
            pressed && styles.downloadPressed,
            disabled && styles.disabled,
          ]}
        >
          <Text style={styles.downloadText}>
            {downloadsLocked ? "AGUARDE" : "BAIXAR"}
          </Text>
        </Pressable>
      )}
    </View>
  );
}

function formatDate(value: string) {
  const [year, month, day] = value.split("-");
  return year && month && day ? `${day}/${month}/${year}` : value;
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: theme.colors.background },
  content: { flexGrow: 1, padding: theme.spacing.md, paddingBottom: 0 },
  pressed: { opacity: 0.65 },
  userCard: {
    marginBottom: theme.spacing.lg,
    padding: theme.spacing.md,
    borderLeftWidth: 4,
    borderLeftColor: theme.colors.brand,
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.surface,
  },
  greeting: { color: theme.colors.text, fontSize: 17, fontWeight: "800" },
  userMeta: {
    marginTop: theme.spacing.xs,
    color: theme.colors.textMuted,
    fontSize: 12,
    fontWeight: "700",
  },
  heading: { color: theme.colors.text, fontSize: 24, fontWeight: "900" },
  description: {
    marginTop: theme.spacing.xs,
    marginBottom: theme.spacing.lg,
    color: theme.colors.textMuted,
    fontSize: 14,
    lineHeight: 20,
  },
  errorBox: {
    marginBottom: theme.spacing.md,
    padding: theme.spacing.md,
    borderWidth: 1,
    borderColor: "#FECDCA",
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.errorSurface,
  },
  errorText: { color: theme.colors.error, fontSize: 13, lineHeight: 18 },
  retryButton: {
    minHeight: 44,
    alignSelf: "flex-start",
    justifyContent: "center",
    marginTop: theme.spacing.sm,
  },
  retryText: { color: theme.colors.error, fontSize: 13, fontWeight: "800" },
  stateBox: {
    minHeight: 220,
    alignItems: "center",
    justifyContent: "center",
    padding: theme.spacing.lg,
    borderRadius: theme.radius.lg,
    backgroundColor: theme.colors.surface,
  },
  emptyTitle: { color: theme.colors.text, fontSize: 16, fontWeight: "800" },
  stateText: {
    marginTop: theme.spacing.sm,
    color: theme.colors.textMuted,
    fontSize: 14,
    lineHeight: 20,
    textAlign: "center",
  },
  bookCard: {
    minHeight: 210,
    justifyContent: "space-between",
    padding: theme.spacing.lg,
    borderRadius: theme.radius.lg,
    backgroundColor: theme.colors.brand,
    shadowColor: "#7A0000",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.24,
    shadowRadius: 12,
    elevation: 4,
  },
  cardCopy: { flex: 1 },
  bookName: {
    color: theme.colors.surface,
    fontSize: 20,
    fontWeight: "900",
    letterSpacing: 0.5,
    textTransform: "uppercase",
  },
  bookType: {
    marginTop: theme.spacing.sm,
    color: theme.colors.surface,
    fontSize: 12,
    fontWeight: "800",
    letterSpacing: 1.2,
    opacity: 0.88,
  },
  bookDescription: {
    marginTop: theme.spacing.md,
    color: theme.colors.surface,
    fontSize: 13,
    lineHeight: 19,
    opacity: 0.9,
  },
  updatedAt: {
    marginTop: theme.spacing.md,
    color: theme.colors.surface,
    fontSize: 11,
    fontWeight: "700",
    opacity: 0.78,
  },
  downloadButton: {
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    marginTop: theme.spacing.lg,
    borderRadius: theme.radius.sm,
    backgroundColor: theme.colors.surface,
  },
  downloadPressed: { backgroundColor: theme.colors.surfaceMuted },
  downloadText: { color: theme.colors.text, fontSize: 13, fontWeight: "900", letterSpacing: 1.2 },
  disabled: { opacity: 0.55 },
  separator: { height: theme.spacing.md },
  footer: {
    marginTop: theme.spacing.xl,
    marginHorizontal: -theme.spacing.md,
    paddingHorizontal: theme.spacing.md,
    paddingVertical: theme.spacing.md,
    backgroundColor: theme.colors.action,
    color: theme.colors.surface,
    fontSize: 10,
    fontWeight: "800",
    letterSpacing: 1.3,
    textAlign: "center",
  },
});
