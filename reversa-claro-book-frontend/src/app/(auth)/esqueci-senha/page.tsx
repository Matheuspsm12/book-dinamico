"use client";

import { useState } from "react";
import Link from "next/link";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import * as authApi from "@/lib/api/auth";

export default function EsqueciSenhaPage() {
  const [email, setEmail] = useState("");
  const [enviado, setEnviado] = useState(false);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      await authApi.esqueciSenha(email);
      setEnviado(true);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao solicitar redefinição.");
    } finally {
      setLoading(false);
    }
  }

  if (enviado) {
    return (
      <div className="text-center">
        <CheckCircle2 className="mx-auto text-emerald-500" size={56} />
        <h1 className="mt-4 text-2xl font-bold text-zinc-800">Verifique seu e-mail</h1>
        <p className="mt-2 text-sm text-zinc-500">
          Se houver uma conta associada a <strong>{email}</strong>, enviamos um link para
          redefinir a senha. O link expira em 30 minutos.
        </p>
        <Link
          href="/login"
          className="mt-8 inline-block font-semibold text-[var(--claro-red)] hover:underline"
        >
          Voltar para o login
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-zinc-800">Esqueci minha senha</h1>
      <p className="mt-1 text-sm text-zinc-500">
        Informe seu e-mail cadastrado e enviaremos um link para redefinir sua senha.
      </p>

      <form onSubmit={onSubmit} className="mt-6 space-y-4">
        <div>
          <Label htmlFor="email">E-mail</Label>
          <Input
            id="email"
            type="email"
            required
            className="mt-1"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>

        {err && (
          <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {err}
          </div>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={loading}>
          {loading ? "Enviando..." : "Enviar link de redefinição"}
        </Button>

        <p className="text-center text-sm text-zinc-500">
          Lembrou a senha?{" "}
          <Link href="/login" className="font-semibold text-[var(--claro-red)] hover:underline">
            Entrar
          </Link>
        </p>
      </form>
    </div>
  );
}
