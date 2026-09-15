"use client";

import { usePathname } from "next/navigation";
import { useState } from "react";

import { Sidebar } from "src/components/shared/Sidebar";
import { useAuth } from "src/app/contexts/AuthContext";
import { cn } from "src/lib/utils";

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, loading } = useAuth();
  const pathname = usePathname();
  const fullBleed = pathname === "/book";
  const [sidebarOpen, setSidebarOpen] = useState(true);

  if (loading || !user) return null;
  return (
    <div className="flex min-h-screen">
      <Sidebar open={sidebarOpen} onToggle={() => setSidebarOpen((o) => !o)} />
      <main
        className={cn(
          "flex-1 transition-all duration-200 max-[1024px]:pl-20",
          sidebarOpen ? "pl-64" : "pl-20",
        )}
      >
        {fullBleed ? (
          <div className="min-h-screen">{children}</div>
        ) : (
          <div className="mx-auto max-w-7xl px-8 py-8">{children}</div>
        )}
      </main>
    </div>
  );
}
