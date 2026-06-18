"use client";

import {
  Download,
  FileSpreadsheet,
  Filter,
  RefreshCw,
  RotateCcw,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { PageHeader } from "src/components/shared/PageHeader";
import { Button } from "src/components/ui/button";
import { Card, CardContent } from "src/components/ui/card";
import type {
  PageResponse,
  ProcessamentoResponse,
  ProcessamentoTipo,
} from "src/lib/api/types";
import { cn } from "src/lib/utils";
import * as processamentosApi from "src/services/processamentos-service";

const PAGE_SIZE = 20;

function salvarBlob(blob: Blob, nomeArquivo: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = nomeArquivo;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function formatDateTime(value?: string) {
  return value || "-";
}

function statusClass(status?: string) {
  const normalized = status?.toLowerCase() ?? "";
  if (normalized.includes("sucesso")) {
    return "border-emerald-200 bg-emerald-50 text-emerald-700";
  }
  if (normalized.includes("erro")) {
    return "border-red-200 bg-red-50 text-red-700";
  }
  return "border-amber-200 bg-amber-50 text-amber-700";
}

export default function ProcessamentosPage() {
  const [page, setPage] = useState<PageResponse<ProcessamentoResponse> | null>(
    null,
  );
  const [tipo, setTipo] = useState<ProcessamentoTipo | "">("");
  const [pageNumber, setPageNumber] = useState(0);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const response = tipo
        ? await processamentosApi.filtrar(tipo, pageNumber, PAGE_SIZE)
        : await processamentosApi.listar(pageNumber, PAGE_SIZE);
      setPage(response);
    } catch (e) {
      setErr(
        e instanceof Error ? e.message : "Erro ao carregar processamentos.",
      );
    } finally {
      setLoading(false);
    }
  }, [pageNumber, tipo]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function baixar(proc: ProcessamentoResponse) {
    setBusyId(proc.id);
    setErr(null);
    try {
      const blob = await processamentosApi.baixar(proc.id);
      salvarBlob(blob, proc.nomeArquivo || `processamento-${proc.id}`);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao baixar arquivo.");
    } finally {
      setBusyId(null);
    }
  }

  async function reprocessar(proc: ProcessamentoResponse) {
    setBusyId(proc.id);
    setErr(null);
    try {
      await processamentosApi.reprocessar(proc.id);
      await carregar();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao reprocessar.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Processamentos"
        subtitle="Acompanhe uploads processados, downloads e reagendamentos."
      />

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="inline-flex h-10 items-center gap-2 rounded-md border border-zinc-300 bg-white px-3 text-sm text-zinc-700">
            <Filter size={16} />
            <select
              value={tipo}
              onChange={(e) => {
                setTipo(e.target.value as ProcessamentoTipo | "");
                setPageNumber(0);
              }}
              className="bg-transparent outline-none"
              aria-label="Filtrar por tipo de processamento"
            >
              <option value="">Todos os tipos</option>
              <option value="DOCUMENTO">Documento</option>
            </select>
          </div>
          <Button variant="outline" onClick={() => void carregar()}>
            <RefreshCw size={16} /> Atualizar
          </Button>
        </div>

        <p className="text-sm text-zinc-500">
          {loading
            ? "Carregando..."
            : `${page?.totalElements ?? 0} processamento(s)`}
        </p>
      </div>

      {err && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-2 text-red-700 text-sm">
          {err}
        </div>
      )}

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead className="border-zinc-200 border-b bg-zinc-50 text-xs text-zinc-500 uppercase">
                <tr>
                  <th className="px-4 py-3 font-semibold">Arquivo</th>
                  <th className="px-4 py-3 font-semibold">Tipo</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Usuario</th>
                  <th className="px-4 py-3 font-semibold">Inicio</th>
                  <th className="px-4 py-3 font-semibold">Fim</th>
                  <th className="px-4 py-3 text-right font-semibold">Acoes</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-10 text-center text-zinc-500"
                    >
                      Carregando processamentos...
                    </td>
                  </tr>
                ) : !page || page.content.length === 0 ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-10 text-center text-zinc-500"
                    >
                      Nenhum processamento encontrado.
                    </td>
                  </tr>
                ) : (
                  page.content.map((proc) => (
                    <tr
                      key={proc.id}
                      className="border-zinc-100 border-b last:border-0"
                    >
                      <td className="px-4 py-3">
                        <div className="flex min-w-[220px] items-center gap-3">
                          <div className="grid h-9 w-9 place-items-center rounded-md bg-red-50 text-[var(--claro-red)]">
                            <FileSpreadsheet size={18} />
                          </div>
                          <div className="min-w-0">
                            <p className="truncate font-semibold text-zinc-800">
                              {proc.nomeArquivo}
                            </p>
                            <p className="text-xs text-zinc-500">
                              {proc.tamanho ?? "-"}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-zinc-700">
                        {proc.nomeProcessamento ?? proc.tipoProcessamento}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={cn(
                            "inline-flex rounded-full border px-2.5 py-1 font-semibold text-xs",
                            statusClass(proc.resultadoAmigavel),
                          )}
                        >
                          {proc.resultadoAmigavel ?? "-"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-zinc-700">
                        {proc.usuario ?? "-"}
                      </td>
                      <td className="px-4 py-3 text-zinc-600">
                        {formatDateTime(proc.dataInicio ?? proc.dataStart)}
                      </td>
                      <td className="px-4 py-3 text-zinc-600">
                        {formatDateTime(proc.dataFim)}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => void baixar(proc)}
                            disabled={busyId === proc.id}
                          >
                            <Download size={14} /> Baixar
                          </Button>
                          <Button
                            size="sm"
                            onClick={() => void reprocessar(proc)}
                            disabled={busyId === proc.id}
                          >
                            <RotateCcw size={14} /> Reprocessar
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      <div className="mt-4 flex items-center justify-between">
        <Button
          variant="outline"
          disabled={!page || page.first || loading}
          onClick={() => setPageNumber((p) => Math.max(0, p - 1))}
        >
          Anterior
        </Button>
        <span className="text-sm text-zinc-500">
          Pagina {(page?.number ?? pageNumber) + 1} de{" "}
          {Math.max(page?.totalPages ?? 1, 1)}
        </span>
        <Button
          variant="outline"
          disabled={!page || page.last || loading}
          onClick={() => setPageNumber((p) => p + 1)}
        >
          Proxima
        </Button>
      </div>
    </div>
  );
}
