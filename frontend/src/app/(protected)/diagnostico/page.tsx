"use client";

import { ChevronDown, ChevronRight, RefreshCw } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import { PageHeader } from "src/components/shared/PageHeader";
import { Card, CardContent } from "src/components/ui/card";
import { useAuth } from "src/app/contexts/AuthContext";
import type { LogConsultaResponse, LogLinhaResponse } from "src/lib/api/types";
import { cn, formatDateTime } from "src/lib/utils";
import * as diagnosticoApi from "src/services/diagnostico-service";

const NIVEIS = [
  { valor: "", label: "Todos os níveis" },
  { valor: "INFO", label: "INFO ou acima" },
  { valor: "WARN", label: "WARN ou acima" },
  { valor: "ERROR", label: "Só ERROR" },
];

const LINHAS_OPCOES = [100, 300, 500, 1000, 2000];
const AUTO_REFRESH_MS = 10_000;

const NIVEL_BADGE: Record<string, string> = {
  ERROR: "bg-red-100 text-red-700",
  WARN: "bg-amber-100 text-amber-700",
  INFO: "bg-sky-100 text-sky-700",
  DEBUG: "bg-zinc-100 text-zinc-500",
  TRACE: "bg-zinc-100 text-zinc-400",
  RAW: "bg-zinc-100 text-zinc-400",
};

export default function DiagnosticoPage() {
  const router = useRouter();
  const { user, loading } = useAuth();

  const [nivel, setNivel] = useState("");
  const [linhas, setLinhas] = useState(300);
  const [arquivo, setArquivo] = useState("");
  const [busca, setBusca] = useState("");
  const [buscaDebounced, setBuscaDebounced] = useState("");
  const [auto, setAuto] = useState(false);

  const [dados, setDados] = useState<LogConsultaResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  // debounce da busca (400ms)
  useEffect(() => {
    const t = setTimeout(() => setBuscaDebounced(busca), 400);
    return () => clearTimeout(t);
  }, [busca]);

  const carregar = useCallback(
    async (silencioso = false) => {
      if (!silencioso) setCarregando(true);
      setErr(null);
      try {
        const resp = await diagnosticoApi.buscarLogs({
          linhas,
          nivel: nivel || undefined,
          busca: buscaDebounced || undefined,
          arquivo: arquivo || undefined,
        });
        setDados(resp);
      } catch (e) {
        setErr(e instanceof Error ? e.message : "Erro ao carregar os logs.");
      } finally {
        setCarregando(false);
      }
    },
    [linhas, nivel, buscaDebounced, arquivo],
  );

  useEffect(() => {
    if (!loading && user && user.role !== "ADMIN") router.replace("/book");
  }, [user, loading, router]);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    void carregar();
  }, [user, carregar]);

  useEffect(() => {
    if (!auto || user?.role !== "ADMIN") return;
    const id = setInterval(() => void carregar(true), AUTO_REFRESH_MS);
    return () => clearInterval(id);
  }, [auto, carregar, user]);

  if (loading || !user || user.role !== "ADMIN") return null;

  const inativo = dados && !dados.logEmArquivoAtivo;
  const arquivos = dados?.arquivosDisponiveis ?? [];

  return (
    <div>
      <PageHeader
        title="Diagnóstico"
        subtitle="Últimas linhas do log da aplicação (book.log) — só para administradores."
      />

      <Card>
        <CardContent className="pt-6">
          {/* Controles */}
          <div className="mb-4 flex flex-wrap items-center gap-2">
            <select
              value={nivel}
              onChange={(e) => setNivel(e.target.value)}
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm"
            >
              {NIVEIS.map((n) => (
                <option key={n.valor} value={n.valor}>
                  {n.label}
                </option>
              ))}
            </select>

            <select
              value={linhas}
              onChange={(e) => setLinhas(Number(e.target.value))}
              className="rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm"
            >
              {LINHAS_OPCOES.map((n) => (
                <option key={n} value={n}>
                  {n} linhas
                </option>
              ))}
            </select>

            {arquivos.length > 1 && (
              <select
                value={arquivo}
                onChange={(e) => setArquivo(e.target.value)}
                className="max-w-[220px] rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm"
              >
                <option value="">Arquivo ativo</option>
                {arquivos.map((a) => (
                  <option key={a} value={a}>
                    {a}
                  </option>
                ))}
              </select>
            )}

            <input
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              placeholder="Buscar no texto…"
              className="min-w-[180px] flex-1 rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm"
            />

            <label className="flex select-none items-center gap-2 text-sm text-zinc-600">
              <input
                type="checkbox"
                checked={auto}
                onChange={(e) => setAuto(e.target.checked)}
              />
              Auto (10s)
            </label>

            <button
              type="button"
              onClick={() => void carregar()}
              className="flex items-center gap-2 rounded-md border border-zinc-300 bg-white px-3 py-2 font-medium text-sm text-zinc-700 hover:bg-zinc-50"
            >
              <RefreshCw
                size={15}
                className={cn(carregando && "animate-spin")}
              />
              Atualizar
            </button>
          </div>

          {err && (
            <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
              {err}
            </div>
          )}

          {inativo && (
            <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-6 text-amber-800 text-sm">
              {dados?.mensagemInativo ??
                "O log em arquivo não está ativo neste ambiente."}
            </div>
          )}

          {!inativo && (
            <>
              <div className="overflow-x-auto rounded-md border border-zinc-200 bg-zinc-950">
                <div className="min-w-[720px] divide-y divide-zinc-800 font-mono text-xs">
                  {carregando && !dados ? (
                    <p className="py-10 text-center text-zinc-500">Carregando…</p>
                  ) : (dados?.linhas.length ?? 0) === 0 ? (
                    <p className="py-10 text-center text-zinc-500">
                      Nenhuma linha para os filtros atuais.
                    </p>
                  ) : (
                    dados?.linhas.map((l, i) => (
                      <LinhaLog key={`${l.timestamp ?? "raw"}-${i}`} linha={l} />
                    ))
                  )}
                </div>
              </div>

              <p className="mt-3 text-xs text-zinc-400">
                {dados?.totalLinhas ?? 0} linha(s)
                {dados?.arquivo ? ` · ${dados.arquivo}` : ""}
                {dados?.geradoEm
                  ? ` · atualizado ${formatDateTime(dados.geradoEm)}`
                  : ""}
              </p>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function LinhaLog({ linha }: { linha: LogLinhaResponse }) {
  const [aberto, setAberto] = useState(false);
  const temStack = Boolean(linha.stacktrace);
  const badge = NIVEL_BADGE[linha.nivel] ?? "bg-zinc-100 text-zinc-500";

  return (
    <div className="px-3 py-1.5 hover:bg-zinc-900">
      <div
        className={cn(
          "flex items-start gap-2",
          temStack && "cursor-pointer",
        )}
        onClick={temStack ? () => setAberto((v) => !v) : undefined}
      >
        <span className="w-4 shrink-0 pt-0.5 text-zinc-600">
          {temStack ? (
            aberto ? (
              <ChevronDown size={12} />
            ) : (
              <ChevronRight size={12} />
            )
          ) : null}
        </span>
        <span className="shrink-0 text-zinc-500">
          {linha.timestamp ?? "—"}
        </span>
        <span
          className={cn(
            "shrink-0 rounded px-1.5 py-0.5 font-semibold",
            badge,
          )}
        >
          {linha.nivel}
        </span>
        {linha.logger && (
          <span
            className="shrink-0 max-w-[240px] truncate text-zinc-400"
            title={linha.logger}
          >
            {linha.logger}
          </span>
        )}
        <span className="whitespace-pre-wrap break-words text-zinc-200">
          {linha.mensagem}
        </span>
      </div>

      {temStack && aberto && (
        <pre className="mt-1 ml-6 overflow-x-auto whitespace-pre-wrap break-words border-zinc-800 border-l-2 pl-3 text-[11px] text-red-300">
          {linha.stacktrace}
        </pre>
      )}
    </div>
  );
}
