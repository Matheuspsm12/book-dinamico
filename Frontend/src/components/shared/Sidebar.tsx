"use client";

import {
  ChevronRight,
  Download,
  LayoutDashboard,
  ListChecks,
  LogOut,
  Upload,
  Users,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";

import { useAuth } from "src/app/contexts/AuthContext";
import { cn } from "src/lib/utils";

type Item = {
  label: string;
  href: string;
  icon: React.ComponentType<{ size?: number }>;
  roles: ("ADMIN" | "USUARIO")[];
};

const items: Item[] = [
  {
    label: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
    roles: ["ADMIN"],
  },
  {
    label: "Book Dinâmico",
    href: "/book",
    icon: Download,
    roles: ["ADMIN", "USUARIO"],
  },
  {
    label: "Upload Book",
    href: "/upload-book",
    icon: Upload,
    roles: ["ADMIN"],
  },
  {
    label: "Processamentos",
    href: "/processamentos",
    icon: ListChecks,
    roles: ["ADMIN"],
  },
  {
    label: "Gerenciar Usuários",
    href: "/gerenciar-usuarios",
    icon: Users,
    roles: ["ADMIN"],
  },
];

export function Sidebar() {
  const [open, setOpen] = useState(true);
  const pathname = usePathname();
  const { user, signOut } = useAuth();

  if (!user) return null;
  const visible = items.filter((i) => i.roles.includes(user.role));

  return (
    <aside
      className={cn(
        "fixed top-0 left-0 z-30 flex h-full flex-col justify-between border-zinc-200 border-r bg-[var(--sidebar-bg)] shadow-sm transition-all duration-200",
        open ? "w-64" : "w-20",
      )}
    >
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="absolute top-20 -right-3 z-10 grid h-6 w-6 place-items-center rounded-full bg-white shadow ring-1 ring-zinc-200"
        aria-label="toggle sidebar"
      >
        <ChevronRight
          size={14}
          className={cn("transition", open && "rotate-180")}
        />
      </button>

      <div>
        <div className="flex h-16 items-center gap-2 border-zinc-200/70 border-b px-3">
          <img
            src="/img/logo_symbol_c.svg"
            alt="Claro"
            className={cn("ml-1", open ? "max-w-10" : "max-w-8")}
          />
          {open && (
            <div className="ml-2 border-zinc-300 border-l pl-3">
              <p className="font-bold text-[10px] text-zinc-700 uppercase leading-tight">
                Book Dinâmico
                <br />
                Claro / Logística
              </p>
            </div>
          )}
        </div>

        <nav className="mt-4 px-2">
          {open && (
            <p className="mb-2 px-3 font-semibold text-[10px] text-zinc-500 uppercase tracking-wider">
              Navegação
            </p>
          )}
          <ul className="space-y-1">
            {visible.map(({ href, label, icon: Icon }) => {
              const active =
                pathname === href || pathname.startsWith(`${href}/`);
              return (
                <li key={href}>
                  <Link
                    href={href}
                    className={cn(
                      "flex items-center gap-3 rounded-md px-3 py-2 font-medium text-sm transition",
                      active
                        ? "bg-[var(--claro-red)] text-white shadow-sm"
                        : "text-zinc-700 hover:bg-white hover:text-[var(--claro-red)]",
                      !open && "justify-center",
                    )}
                    title={label}
                  >
                    <Icon size={20} />
                    {open && <span className="truncate">{label}</span>}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>
      </div>

      <div className="border-zinc-200/70 border-t p-3">
        <button
          type="button"
          onClick={() => void signOut()}
          className={cn(
            "mb-3 flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-zinc-600 hover:bg-white hover:text-[var(--claro-red)]",
            !open && "justify-center",
          )}
        >
          <LogOut size={18} />
          {open && <span>Sair</span>}
        </button>

        <div className="flex items-center justify-start gap-2 px-1 text-zinc-500">
          <img src="/img/logo_tcia_black.svg" alt="TCIA" className="w-12" />
          {open && (
            <div className="ml-2 border-zinc-300 border-l pl-3">
              <p className="text-[10px]">tciagroup.com / 2025</p>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
}
