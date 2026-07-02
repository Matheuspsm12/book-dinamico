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
  await api.post("/autenticacao/logout");
}

export async function recuperarSenha(email: string) {
  await api.post("/autenticacao/recuperar-senha", { email });
}

export async function redefinirSenha(token: string, novaSenha: string) {
  await api.post("/autenticacao/redefinir-senha", { token, novaSenha });
}
