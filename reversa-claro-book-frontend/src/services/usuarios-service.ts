import { api } from "src/lib/api/client";
import type {
  PageResponse,
  UsuarioCadastroRequest,
  UsuarioEdicaoRequest,
  UsuarioFiltroRequest,
  UsuarioResponse,
} from "src/lib/api/types";

export async function cadastrar(input: UsuarioCadastroRequest) {
  return api.post<UsuarioResponse>("/api/usuarios/cadastro", { body: input });
}

export async function paginar(
  filtro: UsuarioFiltroRequest | undefined,
  page = 0,
  size = 50,
  sort?: string,
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (sort) params.set("sort", sort);
  return api.post<PageResponse<UsuarioResponse>>(
    `/api/usuarios/paginar?${params.toString()}`,
    { body: filtro ?? {} },
  );
}

export async function aprovar(id: number) {
  return api.post<UsuarioResponse>(`/api/usuarios/${id}/aprovar`);
}

export async function rejeitar(id: number) {
  return api.post<UsuarioResponse>(`/api/usuarios/${id}/rejeitar`);
}

export async function atualizar(id: number, patch: UsuarioEdicaoRequest) {
  return api.put<UsuarioResponse>(`/api/usuarios/${id}`, { body: patch });
}

export async function ativar(id: number) {
  return api.post<UsuarioResponse>(`/api/usuarios/${id}/ativar`);
}

export async function desativar(id: number) {
  return api.post<UsuarioResponse>(`/api/usuarios/${id}/desativar`);
}

/** Solicita reset de senha do usuário autenticado — backend envia nova senha por e-mail. */
export async function resetarMinhaSenhaPorEmail() {
  return api.post<void>("/api/usuarios/me/resetar-senha");
}
