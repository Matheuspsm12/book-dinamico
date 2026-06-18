import { api } from "src/lib/api/client";
import type {
  DocumentoMetadataRequest,
  DocumentoResponse,
} from "src/lib/api/types";

export async function listar() {
  return api.get<DocumentoResponse[]>("/api/documentos");
}

export async function buscar(id: number) {
  return api.get<DocumentoResponse>(`/api/documentos/${id}`);
}

export async function criar(metadata: DocumentoMetadataRequest, arquivo: File) {
  const form = new FormData();
  // Spring exige metadata como part JSON — anexamos como Blob com tipo correto
  form.append(
    "metadata",
    new Blob([JSON.stringify(metadata)], { type: "application/json" }),
  );
  form.append("arquivo", arquivo);
  return api.post<DocumentoResponse>("/api/documentos", { body: form });
}

export async function substituirArquivo(id: number, arquivo: File) {
  const form = new FormData();
  form.append("arquivo", arquivo);
  return api.put<DocumentoResponse>(`/api/documentos/${id}/arquivo`, {
    body: form,
  });
}

export async function atualizarMetadados(
  id: number,
  metadata: DocumentoMetadataRequest,
) {
  return api.put<DocumentoResponse>(`/api/documentos/${id}`, {
    body: metadata,
  });
}

export async function deletar(id: number) {
  return api.delete<void>(`/api/documentos/${id}`);
}

/**
 * Baixa o binário do documento como Blob. Necessário porque o endpoint exige
 * cabeçalho Authorization (não dá pra usar <a href> simples).
 */
export async function baixar(id: number): Promise<Blob> {
  return api.get<Blob>(`/api/documentos/${id}/download`, { asBlob: true });
}

/**
 * Dispara o download no navegador a partir de um Blob.
 */
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
