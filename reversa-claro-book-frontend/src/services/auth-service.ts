import { api } from "src/lib/api/client";
import type { TokenResponse } from "src/lib/api/types";

export async function login(email: string, senha: string) {
  return api.post<TokenResponse>("/autenticacao/login", {
    body: { email, senha },
  });
}

export async function logout() {
  // Backend revoga o jti server-side; cliente vai limpar storage independentemente
  // do resultado da chamada (caso o token já tenha expirado, a chamada falha mas
  // o logout local segue válido).
  return api.post<void>("/autenticacao/logout", { silent401: true });
}
