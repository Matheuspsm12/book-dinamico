"use client";

import { useEffect, useState } from "react";
import { Download } from "lucide-react";
import * as docsApi from "@/lib/api/documentos";
import type { DocumentoResponse } from "@/lib/api/types";
import { formatDate } from "@/lib/utils";

function formatoLabel(ext: DocumentoResponse["extensao"]) {
  if (ext === "XLSM" || ext === "XLSX") return "EXCEL";
  return "POWER POINT";
}

function BookCard({
  doc,
  onDownload,
  baixando,
}: {
  doc: DocumentoResponse;
  onDownload: (d: DocumentoResponse) => void;
  baixando: boolean;
}) {
  return (
    <div className="flex flex-col items-center">
      <div className="claro-card flex w-[220px] flex-col items-center justify-between px-6 py-7 text-center">
        <div className="flex h-[110px] flex-col items-center justify-center">
          <p className="text-base font-extrabold uppercase leading-tight tracking-wide">
            {doc.nome}
          </p>
          <p className="mt-3 text-sm font-semibold uppercase opacity-90">
            {formatoLabel(doc.extensao)}
          </p>
        </div>
        <button
          onClick={() => onDownload(doc)}
          disabled={baixando}
          className="baixar-arrow mt-4 flex items-center gap-2 bg-white px-6 py-2 pl-7 text-sm font-bold uppercase tracking-wide text-zinc-800 transition hover:bg-zinc-100 disabled:opacity-50"
        >
          <Download size={14} />
          {baixando ? "Baixando..." : "Baixar"}
        </button>
      </div>
      <div className="mt-3 text-center text-[11px] uppercase text-zinc-500">
        <p>Última</p>
        <p>Atualização</p>
        <p className="font-semibold text-zinc-700">{formatDate(doc.dataAtualizacao)}</p>
      </div>
    </div>
  );
}

export default function BookDownloadPage() {
  const [docs, setDocs] = useState<DocumentoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [baixandoId, setBaixandoId] = useState<number | null>(null);

  useEffect(() => {
    let mounted = true;
    docsApi
      .listar()
      .then((d) => {
        if (mounted) setDocs(d);
      })
      .catch((e) => {
        if (mounted) setErr(e instanceof Error ? e.message : "Erro ao carregar documentos.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, []);

  async function handleDownload(doc: DocumentoResponse) {
    setErr(null);
    setBaixandoId(doc.id);
    try {
      const blob = await docsApi.baixar(doc.id);
      const filename =
        doc.nome.replace(/\s+/g, "_") + "." + doc.extensao.toLowerCase();
      docsApi.salvarBlob(blob, filename);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao baixar.");
    } finally {
      setBaixandoId(null);
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-white">
      <img
        src="/img/bg_claro.svg"
        alt=""
        aria-hidden
        className="pointer-events-none absolute -left-32 top-1/4 w-[600px] opacity-95"
      />

      <div className="relative z-10 grid min-h-screen grid-cols-[1fr_320px]">
        <div className="flex flex-col px-12 py-10">
          <div className="flex justify-center">
            <img src="/img/logo_claro.svg" alt="Claro" className="h-16 w-auto" />
          </div>
          <div className="mx-auto mt-2 h-px w-40 bg-[var(--claro-red)]/40" />

          <div className="flex flex-1 items-center justify-center">
            {loading ? (
              <p className="text-zinc-500">Carregando documentos…</p>
            ) : docs.length === 0 ? (
              <p className="text-zinc-500">
                Nenhum documento disponível ainda. Aguarde o admin publicar.
              </p>
            ) : (
              <div className="flex flex-wrap items-start justify-center gap-10">
                {docs.map((d) => (
                  <BookCard
                    key={d.id}
                    doc={d}
                    onDownload={handleDownload}
                    baixando={baixandoId === d.id}
                  />
                ))}
              </div>
            )}
          </div>

          {err && (
            <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">
              {err}
            </div>
          )}

          <div className="-mx-12 bg-zinc-900 px-12 py-4 text-center text-sm font-bold uppercase tracking-[0.2em] text-white">
            Conectando dados, impulsionando resultados
          </div>
        </div>

        <aside className="flex flex-col justify-between bg-[var(--claro-red)] px-8 py-12 text-white">
          <div className="flex w-full flex-col items-center">
            <h3 className="w-full text-center text-base font-bold uppercase tracking-wide">
              Última Atualização
            </h3>
            <div className="mt-6 w-full max-w-[240px] rounded-md bg-white/15 p-5 text-xs text-white">
              {docs.length === 0 && (
                <p className="text-center opacity-80">Nenhum documento publicado.</p>
              )}
              {docs.map((d) => (
                <div key={d.id} className="mb-4 text-center last:mb-0">
                  <p className="font-bold uppercase">{d.nome}</p>
                  <p className="mt-0.5 opacity-80">{d.descricao}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="flex w-full flex-col items-center">
            <img src="/img/TCIA_white.svg" alt="TCIA" className="h-14 w-auto" />
            <p className="mt-2 text-[10px] uppercase tracking-widest opacity-80">
              tciagroup.com
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
