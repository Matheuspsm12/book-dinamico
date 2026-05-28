"use client";

import { useAuth } from "@/contexts/AuthContext";
import { User as UserIcon } from "lucide-react";

export function PageHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  const { user } = useAuth();
  return (
    <header className="flex items-start justify-between pb-6">
      <div>
        <h1 className="text-3xl font-bold text-[var(--claro-red)]">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-zinc-500">{subtitle}</p>}
      </div>
      {user && (
        <div className="flex items-center gap-2 text-right">
          <div className="grid h-9 w-9 place-items-center rounded-full bg-zinc-200 text-zinc-600">
            <UserIcon size={18} />
          </div>
          <div>
            <p className="text-sm font-semibold text-zinc-800 leading-tight">{user.nome}</p>
            <p className="text-xs text-zinc-500 leading-tight">
              {user.role === "ADMIN" ? "Administrador" : "Usuário"}
            </p>
          </div>
        </div>
      )}
    </header>
  );
}
