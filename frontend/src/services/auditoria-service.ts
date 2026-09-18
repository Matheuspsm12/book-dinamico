import api from "src/services/api";
import { normalizePage, type PagePayload } from "src/lib/api/page";
import type { AuditoriaResponse } from "src/lib/api/types";

export async function listarHistoricoDocumentos(page = 0, size = 50) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const { data } = await api.get<PagePayload<AuditoriaResponse>>(
    `/api/auditoria/documentos?${params.toString()}`,
  );
  return normalizePage<AuditoriaResponse>(data);
}

export async function listarHistoricoUsuarios(page = 0, size = 50) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const { data } = await api.get<PagePayload<AuditoriaResponse>>(
    `/api/auditoria/usuarios?${params.toString()}`,
  );
  return normalizePage<AuditoriaResponse>(data);
}
