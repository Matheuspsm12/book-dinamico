"use client";

import {
  CheckCircle2,
  ChevronDown,
  KeyRound,
  LogOut,
  Mail,
  User as UserIcon,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { Button } from "src/components/ui/button";
import { Dialog } from "src/components/ui/dialog";
import { useAuth } from "src/app/contexts/AuthContext";
import { cn } from "src/lib/utils";
import * as usuariosApi from "src/services/usuarios-service";

export function UserMenu() {
  const { user, signOut } = useAuth();
  const [open, setOpen] = useState(false);
  const [showSenha, setShowSenha] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);

  // Fecha ao clicar fora
  useEffect(() => {
    function onDoc(ev: MouseEvent) {
      if (!wrapRef.current?.contains(ev.target as Node)) setOpen(false);
    }
    if (open) document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  if (!user) return null;

  return (
    <>
      <div ref={wrapRef} className="relative">
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          className="flex items-center gap-2 rounded-md px-2 py-1 text-right transition hover:bg-zinc-100"
        >
          <div className="grid h-9 w-9 place-items-center rounded-full bg-zinc-200 text-zinc-600">
            <UserIcon size={18} />
          </div>
          <div>
            <p className="font-semibold text-sm text-zinc-800 leading-tight">
              {user.nome}
            </p>
            <p className="text-xs text-zinc-500 leading-tight">
              {user.role === "ADMIN" ? "Administrador" : "Usuário"}
            </p>
          </div>
          <ChevronDown
            size={16}
            className={cn("text-zinc-400 transition", open && "rotate-180")}
          />
        </button>

        {open && (
          <div className="absolute top-full right-0 z-50 mt-2 w-56 overflow-hidden rounded-md border border-zinc-200 bg-white shadow-lg">
            <div className="border-zinc-100 border-b px-4 py-3">
              <p className="truncate font-semibold text-sm text-zinc-800">
                {user.nome}
              </p>
              <p className="truncate text-xs text-zinc-500">{user.email}</p>
            </div>
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                setShowSenha(true);
              }}
              className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-zinc-700 hover:bg-zinc-50"
            >
              <KeyRound size={15} /> Alterar senha
            </button>
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                void signOut();
              }}
              className="flex w-full items-center gap-2 border-zinc-100 border-t px-4 py-2.5 text-left text-red-600 text-sm hover:bg-red-50"
            >
              <LogOut size={15} /> Sair
            </button>
          </div>
        )}
      </div>

      {showSenha && (
        <AlterarSenhaModal
          email={user.email}
          onClose={() => setShowSenha(false)}
        />
      )}
    </>
  );
}

function AlterarSenhaModal({
  email,
  onClose,
}: {
  email: string;
  onClose: () => void;
}) {
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [enviado, setEnviado] = useState(false);

  async function enviar() {
    setErr(null);
    setSubmitting(true);
    try {
      await usuariosApi.resetarMinhaSenhaPorEmail();
      setEnviado(true);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao solicitar nova senha.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()} title="Alterar senha">
      {enviado ? (
        <div className="space-y-4 text-center">
          <CheckCircle2 className="mx-auto text-emerald-500" size={48} />
          <p className="text-sm text-zinc-700">
            Enviamos uma nova senha para <strong>{email}</strong>.
            <br />
            Use-a no próximo login e altere quando preferir.
          </p>
          <div className="flex justify-center pt-2">
            <Button onClick={onClose}>Fechar</Button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="rounded-md border border-zinc-200 bg-zinc-50 p-4 text-sm text-zinc-700">
            Ao confirmar, geramos uma <strong>nova senha temporária</strong> e
            enviamos para o seu e-mail cadastrado:
            <div className="mt-2 flex items-center gap-2 rounded-md bg-white px-3 py-2 font-mono text-xs text-zinc-800">
              <Mail size={14} className="text-[var(--claro-red)]" />
              {email}
            </div>
            <p className="mt-3 text-xs text-zinc-500">
              Sua senha atual será invalidada imediatamente. Se não receber o
              e-mail em 5 minutos, verifique a caixa de spam.
            </p>
          </div>

          {err && (
            <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
              {err}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={submitting}
            >
              Cancelar
            </Button>
            <Button onClick={enviar} disabled={submitting}>
              <KeyRound size={14} />{" "}
              {submitting ? "Enviando…" : "Enviar nova senha"}
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  );
}
