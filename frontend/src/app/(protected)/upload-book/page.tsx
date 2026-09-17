"use client";

import {
  AlertTriangle,
  CheckCircle2,
  FileSpreadsheet,
  FileText,
  HardDrive,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  Upload,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

import { PageHeader } from "src/components/shared/PageHeader";
import { Button } from "src/components/ui/button";
import { Card, CardContent } from "src/components/ui/card";
import { Dialog } from "src/components/ui/dialog";
import { Input } from "src/components/ui/input";
import { Label } from "src/components/ui/label";
import { useAuth } from "src/app/contexts/AuthContext";
import type { DocumentoResponse } from "src/lib/api/types";
import { cn, formatDate, hojeLocal, inferNomeFromFilename } from "src/lib/utils";
import * as docsApi from "src/services/documentos-service";

const ALLOWED = [
  ".xlsm",
  ".xlsx",
  ".xlsb",
  ".xltx",
  ".xltm",
  ".pptx",
] as const;
const MAX_BYTES = 60 * 1024 * 1024;
/** Teto total de armazenamento agregado de todos os documentos. */
const MAX_TOTAL_BYTES = 2 * 1024 * 1024 * 1024; // 2 GB
/** A partir deste percentual exibimos alerta antecipado. */
const AVISO_PERCENTUAL = 0.9;

type Modal =
  | { kind: "none" }
  | { kind: "novo" }
  | { kind: "substituir"; doc: DocumentoResponse }
  | { kind: "editar"; doc: DocumentoResponse }
  | { kind: "excluir"; doc: DocumentoResponse };

function extLabel(ext: DocumentoResponse["extensao"]) {
  return ext;
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024)
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

function validarArquivo(f: File | null): string | null {
  if (!f) return "Selecione um arquivo.";
  const ext = `.${f.name.split(".").pop()?.toLowerCase() ?? ""}`;
  if (!ALLOWED.includes(ext as (typeof ALLOWED)[number])) {
    return `Extensão inválida (${ext}). Permitidas: ${ALLOWED.join(", ")}`;
  }
  if (f.size > MAX_BYTES) return "Arquivo maior que 60 MB.";
  return null;
}

export default function UploadBookPage() {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [docs, setDocs] = useState<DocumentoResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [modal, setModal] = useState<Modal>({ kind: "none" });
  const [flash, setFlash] = useState<{
    tipo: "ok" | "err";
    msg: string;
  } | null>(null);

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      setDocs(await docsApi.listar());
    } catch (e) {
      setFlash({
        tipo: "err",
        msg: e instanceof Error ? e.message : "Erro ao listar.",
      });
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    if (!loading && user && user.role !== "ADMIN") router.replace("/book");
    void carregar();
  }, [user, loading, router, carregar]);

  function showOk(msg: string) {
    setFlash({ tipo: "ok", msg });
    setTimeout(() => setFlash(null), 5000);
  }
  function showErr(msg: string) {
    setFlash({ tipo: "err", msg });
  }

  async function onClosed() {
    setModal({ kind: "none" });
    await carregar();
  }

  if (loading || !user) return null;

  const totalBytes = docs.reduce((acc, d) => acc + d.tamanhoBytes, 0);
  const disponivel = Math.max(0, MAX_TOTAL_BYTES - totalBytes);
  const cheio = totalBytes >= MAX_TOTAL_BYTES;

  return (
    <div>
      <PageHeader
        title="Upload do Book"
        subtitle="Gerencie os documentos publicados no portal."
      />

      {!carregando && (
        <StorageMeter totalBytes={totalBytes} docs={docs} />
      )}

      <div className="mb-5 flex items-center justify-between">
        <p className="text-sm text-zinc-500">
          {carregando
            ? "Carregando…"
            : `${docs.length} documento(s) publicado(s)`}
        </p>
        <Button
          onClick={() => setModal({ kind: "novo" })}
          disabled={carregando || cheio}
          title={
            cheio
              ? "Limite de 2 GB atingido — exclua documentos para liberar espaço."
              : undefined
          }
        >
          <Plus size={16} /> Novo documento
        </Button>
      </div>

      {flash && (
        <div
          className={cn(
            "mb-4 flex items-center gap-2 rounded-md border px-3 py-2 text-sm",
            flash.tipo === "ok"
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-red-200 bg-red-50 text-red-700",
          )}
        >
          {flash.tipo === "ok" && <CheckCircle2 size={16} />}
          <span>{flash.msg}</span>
        </div>
      )}

      {carregando ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-zinc-400">
            Carregando documentos…
          </CardContent>
        </Card>
      ) : docs.length === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-zinc-500">
            Nenhum documento publicado ainda. Clique em{" "}
            <strong>Novo documento</strong> para criar o primeiro.
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {docs.map((d) => (
            <DocCard
              key={d.id}
              doc={d}
              onSubstituir={() => setModal({ kind: "substituir", doc: d })}
              onEditar={() => setModal({ kind: "editar", doc: d })}
              onExcluir={() => setModal({ kind: "excluir", doc: d })}
            />
          ))}
        </div>
      )}

      <NovoModal
        open={modal.kind === "novo"}
        disponivel={disponivel}
        onClose={() => setModal({ kind: "none" })}
        onSuccess={async (d) => {
          showOk(`Documento "${d.nome}" criado.`);
          await onClosed();
        }}
        onError={showErr}
      />

      {modal.kind === "substituir" && (
        <SubstituirArquivoModal
          doc={modal.doc}
          open
          onClose={() => setModal({ kind: "none" })}
          onSuccess={async (d) => {
            showOk(`Arquivo de "${d.nome}" substituído.`);
            await onClosed();
          }}
          onError={showErr}
        />
      )}

      {modal.kind === "editar" && (
        <EditarMetadadosModal
          doc={modal.doc}
          open
          onClose={() => setModal({ kind: "none" })}
          onSuccess={async (d) => {
            showOk(`Metadados de "${d.nome}" atualizados.`);
            await onClosed();
          }}
          onError={showErr}
        />
      )}

      {modal.kind === "excluir" && (
        <ExcluirModal
          doc={modal.doc}
          open
          onClose={() => setModal({ kind: "none" })}
          onSuccess={async () => {
            showOk(`Documento "${modal.doc.nome}" excluído.`);
            await onClosed();
          }}
          onError={showErr}
        />
      )}
    </div>
  );
}

function StorageMeter({
  totalBytes,
  docs,
}: {
  totalBytes: number;
  docs: DocumentoResponse[];
}) {
  const pct = Math.min(100, (totalBytes / MAX_TOTAL_BYTES) * 100);
  const cheio = totalBytes >= MAX_TOTAL_BYTES;
  const emAlerta = totalBytes >= MAX_TOTAL_BYTES * AVISO_PERCENTUAL;

  const barColor = cheio
    ? "bg-red-500"
    : emAlerta
      ? "bg-amber-500"
      : "bg-emerald-500";

  // Maiores arquivos primeiro — ajuda a decidir o que excluir.
  const maiores = [...docs]
    .sort((a, b) => b.tamanhoBytes - a.tamanhoBytes)
    .slice(0, 3);

  return (
    <Card className="mb-5">
      <CardContent className="pt-6">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2">
            <div className="grid h-9 w-9 place-items-center rounded-md bg-zinc-100 text-zinc-600">
              <HardDrive size={18} />
            </div>
            <div>
              <p className="font-semibold text-zinc-800 text-sm">
                Armazenamento total
              </p>
              <p className="text-xs text-zinc-500">
                {formatBytes(totalBytes)} de {formatBytes(MAX_TOTAL_BYTES)}{" "}
                utilizados
              </p>
            </div>
          </div>
          <span
            className={cn(
              "rounded-full px-2.5 py-1 font-semibold text-xs",
              cheio
                ? "bg-red-50 text-red-700"
                : emAlerta
                  ? "bg-amber-50 text-amber-700"
                  : "bg-emerald-50 text-emerald-700",
            )}
          >
            {pct.toFixed(pct >= 10 ? 0 : 1)}%
          </span>
        </div>

        <div className="mt-4 h-2.5 w-full overflow-hidden rounded-full bg-zinc-100">
          <div
            className={cn("h-full rounded-full transition-all", barColor)}
            style={{ width: `${pct}%` }}
          />
        </div>

        {(cheio || emAlerta) && (
          <div
            className={cn(
              "mt-4 flex items-start gap-2 rounded-md border px-3 py-2 text-sm",
              cheio
                ? "border-red-200 bg-red-50 text-red-700"
                : "border-amber-200 bg-amber-50 text-amber-700",
            )}
          >
            <AlertTriangle size={16} className="mt-0.5 shrink-0" />
            <div>
              <p className="font-semibold">
                {cheio
                  ? "Limite de 2 GB atingido."
                  : "Você está perto do limite de 2 GB."}
              </p>
              <p>
                {cheio
                  ? "Não é possível enviar novos arquivos. Exclua alguns documentos para liberar espaço."
                  : `Restam ${formatBytes(Math.max(0, MAX_TOTAL_BYTES - totalBytes))} livres. Considere excluir documentos antigos.`}
                {maiores.length > 0 && (
                  <>
                    {" "}
                    Maiores arquivos:{" "}
                    {maiores
                      .map((d) => `${d.nome} (${formatBytes(d.tamanhoBytes)})`)
                      .join(", ")}
                    .
                  </>
                )}
              </p>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function DocCard({
  doc,
  onSubstituir,
  onEditar,
  onExcluir,
}: {
  doc: DocumentoResponse;
  onSubstituir: () => void;
  onEditar: () => void;
  onExcluir: () => void;
}) {
  const Icon = doc.extensao === "PPTX" ? FileText : FileSpreadsheet;
  return (
    <Card>
      <CardContent className="flex h-full flex-col pt-6">
        <div className="flex items-start gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-md bg-red-50 text-[var(--claro-red)]">
            <Icon size={20} />
          </div>
          <div className="min-w-0 flex-1">
            <p
              className="truncate font-semibold text-zinc-800"
              title={doc.nome}
            >
              {doc.nome}
            </p>
            <p className="text-xs text-zinc-500">
              {extLabel(doc.extensao)} · {formatBytes(doc.tamanhoBytes)}
            </p>
          </div>
        </div>

        <dl className="mt-4 space-y-1.5 text-xs">
          <div className="flex justify-between">
            <dt className="text-zinc-500">Atualização</dt>
            <dd className="font-medium text-zinc-700">
              {formatDate(doc.dataAtualizacao)}
            </dd>
          </div>
        </dl>

        <p
          className="mt-3 line-clamp-3 rounded-md bg-zinc-50 p-2 text-xs text-zinc-700"
          title={doc.descricao}
        >
          {doc.descricao}
        </p>

        <div className="mt-4 flex flex-wrap gap-2 border-zinc-100 border-t pt-4">
          <Button size="sm" onClick={onSubstituir}>
            <RefreshCw size={14} /> Substituir arquivo
          </Button>
          <Button size="sm" variant="outline" onClick={onEditar}>
            <Pencil size={14} /> Editar
          </Button>
          <Button
            size="sm"
            variant="outline"
            className="ml-auto text-red-600 hover:text-red-700"
            onClick={onExcluir}
          >
            <Trash2 size={14} /> Excluir
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function FilePicker({
  file,
  onPick,
}: {
  file: File | null;
  onPick: (f: File | null) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  return (
    <>
      <label
        htmlFor="modal-file"
        className="flex cursor-pointer items-center justify-center gap-3 rounded-md border-2 border-zinc-300 border-dashed bg-zinc-50 px-4 py-6 text-sm text-zinc-500 hover:border-[var(--claro-red)] hover:bg-red-50/50"
      >
        <Upload size={20} />
        {file ? (
          <span className="font-semibold text-zinc-800">{file.name}</span>
        ) : (
          <span>
            Clique para escolher — <strong>{ALLOWED.join(", ")}</strong> (até 60
            MB)
          </span>
        )}
      </label>
      <input
        ref={inputRef}
        id="modal-file"
        type="file"
        accept={ALLOWED.join(",")}
        className="sr-only"
        onChange={(e) => onPick(e.target.files?.[0] ?? null)}
      />
    </>
  );
}

function NovoModal({
  open,
  disponivel,
  onClose,
  onSuccess,
  onError,
}: {
  open: boolean;
  disponivel: number;
  onClose: () => void;
  onSuccess: (d: DocumentoResponse) => void | Promise<void>;
  onError: (msg: string) => void;
}) {
  const [nome, setNome] = useState("");
  const [descricao, setDescricao] = useState("");
  const [data, setData] = useState(() => hojeLocal());
  const [file, setFile] = useState<File | null>(null);
  const [erroLocal, setErroLocal] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [progresso, setProgresso] = useState<number | null>(null);

  function reset() {
    setNome("");
    setDescricao("");
    setData(hojeLocal());
    setFile(null);
    setErroLocal(null);
    setSubmitting(false);
    setProgresso(null);
  }

  function handleOpenChange(o: boolean) {
    if (!o) {
      reset();
      onClose();
    }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setErroLocal(null);
    if (!nome.trim()) return setErroLocal("Informe o nome.");
    if (!descricao.trim()) return setErroLocal("Informe a descrição.");
    if (!data) return setErroLocal("Informe a data de atualização.");
    const arquivoErr = validarArquivo(file);
    if (arquivoErr) return setErroLocal(arquivoErr);
    if (!file) return;
    if (file.size > disponivel) {
      return setErroLocal(
        `Sem espaço: este arquivo (${formatBytes(file.size)}) excede o espaço livre (${formatBytes(disponivel)}). Exclua documentos para liberar o limite de 2 GB.`,
      );
    }

    setSubmitting(true);
    try {
      const created = await docsApi.criar(
        {
          nome: nome.trim(),
          descricao: descricao.trim(),
          dataAtualizacao: data,
        },
        file,
        setProgresso,
      );
      reset();
      await onSuccess(created);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao criar.";
      setErroLocal(msg);
      onError(msg);
      setSubmitting(false);
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={handleOpenChange}
      title="Novo documento"
      description="Cadastra um novo documento e faz o upload do binário inicial."
      size="lg"
    >
      <form onSubmit={submit} className="space-y-4">
        <div>
          <Label htmlFor="novo-nome">Nome</Label>
          <Input
            id="novo-nome"
            className="mt-1"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="novo-data">Data de atualização (manual)</Label>
          <Input
            id="novo-data"
            type="date"
            className="mt-1"
            value={data}
            onChange={(e) => setData(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="novo-desc">Descrição / resumo</Label>
          <textarea
            id="novo-desc"
            rows={3}
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            className="mt-1 w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm outline-none focus:border-[var(--claro-red)] focus:ring-2 focus:ring-[var(--claro-red)]/20"
          />
        </div>
        <div>
          <Label>Arquivo</Label>
          <div className="mt-1">
            <FilePicker
              file={file}
              onPick={(f) => {
                setFile(f);
                if (f) {
                  const nomeArquivo = inferNomeFromFilename(f.name);
                  setNome(nomeArquivo);
                  setDescricao(nomeArquivo);
                }
              }}
            />
          </div>
        </div>
        {erroLocal && (
          <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
            {erroLocal}
          </div>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => handleOpenChange(false)}
          >
            Cancelar
          </Button>
          <Button type="submit" disabled={submitting}>
            <Upload size={14} />{" "}
            {submitting
              ? progresso != null
                ? `${Math.round(progresso * 100)}%`
                : "Criando…"
              : "Criar documento"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

function SubstituirArquivoModal({
  doc,
  open,
  onClose,
  onSuccess,
  onError,
}: {
  doc: DocumentoResponse;
  open: boolean;
  onClose: () => void;
  onSuccess: (d: DocumentoResponse) => void | Promise<void>;
  onError: (msg: string) => void;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [erroLocal, setErroLocal] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [progresso, setProgresso] = useState<number | null>(null);

  function handleOpenChange(o: boolean) {
    if (!o) {
      setFile(null);
      setErroLocal(null);
      setSubmitting(false);
      setProgresso(null);
      onClose();
    }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const err = validarArquivo(file);
    if (err) return setErroLocal(err);
    if (!file) return;
    setSubmitting(true);
    try {
      // Nome e data são atualizados junto com o binário numa única chamada —
      // assim o histórico registra apenas uma "Substituição", não substituição + edição.
      const updated = await docsApi.substituirArquivo(
        doc.id,
        file,
        {
          nome: inferNomeFromFilename(file.name),
          dataAtualizacao: hojeLocal(),
        },
        setProgresso,
      );
      await onSuccess(updated);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao substituir.";
      setErroLocal(msg);
      onError(msg);
      setSubmitting(false);
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={handleOpenChange}
      title="Substituir arquivo"
      description={
        <>
          Documento: <strong>{doc.nome}</strong> será substituído pelo documento{" "}
          <strong>
            {file ? inferNomeFromFilename(file.name) : "selecionado"}
          </strong>
          .
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <div>
          <Label>Versão atual</Label>
          <div className="mt-1 rounded-md bg-zinc-50 px-3 py-2 text-xs text-zinc-700">
            {doc.extensao} · {formatBytes(doc.tamanhoBytes)} · atualizado{" "}
            {formatDate(doc.atualizadoEm)}
          </div>
        </div>
        <div>
          <Label>Novo arquivo</Label>
          <div className="mt-1">
            <FilePicker file={file} onPick={setFile} />
          </div>
        </div>
        {erroLocal && (
          <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
            {erroLocal}
          </div>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => handleOpenChange(false)}
          >
            Cancelar
          </Button>
          <Button type="submit" disabled={submitting}>
            <RefreshCw size={14} />{" "}
            {submitting
              ? progresso != null
                ? `${Math.round(progresso * 100)}%`
                : "Enviando…"
              : "Substituir"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

function EditarMetadadosModal({
  doc,
  open,
  onClose,
  onSuccess,
  onError,
}: {
  doc: DocumentoResponse;
  open: boolean;
  onClose: () => void;
  onSuccess: (d: DocumentoResponse) => void | Promise<void>;
  onError: (msg: string) => void;
}) {
  const [nome, setNome] = useState(doc.nome);
  const [descricao, setDescricao] = useState(doc.descricao);
  const [data, setData] = useState(doc.dataAtualizacao);
  const [erroLocal, setErroLocal] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function handleOpenChange(o: boolean) {
    if (!o) {
      setNome(doc.nome);
      setDescricao(doc.descricao);
      setData(doc.dataAtualizacao);
      setErroLocal(null);
      setSubmitting(false);
      onClose();
    }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setErroLocal(null);
    if (!nome.trim()) return setErroLocal("Informe o nome.");
    if (!descricao.trim()) return setErroLocal("Informe a descrição.");
    if (!data) return setErroLocal("Informe a data de atualização.");
    setSubmitting(true);
    try {
      const updated = await docsApi.atualizarMetadados(doc.id, {
        nome: nome.trim(),
        descricao: descricao.trim(),
        dataAtualizacao: data,
      });
      await onSuccess(updated);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao atualizar.";
      setErroLocal(msg);
      onError(msg);
      setSubmitting(false);
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={handleOpenChange}
      title="Editar metadados"
      description={
        <>
          Documento: <strong>{doc.nome}</strong>. Não altera o binário — apenas
          a informação exibida no portal.
        </>
      }
      size="lg"
    >
      <form onSubmit={submit} className="space-y-4">
        <div>
          <Label htmlFor="edit-nome">Nome</Label>
          <Input
            id="edit-nome"
            className="mt-1"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="edit-data">Data de atualização (manual)</Label>
          <Input
            id="edit-data"
            type="date"
            className="mt-1"
            value={data}
            onChange={(e) => setData(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="edit-desc">Descrição / resumo</Label>
          <textarea
            id="edit-desc"
            rows={3}
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            className="mt-1 w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm outline-none focus:border-[var(--claro-red)] focus:ring-2 focus:ring-[var(--claro-red)]/20"
          />
        </div>
        {erroLocal && (
          <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
            {erroLocal}
          </div>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => handleOpenChange(false)}
          >
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

function ExcluirModal({
  doc,
  open,
  onClose,
  onSuccess,
  onError,
}: {
  doc: DocumentoResponse;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void | Promise<void>;
  onError: (msg: string) => void;
}) {
  const [submitting, setSubmitting] = useState(false);

  function handleOpenChange(o: boolean) {
    if (!o) {
      setSubmitting(false);
      onClose();
    }
  }

  async function excluir() {
    setSubmitting(true);
    try {
      await docsApi.deletar(doc.id);
      await onSuccess();
    } catch (e) {
      onError(e instanceof Error ? e.message : "Erro ao excluir.");
      setSubmitting(false);
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={handleOpenChange}
      title="Excluir documento"
      description={
        <>
          Tem certeza que deseja excluir <strong>{doc.nome}</strong>? Esta ação
          esconde o documento do portal (soft delete) — o arquivo continua
          armazenado no servidor.
        </>
      }
      footer={
        <>
          <Button variant="outline" onClick={() => handleOpenChange(false)}>
            Cancelar
          </Button>
          <Button
            onClick={excluir}
            disabled={submitting}
            className="bg-red-600 text-white hover:bg-red-700"
          >
            <Trash2 size={14} /> {submitting ? "Excluindo…" : "Excluir"}
          </Button>
        </>
      }
    >
      <p className="text-sm text-zinc-600">
        {formatDate(doc.dataAtualizacao)} · {doc.extensao} ·{" "}
        {formatBytes(doc.tamanhoBytes)}
      </p>
    </Dialog>
  );
}
