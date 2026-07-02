"use client";

import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { createContext, useContext, useEffect, useState } from "react";

import type { AuthSession } from "src/lib/auth-storage";
import { clearSession, getSession, saveSession } from "src/lib/auth-storage";
import * as authApi from "src/services/auth-service";

type Ctx = {
  user: AuthSession | null;
  signIn: (
    email: string,
    senha: string,
    persistente: boolean,
  ) => Promise<AuthSession>;
  signOut: () => Promise<void>;
  loading: boolean;
};

const AuthCtx = createContext<Ctx>({
  user: null,
  signIn: async () => {
    throw new Error("AuthContext não inicializado");
  },
  signOut: async () => {},
  loading: true,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    setUser(getSession());
    setLoading(false);
  }, []);

  useEffect(() => {
    if (loading) return;
    const isAuthRoute =
      pathname === "/login" ||
      pathname === "/cadastro" ||
      pathname === "/recuperar-senha";
    if (!user && !isAuthRoute) {
      router.replace("/login");
    } else if (user && isAuthRoute) {
      router.replace(user.role === "ADMIN" ? "/dashboard" : "/book");
    }
  }, [user, pathname, loading, router]);

  async function signIn(email: string, senha: string, persistente: boolean) {
    const token = await authApi.login(email, senha);
    const session: AuthSession = {
      token: token.token,
      expiraEm: token.expiraEm,
      nome: token.nome,
      email: token.email,
      role: token.role,
    };
    saveSession(session, persistente);
    setUser(session);
    return session;
  }

  async function signOut() {
    try {
      await authApi.logout();
    } catch {}
    clearSession();
    setUser(null);
    router.replace("/login");
  }

  return (
    <AuthCtx.Provider value={{ user, signIn, signOut, loading }}>
      {children}
    </AuthCtx.Provider>
  );
}

export const useAuth = () => useContext(AuthCtx);
