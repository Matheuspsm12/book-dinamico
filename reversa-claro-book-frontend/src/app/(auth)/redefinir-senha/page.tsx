"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Eye, EyeOff, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import * as authApi from "@/lib/api/auth";

export default function RedefinirSenhaPage() {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [tokenLido, setTokenLido] = useState(false);
  const [senha, setSenha] = useState("");
  const [confirma, setConfirma] = useState("");
  const [mostrar, setMostrar] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [ok, setOk] = useState(false);
  const [loading, setLoading] = useState(false);

  // Lê o token da query string no client (evita exigência de Suspense do useSearchParams).
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setToken(params.get("token"));
    setTokenLido(true);
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    if (senha.length < 8) {
      setErr("A senha deve ter no mínimo 8 caracteres.");
      return;
    }
    if (senha !== confirma) {
      setErr("As senhas não coincidem.");
      return;
    }
    if (!token) {
      setErr("Link inválido. Solicite uma nova redefinição.");
      return;
    }
    setLoading(true);
    try {
      await authApi.redefinirSenha(token, senha);
      setOk(true);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao redefinir a senha.");
    } finally {
      setLoading(false);
    }
  }

  if (ok) {
    return (
      <div className="text-center">
        <CheckCircle2 className="mx-auto text-emerald-500" size={56} />
        <h1 className="mt-4 text-2xl font-bold text-zinc-800">Senha redefinida!</h1>
        <p className="mt-2 text-sm text-zinc-500">
          Sua senha foi alterada com sucesso. Você já pode entrar com a nova senha.
        </p>
        <Button className="mt-8 w-full" size="lg" onClick={() => router.replace("/login")}>
          Ir para o login
        </Button>
      </div>
    );
  }

  if (tokenLido && !token) {
    return (
      <div className="text-center">
        <h1 className="text-2xl font-bold text-zinc-800">Link inválido</h1>
        <p className="mt-2 text-sm text-zinc-500">
          O link de redefinição é inválido ou está incompleto. Solicite um novo.
        </p>
        <Link
          href="/esqueci-senha"
          className="mt-8 inline-block font-semibold text-[var(--claro-red)] hover:underline"
        >
          Solicitar novo link
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-zinc-800">Redefinir senha</h1>
      <p className="mt-1 text-sm text-zinc-500">Escolha uma nova senha para sua conta.</p>

      <form onSubmit={onSubmit} className="mt-6 space-y-4">
        <div className="relative space-y-1.5">
          <Label htmlFor="senha">Nova senha (mínimo 8 caracteres)</Label>
          <div className="relative">
            <Input
              id="senha"
              type={mostrar ? "text" : "password"}
              required
              minLength={8}
              maxLength={72}
              className="pr-10"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
            />
            <button
              type="button"
              onClick={() => setMostrar(!mostrar)}
              className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center justify-center w-8 h-8 text-zinc-400 hover:text-zinc-700 transition-colors"
              aria-label={mostrar ? "Esconder senha" : "Mostrar senha"}
            >
              {mostrar ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="confirma">Confirmar nova senha</Label>
          <Input
            id="confirma"
            type={mostrar ? "text" : "password"}
            required
            minLength={8}
            maxLength={72}
            value={confirma}
            onChange={(e) => setConfirma(e.target.value)}
          />
        </div>

        {err && (
          <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {err}
          </div>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={loading || !tokenLido}>
          {loading ? "Salvando..." : "Redefinir senha"}
        </Button>
      </form>
    </div>
  );
}
