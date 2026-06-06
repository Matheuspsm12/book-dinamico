import { api } from "./client";
import type { TokenResponse } from "./types";

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

/** "Esqueci minha senha" — backend responde sempre 204 (não revela se o e-mail existe). */
export async function esqueciSenha(email: string) {
  return api.post<void>("/autenticacao/esqueci-senha", { body: { email } });
}

/** Redefine a senha a partir do token recebido por e-mail. */
export async function redefinirSenha(token: string, novaSenha: string) {
  return api.post<void>("/autenticacao/redefinir-senha", { body: { token, novaSenha } });
}

/** "Quero manter meu acesso" — confirma interesse via token do e-mail de ociosidade. */
export async function manterAcesso(token: string) {
  return api.post<void>("/autenticacao/manter-acesso", { body: { token } });
}
