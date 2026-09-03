import api from "./api";
import type { DocumentoResponse, PageResponse, UsuarioResponse } from "../types/api";

export async function listarDocumentos() {
  const { data } = await api.get<DocumentoResponse[]>("/api/documentos");
  return data;
}

export async function contarUsuarios(status: "PENDENTE" | "APROVADO" | "REJEITADO") {
  const { data } = await api.post<PageResponse<UsuarioResponse>>("/api/usuarios/paginar?page=0&size=1", { status });
  return data.totalElements;
}
