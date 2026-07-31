"use client";

import { FileText, Users } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import { PageHeader } from "src/components/shared/PageHeader";
import { Card, CardContent } from "src/components/ui/card";
import { useAuth } from "src/app/contexts/AuthContext";
import type { AuditoriaResponse } from "src/lib/api/types";
import { cn, formatDateTime } from "src/lib/utils";
import * as auditoriaApi from "src/services/auditoria-service";

const ACAO_LABEL: Record<string, string> = {
  CRIAR: "Criação",
  ALTERAR: "Alteração",
  SUBSTITUIR: "Substituição",
  EXCLUIR: "Exclusão",
  INATIVIDADE: "Inatividade",
  PROCESSAR: "Processamento",
};

// No histórico do book, "ALTERAR" é edição de metadados.
const ACAO_LABEL_BOOK: Record<string, string> = {
  ...ACAO_LABEL,
  ALTERAR: "Edição",
};

const ACAO_BADGE: Record<string, string> = {
  CRIAR: "bg-emerald-100 text-emerald-700",
  ALTERAR: "bg-blue-100 text-blue-700",
  SUBSTITUIR: "bg-amber-100 text-amber-700",
  EXCLUIR: "bg-red-100 text-red-700",
  INATIVIDADE: "bg-purple-100 text-purple-700",
  PROCESSAR: "bg-zinc-100 text-zinc-600",
};

type Aba = "book" | "usuario";

export default function HistoricoPage() {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [aba, setAba] = useState<Aba>("book");
  const [itens, setItens] = useState<AuditoriaResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErr(null);
    try {
      const page =
        aba === "book"
          ? await auditoriaApi.listarHistoricoDocumentos(0, 100)
          : await auditoriaApi.listarHistoricoUsuarios(0, 100);
      setItens(page.content);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao carregar histórico.");
    } finally {
      setCarregando(false);
    }
  }, [aba]);

  useEffect(() => {
    if (!loading && user && user.role !== "ADMIN") router.replace("/book");
  }, [user, loading, router]);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    void carregar();
  }, [user, carregar]);

  if (loading || !user || user.role !== "ADMIN") return null;

  const isBook = aba === "book";
  const labelMap = isBook ? ACAO_LABEL_BOOK : ACAO_LABEL;
  const primeiraColuna = isBook ? "Documento" : "Usuário";

  return (
    <div>
      <PageHeader
        title="Histórico"
        subtitle="Registro de quem fez o quê e quando — no book e nos usuários."
      />

      <Card>
        <CardContent className="pt-6">
          <div className="mb-5 flex gap-2">
            <TabButton
              ativo={isBook}
              onClick={() => setAba("book")}
              icon={<FileText size={15} />}
              label="Book (documentos)"
            />
            <TabButton
              ativo={!isBook}
              onClick={() => setAba("usuario")}
              icon={<Users size={15} />}
              label="Usuários"
            />
          </div>

          {err && (
            <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
              {err}
            </div>
          )}

          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-zinc-200 border-b text-left text-xs text-zinc-500 uppercase">
                  <th className="py-3 font-semibold">{primeiraColuna}</th>
                  <th className="py-3 font-semibold">Ação</th>
                  <th className="py-3 font-semibold">Feito por</th>
                  <th className="py-3 font-semibold">Data / Hora</th>
                </tr>
              </thead>
              <tbody>
                {carregando ? (
                  <tr>
                    <td colSpan={4} className="py-10 text-center text-zinc-400">
                      Carregando…
                    </td>
                  </tr>
                ) : itens.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="py-10 text-center text-zinc-400">
                      Nenhum registro ainda.
                    </td>
                  </tr>
                ) : (
                  itens.map((h) => (
                    <tr
                      key={h.id}
                      className="border-zinc-100 border-b hover:bg-zinc-50"
                    >
                      <td className="py-3 font-medium text-zinc-800">
                        {h.detalhes || "—"}
                      </td>
                      <td className="py-3">
                        <span
                          className={cn(
                            "rounded-full px-2 py-0.5 font-semibold text-xs",
                            ACAO_BADGE[h.acao] ?? "bg-zinc-100 text-zinc-600",
                          )}
                        >
                          {labelMap[h.acao] ?? h.acao}
                        </span>
                      </td>
                      <td className="py-3 text-zinc-700">{h.usuario}</td>
                      <td className="py-3 text-zinc-500">
                        {formatDateTime(h.dataHora)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <p className="mt-3 text-xs text-zinc-400">{itens.length} registro(s)</p>
        </CardContent>
      </Card>
    </div>
  );
}

function TabButton({
  ativo,
  onClick,
  icon,
  label,
}: {
  ativo: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex items-center gap-2 rounded-md border px-4 py-2 font-medium text-sm transition-colors",
        ativo
          ? "border-[var(--claro-red)] bg-red-50 text-[var(--claro-red)]"
          : "border-zinc-300 bg-white text-zinc-600 hover:bg-zinc-50",
      )}
    >
      {icon}
      {label}
    </button>
  );
}
