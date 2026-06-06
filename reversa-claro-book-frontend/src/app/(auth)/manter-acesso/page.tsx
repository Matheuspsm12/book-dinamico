"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { CheckCircle2 } from "lucide-react";
import * as authApi from "@/lib/api/auth";

type Estado = "carregando" | "ok" | "erro";

export default function ManterAcessoPage() {
  const [estado, setEstado] = useState<Estado>("carregando");
  const [msg, setMsg] = useState<string>("");

  // Confirma automaticamente ao abrir o link do e-mail.
  useEffect(() => {
    const token = new URLSearchParams(window.location.search).get("token");
    if (!token) {
      setEstado("erro");
      setMsg("Link inválido ou incompleto.");
      return;
    }
    authApi
      .manterAcesso(token)
      .then(() => setEstado("ok"))
      .catch((e) => {
        setEstado("erro");
        setMsg(e instanceof Error ? e.message : "Não foi possível confirmar seu acesso.");
      });
  }, []);

  if (estado === "carregando") {
    return (
      <div className="text-center">
        <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-zinc-200 border-t-[var(--claro-red)]" />
        <p className="mt-4 text-sm text-zinc-500">Confirmando seu acesso...</p>
      </div>
    );
  }

  if (estado === "ok") {
    return (
      <div className="text-center">
        <CheckCircle2 className="mx-auto text-emerald-500" size={56} />
        <h1 className="mt-4 text-2xl font-bold text-zinc-800">Acesso mantido!</h1>
        <p className="mt-2 text-sm text-zinc-500">
          Obrigado por confirmar. Seu acesso ao portal continua ativo.
        </p>
        <Link
          href="/login"
          className="mt-8 inline-block font-semibold text-[var(--claro-red)] hover:underline"
        >
          Ir para o login
        </Link>
      </div>
    );
  }

  return (
    <div className="text-center">
      <h1 className="text-2xl font-bold text-zinc-800">Não foi possível confirmar</h1>
      <p className="mt-2 text-sm text-zinc-500">{msg}</p>
      <Link
        href="/login"
        className="mt-8 inline-block font-semibold text-[var(--claro-red)] hover:underline"
      >
        Voltar para o login
      </Link>
    </div>
  );
}
