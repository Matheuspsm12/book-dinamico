"use client";

import {
  CheckCircle2,
  FileSpreadsheet,
  FileText,
  Pencil,
  Plus,
  RefreshCw,
  Table2,
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
import type {
  DocumentoAbaResponse,
  DocumentoResponse,
} from "src/lib/api/types";
import {
  cn,
  formatDate,
  hojeLocal,
  inferNomeFromFilename,
} from "src/lib/utils";
import * as docsApi from "src/services/documentos-service";

const ALLOWED = [".xlsm", ".xlsx", ".pptx"] as const;
const MAX_BYTES = 60 * 1024 * 1024;

type Modal =
  | { kind: "none" }
  | { kind: "novo" }
  | { kind: "substituir"; doc: DocumentoResponse }
  | { kind: "editar"; doc: DocumentoResponse }
  | { kind: "conteudo"; doc: DocumentoResponse }
  | { kind: "excluir"; doc: DocumentoResponse };

function extLabel(ext: DocumentoResponse["extensao"]) {
  return ext;
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
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

  return (
    <div>
      <PageHeader
        title="Upload do Book Dinâmico"
        subtitle="Gerencie os documentos publicados no portal."
      />

      <div className="mb-5 flex items-center justify-between">
        <p className="text-sm text-zinc-500">
          {carregando
            ? "Carregando…"
            : `${docs.length} documento(s) publicado(s)`}
        </p>
        <Button onClick={() => setModal({ kind: "novo" })}>
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
              onConteudo={() => setModal({ kind: "conteudo", doc: d })}
              onExcluir={() => setModal({ kind: "excluir", doc: d })}
            />
          ))}
        </div>
      )}

      <NovoModal
        open={modal.kind === "novo"}
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

      {modal.kind === "conteudo" && (
        <ConteudoModal
          doc={modal.doc}
          open
          onClose={() => setModal({ kind: "none" })}
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

function ConteudoModal({
  doc,
  open,
  onClose,
}: {
  doc: DocumentoResponse;
  open: boolean;
  onClose: () => void;
}) {
  const [abas, setAbas] = useState<DocumentoAbaResponse[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setAbas(null);
    setErro(null);
    docsApi
      .verConteudo(doc.id)
      .then(setAbas)
      .catch((e) =>
        setErro(e instanceof Error ? e.message : "Erro ao carregar conteúdo."),
      );
  }, [open, doc.id]);

  const rotulo = doc.extensao === "PPTX" ? "Slides" : "Abas";

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => !o && onClose()}
      title={`Conteúdo — ${doc.nome}`}
      description="Extraído no processamento do arquivo."
      size="lg"
    >
      {erro && (
        <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
          {erro}
        </div>
      )}
      {!erro && abas === null && (
        <p className="py-6 text-center text-sm text-zinc-500">Carregando…</p>
      )}
      {!erro && abas !== null && abas.length === 0 && (
        <p className="py-6 text-center text-sm text-zinc-500">
          Nenhum conteúdo extraído. Reprocesse o documento.
        </p>
      )}
      {!erro && abas !== null && abas.length > 0 && (
        <table className="w-full text-sm">
          <thead>
            <tr className="border-zinc-200 border-b text-left text-zinc-500">
              <th className="py-2 font-medium">{rotulo}</th>
              <th className="py-2 text-right font-medium">Linhas</th>
              <th className="py-2 text-right font-medium">Colunas</th>
            </tr>
          </thead>
          <tbody>
            {abas.map((a, i) => (
              <tr key={i} className="border-zinc-100 border-b">
                <td className="py-2 text-zinc-800">{a.nomeAba}</td>
                <td className="py-2 text-right text-zinc-700">{a.qtdLinhas}</td>
                <td className="py-2 text-right text-zinc-700">{a.qtdColunas}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Dialog>
  );
}

function DocCard({
  doc,
  onSubstituir,
  onEditar,
  onConteudo,
  onExcluir,
}: {
  doc: DocumentoResponse;
  onSubstituir: () => void;
  onEditar: () => void;
  onConteudo: () => void;
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
          <Button size="sm" variant="outline" onClick={onConteudo}>
            <Table2 size={14} /> Ver conteúdo
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
  onClose,
  onSuccess,
  onError,
}: {
  open: boolean;
  onClose: () => void;
  onSuccess: (d: DocumentoResponse) => void | Promise<void>;
  onError: (msg: string) => void;
}) {
  const [nome, setNome] = useState("");
  const [nomeManual, setNomeManual] = useState(false);
  const [descricao, setDescricao] = useState("");
  const [data, setData] = useState(() => hojeLocal());
  const [file, setFile] = useState<File | null>(null);
  const [erroLocal, setErroLocal] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function reset() {
    setNome("");
    setNomeManual(false);
    setDescricao("");
    setData(hojeLocal());
    setFile(null);
    setErroLocal(null);
    setSubmitting(false);
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

    setSubmitting(true);
    try {
      const created = await docsApi.criar(
        {
          nome: nome.trim(),
          descricao: descricao.trim(),
          dataAtualizacao: data,
        },
        file,
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
            onChange={(e) => {
              setNome(e.target.value);
              setNomeManual(e.target.value.trim().length > 0);
            }}
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
                if (f && !nomeManual) setNome(inferNomeFromFilename(f.name));
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
            <Upload size={14} /> {submitting ? "Criando…" : "Criar documento"}
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

  function handleOpenChange(o: boolean) {
    if (!o) {
      setFile(null);
      setErroLocal(null);
      setSubmitting(false);
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
      await docsApi.substituirArquivo(doc.id, file);
      const hoje = hojeLocal();
      const nomeArquivo = inferNomeFromFilename(file.name);
      const updated = await docsApi.atualizarMetadados(doc.id, {
        nome: nomeArquivo,
        descricao: doc.descricao,
        dataAtualizacao: hoje,
      });
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
            <RefreshCw size={14} /> {submitting ? "Enviando…" : "Substituir"}
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
