"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Download, Pencil, RefreshCw, Upload, X } from "lucide-react";
import * as docsApi from "@/services/documentos";
import type { DocumentoResponse } from "@/types";
import { useAuth } from "@/app/contexts/AuthContext";
import { formatDate, cn, hojeLocal, inferNomeFromFilename } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog } from "@/components/ui/dialog";

const ALLOWED = [".xlsm", ".xlsx", ".pptx"] as const;
const MAX_BYTES = 60 * 1024 * 1024;

function formatoLabel(ext: DocumentoResponse["extensao"]) {
  if (ext === "XLSM" || ext === "XLSX") return "EXCEL";
  return "POWER POINT";
}

function validarArquivo(f: File | null): string | null {
  if (!f) return "Selecione um arquivo.";
  const ext = "." + (f.name.split(".").pop()?.toLowerCase() ?? "");
  if (!ALLOWED.includes(ext as (typeof ALLOWED)[number])) {
    return `Extensão inválida (${ext}). Permitidas: ${ALLOWED.join(", ")}`;
  }
  if (f.size > MAX_BYTES) return "Arquivo maior que 60 MB.";
  return null;
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
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const [docs, setDocs] = useState<DocumentoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [baixandoId, setBaixandoId] = useState<number | null>(null);
  const [editando, setEditando] = useState<DocumentoResponse | null>(null);
  const [substituindo, setSubstituindo] = useState<DocumentoResponse | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      setDocs(await docsApi.listar());
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao carregar documentos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

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

        <aside className="flex flex-col bg-[var(--claro-red)] px-6 py-12 text-white">
          <h3 className="text-center text-base font-bold uppercase tracking-wide">
            Última Atualização
          </h3>

          <div className="mt-6 space-y-3">
            {docs.length === 0 && (
              <p className="text-center text-xs opacity-80">Nenhum documento publicado.</p>
            )}
            {docs.map((d) => (
              <div
                key={d.id}
                className="group relative rounded-md bg-white/15 p-4 text-xs text-white"
              >
                <p className="text-center font-bold uppercase">{d.nome}</p>
                <p className="mt-1 text-center opacity-80">{d.descricao}</p>
                <p className="mt-1 text-center text-[10px] uppercase opacity-70">
                  {formatDate(d.dataAtualizacao)}
                </p>

                {isAdmin && (
                  <div className="mt-3 flex justify-center gap-2 border-t border-white/20 pt-3">
                    <button
                      onClick={() => setEditando(d)}
                      className="inline-flex items-center gap-1 rounded bg-white/10 px-2 py-1 text-[10px] font-semibold uppercase hover:bg-white/25"
                      title="Editar informações"
                    >
                      <Pencil size={11} /> Editar
                    </button>
                    <button
                      onClick={() => setSubstituindo(d)}
                      className="inline-flex items-center gap-1 rounded bg-white/10 px-2 py-1 text-[10px] font-semibold uppercase hover:bg-white/25"
                      title="Trocar arquivo"
                    >
                      <RefreshCw size={11} /> Trocar
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </aside>
      </div>

      {editando && (
        <QuickEditModal
          doc={editando}
          onClose={() => setEditando(null)}
          onSaved={async () => {
            setEditando(null);
            await carregar();
          }}
        />
      )}
      {substituindo && (
        <QuickReplaceModal
          doc={substituindo}
          onClose={() => setSubstituindo(null)}
          onSaved={async () => {
            setSubstituindo(null);
            await carregar();
          }}
        />
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Modal: editar metadata (nome/descricao/data)
// ---------------------------------------------------------------------------

function QuickEditModal({
  doc,
  onClose,
  onSaved,
}: {
  doc: DocumentoResponse;
  onClose: () => void;
  onSaved: () => Promise<void>;
}) {
  const [nome, setNome] = useState(doc.nome);
  const [descricao, setDescricao] = useState(doc.descricao);
  const [data, setData] = useState(doc.dataAtualizacao);
  const [err, setErr] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setSubmitting(true);
    try {
      await docsApi.atualizarMetadados(doc.id, {
        nome: nome.trim(),
        descricao: descricao.trim(),
        dataAtualizacao: data,
      });
      await onSaved();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao salvar.");
      setSubmitting(false);
    }
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()} title="Editar documento" size="lg">
      <form onSubmit={submit} className="space-y-4">
        <div>
          <Label htmlFor="qe-nome">Nome</Label>
          <Input id="qe-nome" className="mt-1" value={nome} onChange={(e) => setNome(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="qe-data">Data de atualização</Label>
          <Input id="qe-data" type="date" className="mt-1" value={data} onChange={(e) => setData(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="qe-desc">Descrição</Label>
          <textarea
            id="qe-desc"
            rows={3}
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            className="mt-1 w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm outline-none focus:border-[var(--claro-red)] focus:ring-2 focus:ring-[var(--claro-red)]/20"
          />
        </div>
        {err && <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{err}</div>}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={submitting}>
            <Pencil size={14} /> {submitting ? "Salvando…" : "Salvar"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

// ---------------------------------------------------------------------------
// Modal: trocar arquivo (também atualiza data pra hoje)
// ---------------------------------------------------------------------------

function QuickReplaceModal({
  doc,
  onClose,
  onSaved,
}: {
  doc: DocumentoResponse;
  onClose: () => void;
  onSaved: () => Promise<void>;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    const v = validarArquivo(file);
    if (v) return setErr(v);
    setSubmitting(true);
    try {
      // 1) substitui o binário
      await docsApi.substituirArquivo(doc.id, file!);
      // 2) ajusta a data de atualização pra hoje (backend não faz isso sozinho)
      const hoje = hojeLocal();
      await docsApi.atualizarMetadados(doc.id, {
        nome: doc.nome,
        descricao: doc.descricao,
        dataAtualizacao: hoje,
      });
      await onSaved();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao substituir.");
      setSubmitting(false);
    }
  }

  return (
    <Dialog
      open
      onOpenChange={(o) => !o && onClose()}
      title="Trocar arquivo"
      description={
        <>
          Documento: <strong>{doc.nome}</strong> será substituído pelo documento{" "}
          <strong>{file ? inferNomeFromFilename(file.name) : "selecionado"}</strong>. A data
          de atualização será marcada como hoje automaticamente.
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <div>
          <Label>Novo arquivo</Label>
          <label
            htmlFor="qr-file"
            className={cn(
              "mt-1 flex cursor-pointer items-center justify-center gap-3 rounded-md border-2 border-dashed border-zinc-300 bg-zinc-50 px-4 py-6 text-sm text-zinc-500",
              "hover:border-[var(--claro-red)] hover:bg-red-50/50",
            )}
          >
            <Upload size={20} />
            {file ? (
              <span className="font-semibold text-zinc-800">{file.name}</span>
            ) : (
              <span>
                Clique para escolher — <strong>{ALLOWED.join(", ")}</strong> (até 60 MB)
              </span>
            )}
          </label>
          <input
            ref={inputRef}
            id="qr-file"
            type="file"
            accept={ALLOWED.join(",")}
            className="sr-only"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
        </div>
        {err && <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{err}</div>}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            <X size={14} /> Cancelar
          </Button>
          <Button type="submit" disabled={submitting}>
            <RefreshCw size={14} /> {submitting ? "Enviando…" : "Substituir"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
