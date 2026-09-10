import api from "src/services/api";
import type { LogConsultaResponse } from "src/lib/api/types";

export interface BuscarLogsParams {
  /** Quantidade de linhas (10–2000). Default do backend: 300. */
  linhas?: number;
  /** Nível mínimo: INFO | WARN | ERROR (ou vazio = todos). */
  nivel?: string;
  /** Busca textual em mensagem/logger/stacktrace. */
  busca?: string;
  /** Nome de um arquivo rotacionado; vazio = arquivo ativo. */
  arquivo?: string;
}

export async function buscarLogs(
  params: BuscarLogsParams = {},
): Promise<LogConsultaResponse> {
  const qs = new URLSearchParams();
  if (params.linhas) qs.set("linhas", String(params.linhas));
  if (params.nivel) qs.set("nivel", params.nivel);
  if (params.busca?.trim()) qs.set("busca", params.busca.trim());
  if (params.arquivo) qs.set("arquivo", params.arquivo);

  const suffix = qs.toString() ? `?${qs.toString()}` : "";
  const { data } = await api.get<LogConsultaResponse>(
    `/api/admin/diagnostico/logs${suffix}`,
  );
  return data;
}
