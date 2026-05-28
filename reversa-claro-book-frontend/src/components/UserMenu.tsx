"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronDown, KeyRound, LogOut, User as UserIcon } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

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
          onClick={() => setOpen((v) => !v)}
          className="flex items-center gap-2 rounded-md px-2 py-1 text-right transition hover:bg-zinc-100"
        >
          <div className="grid h-9 w-9 place-items-center rounded-full bg-zinc-200 text-zinc-600">
            <UserIcon size={18} />
          </div>
          <div>
            <p className="text-sm font-semibold leading-tight text-zinc-800">{user.nome}</p>
            <p className="text-xs leading-tight text-zinc-500">
              {user.role === "ADMIN" ? "Administrador" : "Usuário"}
            </p>
          </div>
          <ChevronDown
            size={16}
            className={cn("text-zinc-400 transition", open && "rotate-180")}
          />
        </button>

        {open && (
          <div className="absolute right-0 top-full z-50 mt-2 w-56 overflow-hidden rounded-md border border-zinc-200 bg-white shadow-lg">
            <div className="border-b border-zinc-100 px-4 py-3">
              <p className="truncate text-sm font-semibold text-zinc-800">{user.nome}</p>
              <p className="truncate text-xs text-zinc-500">{user.email}</p>
            </div>
            <button
              onClick={() => {
                setOpen(false);
                setShowSenha(true);
              }}
              className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-zinc-700 hover:bg-zinc-50"
            >
              <KeyRound size={15} /> Alterar senha
            </button>
            <button
              onClick={() => {
                setOpen(false);
                void signOut();
              }}
              className="flex w-full items-center gap-2 border-t border-zinc-100 px-4 py-2.5 text-left text-sm text-red-600 hover:bg-red-50"
            >
              <LogOut size={15} /> Sair
            </button>
          </div>
        )}
      </div>

      {showSenha && <AlterarSenhaModal onClose={() => setShowSenha(false)} />}
    </>
  );
}

function AlterarSenhaModal({ onClose }: { onClose: () => void }) {
  const [atual, setAtual] = useState("");
  const [nova, setNova] = useState("");
  const [conf, setConf] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [info] = useState<string | null>(
    "Funcionalidade ainda não implementada no backend. Quando o endpoint estiver pronto, este formulário fará a chamada.",
  );

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    if (!atual || !nova || !conf) return setErr("Preencha todos os campos.");
    if (nova.length < 6) return setErr("A nova senha precisa ter no mínimo 6 caracteres.");
    if (nova !== conf) return setErr("Confirmação não confere com a nova senha.");
    // TODO: chamar authApi.alterarSenha(atual, nova) quando endpoint existir
    setErr("Endpoint de alteração de senha não implementado.");
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()} title="Alterar senha">
      <form onSubmit={submit} className="space-y-4">
        {info && (
          <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700">
            {info}
          </div>
        )}
        <div>
          <Label htmlFor="pw-atual">Senha atual</Label>
          <Input id="pw-atual" type="password" className="mt-1" value={atual} onChange={(e) => setAtual(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="pw-nova">Nova senha</Label>
          <Input id="pw-nova" type="password" className="mt-1" value={nova} onChange={(e) => setNova(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="pw-conf">Confirmar nova senha</Label>
          <Input id="pw-conf" type="password" className="mt-1" value={conf} onChange={(e) => setConf(e.target.value)} />
        </div>
        {err && <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{err}</div>}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit">
            <KeyRound size={14} /> Alterar
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
