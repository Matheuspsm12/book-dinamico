"use client";

import Link from "next/link";
import { useState } from "react";

import { Button } from "src/components/ui/button";
import { Input } from "src/components/ui/input";
import { Label } from "src/components/ui/label";
import { recuperarSenha } from "src/services/auth-service";

export default function RecuperarSenhaPage() {
  const [email, setEmail] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [ok, setOk] = useState(false);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      await recuperarSenha(email);
      setOk(true);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao recuperar senha.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <div className="mb-4 md:mb-6">
        <h1 className="mb-2 font-bold text-xl text-zinc-900 md:text-2xl">
          Recuperar senha
        </h1>
        <p className="text-xs text-zinc-500 md:text-sm">
          Informe seu e-mail e enviaremos uma senha temporária.
        </p>
      </div>

      {ok ? (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-3 text-emerald-700 text-sm">
          Se o e-mail informado estiver cadastrado, enviamos uma senha
          temporária. Verifique sua caixa de entrada e faça login com ela.
        </div>
      ) : (
        <form className="space-y-5" onSubmit={onSubmit}>
          <div className="space-y-1.5">
            <Label htmlFor="email" className="font-bold text-sm">
              E-mail
            </Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <Button
            type="submit"
            className="h-auto w-full bg-zinc-900 py-3 text-base text-white hover:bg-zinc-800"
            disabled={loading}
          >
            {loading ? "Enviando..." : "Enviar senha temporária"}
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
