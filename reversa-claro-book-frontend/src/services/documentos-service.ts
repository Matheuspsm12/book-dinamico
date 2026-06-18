import api from "src/services/api";
import type {
  DocumentoMetadataRequest,
  DocumentoResponse,
} from "src/lib/api/types";

export async function listar() {
  const { data } = await api.get<DocumentoResponse[]>("/api/documentos");
  return data;
}

export async function buscar(id: number) {
  const { data } = await api.get<DocumentoResponse>(`/api/documentos/${id}`);
  return data;
}

export async function criar(metadata: DocumentoMetadataRequest, arquivo: File) {
  const form = new FormData();
  // Spring exige metadata como part JSON — anexamos como Blob com tipo correto
  form.append(
    "metadata",
    new Blob([JSON.stringify(metadata)], { type: "application/json" }),
  );
  form.append("arquivo", arquivo);
  const { data } = await api.post<DocumentoResponse>("/api/documentos", form);
  return data;
}

export async function substituirArquivo(id: number, arquivo: File) {
  const form = new FormData();
  form.append("arquivo", arquivo);
  const { data } = await api.put<DocumentoResponse>(
    `/api/documentos/${id}/arquivo`,
    form,
  );
  return data;
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

/**
 * Baixa o binário do documento como Blob. Necessário porque o endpoint exige
 * cabeçalho Authorization (não dá pra usar <a href> simples).
 */
export async function baixar(id: number): Promise<Blob> {
  const { data } = await api.get(`/api/documentos/${id}/download`, {
    responseType: "blob",
  });
  return data as Blob;
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
