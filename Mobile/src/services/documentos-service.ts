import type { DocumentoResponse } from "../types/api";
import api, { requireApiUrl } from "./api";

export async function listarDocumentos() {
  requireApiUrl();
  const { data } = await api.get<DocumentoResponse[]>("/api/documentos");
  return data;
}
