import type {
  ProcessamentoResponse,
  ProcessamentoTipo,
} from "src/lib/api/types";
import { normalizePage, type PagePayload } from "src/lib/api/page";
import api from "src/services/api";

export async function listar(page = 0, size = 20) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const { data } = await api.get<PagePayload<ProcessamentoResponse>>(
    `/api/processamentos?${params.toString()}`,
  );
  return normalizePage<ProcessamentoResponse>(data);
}

export async function filtrar(
  tipoProcessamento: ProcessamentoTipo | "",
  page = 0,
  size = 20,
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (tipoProcessamento) {
    params.set("tipoProcessamento", tipoProcessamento);
  }

  const { data } = await api.get<PagePayload<ProcessamentoResponse>>(
    `/api/processamentos/filtrar?${params.toString()}`,
  );
  return normalizePage<ProcessamentoResponse>(data);
}

export async function baixar(id: number): Promise<Blob> {
  const { data } = await api.get(`/api/processamentos/download/${id}`, {
    responseType: "blob",
  });
  return data as Blob;
}

export async function reprocessar(id: number) {
  await api.get(`/api/processamentos/reprocessar/${id}`);
}
