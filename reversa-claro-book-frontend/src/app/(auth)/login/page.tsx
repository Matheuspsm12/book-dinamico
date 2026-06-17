"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/app/contexts/AuthContext";

export default function LoginPage() {
  const router = useRouter();
  const { signIn } = useAuth();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [permanecerLogado, setPermanecerLogado] = useState(false);
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      const session = await signIn(email, senha, permanecerLogado);
      router.replace(session.role === "ADMIN" ? "/dashboard" : "/book");
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao entrar.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <div className="mb-4 md:mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-zinc-900 mb-2">Login</h1>
        <p className="text-xs md:text-sm text-zinc-500">
          Entre com seu email e senha para acessar o sistema
        </p>
      </div>

      <form className="space-y-5" onSubmit={onSubmit}>
        <div className="space-y-1.5">
          <Label htmlFor="email" className="text-sm font-bold">
            Nome de usuário ou e-mail
          </Label>
          <Input
            id="email"
            type="text"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="relative space-y-1.5">
          <Label htmlFor="password" className="text-sm font-bold">
            Senha
          </Label>
          <div className="relative">
            <Input
              id="password"
              type={mostrarSenha ? "text" : "password"}
              autoComplete="current-password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
              className="pr-10"
            />
            <button
              type="button"
              onClick={() => setMostrarSenha(!mostrarSenha)}
              className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center justify-center w-8 h-8 text-zinc-400 hover:text-zinc-700 transition-colors"
              aria-label={mostrarSenha ? "Esconder senha" : "Mostrar senha"}
            >
              {mostrarSenha ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <input
            id="permanecerLogado"
            type="checkbox"
            checked={permanecerLogado}
            onChange={(e) => setPermanecerLogado(e.target.checked)}
            className="h-4 w-4 rounded border-zinc-300 accent-[var(--claro-red)]"
          />
          <Label htmlFor="permanecerLogado" className="text-sm font-normal cursor-pointer">
            Permanecer logado
          </Label>
        </div>

        <Button
          type="submit"
          className="w-full bg-zinc-900 text-white hover:bg-zinc-800 py-3 h-auto text-base"
          disabled={loading}
        >
          {loading ? "Carregando..." : "Login"}
        </Button>
      </form>

      {err && (
        <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {err}
        </div>
      )}

      <Link
        href="/cadastro"
        className="block text-center text-sm text-[var(--claro-red)] hover:underline mt-4 font-medium"
      >
        Crie sua conta
      </Link>

      <div className="mt-6 rounded-md bg-zinc-100 p-3 text-xs text-zinc-500">
        <p className="font-semibold text-zinc-700">Conta admin (seed):</p>
        <p>admin@claro.com.br / admin</p>
      </div>
    </>
  );
}
