"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";

import { Button } from "src/components/ui/button";
import { Input } from "src/components/ui/input";
import { Label } from "src/components/ui/label";
import { redefinirSenha } from "src/services/auth-service";

function RedefinirSenhaForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [senha, setSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [ok, setOk] = useState(false);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);

    if (!token) {
      setErr("Link inválido. Solicite um novo e-mail de recuperação.");
      return;
    }
    if (senha.length < 8) {
      setErr("A senha deve ter no mínimo 8 caracteres.");
      return;
    }
    if (senha !== confirmacao) {
      setErr("As senhas não conferem.");
      return;
    }

    setLoading(true);
    try {
      await redefinirSenha(token, senha);
      setOk(true);
      setTimeout(() => router.replace("/login"), 2500);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao redefinir a senha.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <div className="mb-4 md:mb-6">
        <h1 className="mb-2 font-bold text-xl text-zinc-900 md:text-2xl">
          Redefinir senha
        </h1>
        <p className="text-xs text-zinc-500 md:text-sm">
          Crie uma nova senha para acessar o portal.
        </p>
      </div>

      {ok ? (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-3 text-emerald-700 text-sm">
          Senha redefinida com sucesso! Redirecionando para o login...
        </div>
      ) : (
        <form className="space-y-5" onSubmit={onSubmit}>
          <div className="space-y-1.5">
            <Label htmlFor="senha" className="font-bold text-sm">
              Nova senha
            </Label>
            <Input
              id="senha"
              type="password"
              autoComplete="new-password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="confirmacao" className="font-bold text-sm">
              Confirmar nova senha
            </Label>
            <Input
              id="confirmacao"
              type="password"
              autoComplete="new-password"
              value={confirmacao}
              onChange={(e) => setConfirmacao(e.target.value)}
              required
            />
          </div>

          <Button
            type="submit"
            className="h-auto w-full bg-zinc-900 py-3 text-base text-white hover:bg-zinc-800"
            disabled={loading}
          >
            {loading ? "Redefinindo..." : "Redefinir senha"}
          </Button>

          {err && (
            <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
              {err}
            </div>
          )}
        </form>
      )}

      <Link
        href="/login"
        className="mt-4 block text-center font-medium text-[var(--claro-red)] text-sm hover:underline"
      >
        Voltar para o login
      </Link>
    </>
  );
}

export default function RedefinirSenhaPage() {
  return (
    <Suspense fallback={null}>
      <RedefinirSenhaForm />
    </Suspense>
  );
}
