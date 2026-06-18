// Persistência do token + perfil mínimo do usuário logado.
// Modo "permanecerLogado" usa localStorage; senão sessionStorage (some ao fechar a aba).

import type { UsuarioRole } from "./api/types";

const TOKEN_KEY = "rcb_token";
const USER_KEY = "rcb_user";

export interface AuthSession {
  token: string;
  expiraEm: string;
  nome: string;
  email: string;
  role: UsuarioRole;
}

function pickStorage(): Storage | null {
  if (typeof window === "undefined") return null;
  // Se a chave existir em localStorage, usar localStorage; senão sessionStorage.
  if (window.localStorage.getItem(TOKEN_KEY)) return window.localStorage;
  return window.sessionStorage;
}

export function saveSession(session: AuthSession, persistente: boolean) {
  if (typeof window === "undefined") return;
  clearSession();
  const storage = persistente ? window.localStorage : window.sessionStorage;
  storage.setItem(TOKEN_KEY, session.token);
  storage.setItem(USER_KEY, JSON.stringify(session));
}

export function getSession(): AuthSession | null {
  const storage = pickStorage();
  if (!storage) return null;
  const raw = storage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    return null;
  }
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return (
    window.localStorage.getItem(TOKEN_KEY) ??
    window.sessionStorage.getItem(TOKEN_KEY)
  );
}

export function clearSession() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
  window.sessionStorage.removeItem(TOKEN_KEY);
  window.sessionStorage.removeItem(USER_KEY);
}
