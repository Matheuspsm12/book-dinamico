"use client";

import { UserMenu } from "@/components/UserMenu";

export function PageHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <header className="flex items-start justify-between pb-6">
      <div>
        <h1 className="text-3xl font-bold text-[var(--claro-red)]">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-zinc-500">{subtitle}</p>}
      </div>
      <UserMenu />
    </header>
  );
}
