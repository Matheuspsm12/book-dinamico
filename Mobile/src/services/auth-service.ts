import api, { friendlyApiError, requireApiUrl } from "./api";
import type { TokenResponse } from "../types/api";

export async function login(email: string, senha: string) {
  try {
    requireApiUrl();
    const { data } = await api.post<TokenResponse>("/autenticacao/login", {
      email,
      senha,
    });
    return data;
  } catch (error) {
    const detail = error instanceof Error && error.message
      ? error.message
      : "Não foi possível entrar.";
    if (__DEV__) console.error("[mobile-auth] Falha no login", error);
    throw new Error(friendlyApiError(error, detail));
  }
}
