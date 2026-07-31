import api from "src/services/api";
import type { PerfilResponse } from "src/lib/api/types";

export async function listarPerfis() {
  const { data } = await api.get<PerfilResponse[]>("/api/perfis");
  return data;
}
