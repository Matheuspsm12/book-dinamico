import api from "src/services/api";
import type { AuditoriaResponse, PageResponse } from "src/lib/api/types";

export async function listarHistoricoDocumentos(page = 0, size = 50) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const { data } = await api.get<PageResponse<AuditoriaResponse>>(
    `/api/auditoria/documentos?${params.toString()}`,
  );
  return data;
}

export async function listarHistoricoUsuarios(page = 0, size = 50) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const { data } = await api.get<PageResponse<AuditoriaResponse>>(
    `/api/auditoria/usuarios?${params.toString()}`,
  );
  return data;
}
