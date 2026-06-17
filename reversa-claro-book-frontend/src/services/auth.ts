import api from "./api";
import type { LoginData, TokenResponse } from "@/types";

export async function login(email: string, senha: string): Promise<TokenResponse> {
  const data: LoginData = { email, senha };
  const response = await api.post<TokenResponse>("/autenticacao/login", data);
  return response.data;
}

export async function logout(): Promise<void> {
  // Backend revoga o jti server-side; o cliente limpa o storage independentemente
  // do resultado (token já expirado faz a chamada falhar, mas o logout local segue válido).
  await api.post("/autenticacao/logout");
}
