"use client";

import { Eye, EyeOff } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { Button } from "src/components/ui/button";
import { Input } from "src/components/ui/input";
import { Label } from "src/components/ui/label";
import { useAuth } from "src/contexts/AuthContext";

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
        <h1 className="mb-2 font-bold text-xl text-zinc-900 md:text-2xl">
          Login
        </h1>
        <p className="text-xs text-zinc-500 md:text-sm">
          Entre com seu email e senha para acessar o sistema
        </p>
      </div>

      <form className="space-y-5" onSubmit={onSubmit}>
        <div className="space-y-1.5">
          <Label htmlFor="email" className="font-bold text-sm">
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
          <Label htmlFor="password" className="font-bold text-sm">
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
              className="absolute top-1/2 right-2 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-zinc-400 transition-colors hover:text-zinc-700"
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
          <Label
            htmlFor="permanecerLogado"
            className="cursor-pointer font-normal text-sm"
          >
            Permanecer logado
          </Label>
        </div>

        <Button
          type="submit"
          className="h-auto w-full bg-zinc-900 py-3 text-base text-white hover:bg-zinc-800"
          disabled={loading}
        >
          {loading ? "Carregando..." : "Login"}
        </Button>
      </form>

      {err && (
        <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
          {err}
        </div>
      )}

      <Link
        href="/cadastro"
        className="mt-4 block text-center font-medium text-[var(--claro-red)] text-sm hover:underline"
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
