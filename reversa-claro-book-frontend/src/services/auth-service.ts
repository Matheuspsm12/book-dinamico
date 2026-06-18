import api from "src/services/api";
import type { TokenResponse } from "src/lib/api/types";

export async function login(email: string, senha: string) {
  const { data } = await api.post<TokenResponse>("/autenticacao/login", {
    email,
    senha,
  });
  return data;
}

export async function logout() {
  // Backend revoga o jti server-side; o AuthContext limpa o storage independentemente
  // do resultado da chamada (token expirado faz a chamada falhar, mas o logout local vale).
  await api.post("/autenticacao/logout");
}
