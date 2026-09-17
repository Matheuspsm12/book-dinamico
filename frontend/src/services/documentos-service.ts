import api from "src/services/api";
import {
  logUploadFailed,
  logUploadProgress,
  logUploadStart,
  logUploadSuccess,
} from "src/lib/client-log";
import type {
  DocumentoMetadataRequest,
  DocumentoResponse,
} from "src/lib/api/types";

export type UploadProgressCallback = (percentual: number) => void;

export async function listar() {
  const { data } = await api.get<DocumentoResponse[]>("/api/documentos");
  return data;
}

export async function buscar(id: number) {
  const { data } = await api.get<DocumentoResponse>(`/api/documentos/${id}`);
  return data;
}

export async function criar(
  metadata: DocumentoMetadataRequest,
  arquivo: File,
  onProgress?: UploadProgressCallback,
) {
  const fluxo = "novo";
  const inicio = Date.now();
  logUploadStart(fluxo, arquivo.size, arquivo.name);

  const form = new FormData();
  form.append(
    "metadata",
    new Blob([JSON.stringify(metadata)], { type: "application/json" }),
  );
  form.append("arquivo", arquivo);

  try {
    const { data } = await api.post<DocumentoResponse>("/api/documentos", form, {
      onUploadProgress: registrarProgresso(fluxo, onProgress),
    });
    logUploadSuccess(fluxo, arquivo.size, Date.now() - inicio);
    return data;
  } catch (e) {
    logUploadFailed(
      fluxo,
      arquivo.size,
      Date.now() - inicio,
      e instanceof Error ? e.message : "erro-desconhecido",
    );
    throw e;
  }
}

export async function substituirArquivo(
  id: number,
  arquivo: File,
  opts?: { nome?: string; dataAtualizacao?: string },
  onProgress?: UploadProgressCallback,
) {
  const fluxo = "substituir";
  const inicio = Date.now();
  logUploadStart(fluxo, arquivo.size, arquivo.name);

  const form = new FormData();
  form.append("arquivo", arquivo);
  if (opts?.nome) form.append("nome", opts.nome);
  if (opts?.dataAtualizacao)
    form.append("dataAtualizacao", opts.dataAtualizacao);

  try {
    const { data } = await api.put<DocumentoResponse>(
      `/api/documentos/${id}/arquivo`,
      form,
      {
        onUploadProgress: registrarProgresso(fluxo, onProgress),
      },
    );
    logUploadSuccess(fluxo, arquivo.size, Date.now() - inicio);
    return data;
  } catch (e) {
    logUploadFailed(
      fluxo,
      arquivo.size,
      Date.now() - inicio,
      e instanceof Error ? e.message : "erro-desconhecido",
    );
    throw e;
  }
}

function registrarProgresso(
  fluxo: string,
  onProgress?: UploadProgressCallback,
) {
  return (evt: { loaded: number; total?: number }) => {
    if (!evt.total) return;
    const pct = evt.total > 0 ? evt.loaded / evt.total : 0;
    onProgress?.(pct);
    logUploadProgress(fluxo, pct * 100);
  };
}

export async function atualizarMetadados(
  id: number,
  metadata: DocumentoMetadataRequest,
) {
  const { data } = await api.put<DocumentoResponse>(
    `/api/documentos/${id}`,
    metadata,
  );
  return data;
}

export async function deletar(id: number) {
  await api.delete(`/api/documentos/${id}`);
}

export async function baixar(id: number): Promise<Blob> {
  const { data } = await api.get(`/api/documentos/${id}/download`, {
    responseType: "blob",
  });
  return data as Blob;
}

export function salvarBlob(blob: Blob, nomeArquivo: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = nomeArquivo;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
