import api from "src/services/api";
import type {
  PageResponse,
  UsuarioCadastroRequest,
  UsuarioEdicaoRequest,
  UsuarioFiltroRequest,
  UsuarioResponse,
} from "src/lib/api/types";

export async function cadastrar(input: UsuarioCadastroRequest) {
  const { data } = await api.post<UsuarioResponse>(
    "/api/usuarios/cadastro",
    input,
  );
  return data;
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
  const { data } = await api.post<PageResponse<UsuarioResponse>>(
    `/api/usuarios/paginar?${params.toString()}`,
    filtro ?? {},
  );
  return data;
}

export async function aprovar(id: number) {
  const { data } = await api.post<UsuarioResponse>(
    `/api/usuarios/${id}/aprovar`,
  );
  return data;
}

export async function rejeitar(id: number) {
  const { data } = await api.post<UsuarioResponse>(
    `/api/usuarios/${id}/rejeitar`,
  );
  return data;
}

export async function atualizar(id: number, patch: UsuarioEdicaoRequest) {
  const { data } = await api.put<UsuarioResponse>(`/api/usuarios/${id}`, patch);
  return data;
}

export async function ativar(id: number) {
  const { data } = await api.post<UsuarioResponse>(
    `/api/usuarios/${id}/ativar`,
  );
  return data;
}

export async function desativar(id: number) {
  const { data } = await api.post<UsuarioResponse>(
    `/api/usuarios/${id}/desativar`,
  );
  return data;
}

/** Solicita reset de senha do usuário autenticado — backend envia nova senha por e-mail. */
export async function resetarMinhaSenhaPorEmail() {
  await api.post("/api/usuarios/me/resetar-senha");
}
