import * as SecureStore from "expo-secure-store";

import type { AuthSession } from "../types/api";

const SESSION_KEY = "claro-book-session";

export async function readSession(): Promise<AuthSession | null> {
  const raw = await SecureStore.getItemAsync(SESSION_KEY);
  if (!raw) return null;

  try {
    const session = JSON.parse(raw) as AuthSession;
    const expiresAt = Date.parse(session.expiraEm);
    if (!session.token || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      await clearSession();
      return null;
    }
    return session;
  } catch {
    await clearSession();
    return null;
  }
}

export async function writeSession(session: AuthSession) {
  await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(session));
}

export async function clearSession() {
  await SecureStore.deleteItemAsync(SESSION_KEY);
}
