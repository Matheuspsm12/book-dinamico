"use client";

import {
  Check,
  Clock,
  Play,
  Power,
  Search,
  Settings2,
  X,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";

import { PageHeader } from "src/components/shared/PageHeader";
import { Button } from "src/components/ui/button";
import { Card, CardContent } from "src/components/ui/card";
import { Dialog } from "src/components/ui/dialog";
import { Input } from "src/components/ui/input";
import { useAuth } from "src/app/contexts/AuthContext";
import type {
  OciosidadeResultado,
  PerfilResponse,
  UsuarioResponse,
  UsuarioStatus,
} from "src/lib/api/types";
import { cn, formatDate } from "src/lib/utils";
import * as perfisApi from "src/services/perfis-service";
import * as usuariosApi from "src/services/usuarios-service";

// Usuários base do sistema não podem ser gerenciados.
const EMAILS_USUARIO_BASE = ["qwerer", "admin@claro.com.br"];
function ehUsuarioBase(email: string) {
  return EMAILS_USUARIO_BASE.includes(email.toLowerCase());
}

const statusBadge: Record<UsuarioStatus, string> = {
  PENDENTE: "bg-amber-100 text-amber-700",
  APROVADO: "bg-emerald-100 text-emerald-700",
  REJEITADO: "bg-red-100 text-red-700",
  DESATIVADO: "bg-zinc-200 text-zinc-600",
};

export default function GerenciarUsuariosPage() {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [users, setUsers] = useState<UsuarioResponse[]>([]);
  const [perfis, setPerfis] = useState<PerfilResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [statusFilter, setStatusFilter] = useState<UsuarioStatus | "TODOS">(
    "TODOS",
  );
  const [empresaFilter, setEmpresaFilter] = useState("todas");

  // modal
  const [selecionado, setSelecionado] = useState<UsuarioResponse | null>(null);
  const [perfilEscolhido, setPerfilEscolhido] = useState<number | "">("");
  const [salvando, setSalvando] = useState(false);
  const [erroModal, setErroModal] = useState<string | null>(null);

  // ociosidade
  const [ociosidadeRodando, setOciosidadeRodando] = useState(false);
  const [ociosidadeResultado, setOciosidadeResultado] =
    useState<OciosidadeResultado | null>(null);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErr(null);
    try {
      const filtro = {
        status: statusFilter === "TODOS" ? undefined : statusFilter,
        empresa: empresaFilter === "todas" ? undefined : empresaFilter,
        nome: q || undefined,
      };
      const page = await usuariosApi.paginar(filtro, 0, 100, "criadoEm,desc");
      setUsers(page.content);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao carregar usuários.");
    } finally {
      setCarregando(false);
    }
  }, [statusFilter, empresaFilter, q]);

  useEffect(() => {
    if (!loading && user && user.role !== "ADMIN") router.replace("/book");
  }, [user, loading, router]);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    const t = setTimeout(() => {
      void carregar();
    }, 250);
    return () => clearTimeout(t);
  }, [user, carregar]);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    perfisApi
      .listarPerfis()
      .then((ps) => setPerfis(ps.filter((p) => p.ativado !== false)))
      .catch(() => setPerfis([]));
  }, [user]);

  const empresas = useMemo(
    () => ["todas", ...Array.from(new Set(users.map((u) => u.empresa)))],
    [users],
  );

  function abrirModal(u: UsuarioResponse) {
    setSelecionado(u);
    setPerfilEscolhido(u.idPerfil ?? "");
    setErroModal(null);
  }

  function fecharModal() {
    setSelecionado(null);
    setPerfilEscolhido("");
    setErroModal(null);
    setSalvando(false);
  }

  async function executarModal(acao: () => Promise<unknown>) {
    setErroModal(null);
    setSalvando(true);
    try {
      await acao();
      fecharModal();
      await carregar();
    } catch (e) {
      setErroModal(e instanceof Error ? e.message : "Erro ao executar ação.");
      setSalvando(false);
    }
  }

  async function simular(notificadoHaDias?: number) {
    if (!selecionado) return;
    setErroModal(null);
    setSalvando(true);
    try {
      const atualizado = await usuariosApi.simularOciosidade(selecionado.id, {
        notificadoHaDias,
      });
      setSelecionado(atualizado);
      await carregar();
    } catch (e) {
      setErroModal(
        e instanceof Error ? e.message : "Erro ao simular ociosidade.",
      );
    } finally {
      setSalvando(false);
    }
  }

  async function rodarOciosidade() {
    setErr(null);
    setOciosidadeResultado(null);
    setOciosidadeRodando(true);
    try {
      const res = await usuariosApi.processarOciosidade();
      setOciosidadeResultado(res);
      await carregar();
    } catch (e) {
      setErr(
        e instanceof Error ? e.message : "Erro ao processar ociosidade.",
      );
    } finally {
      setOciosidadeRodando(false);
    }
  }

  if (loading || !user || user.role !== "ADMIN") return null;

  const nomePerfil = (id?: number) =>
    perfis.find((p) => p.id === id)?.nomePerfil;

  const perfilIdNumber =
    perfilEscolhido === "" ? undefined : Number(perfilEscolhido);

  return (
    <div>
      <PageHeader
        title="Gerenciar Usuários"
        subtitle="Aprovar, rejeitar, definir perfil e controlar acesso ao portal."
      />

      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative min-w-[240px] flex-1">
              <Search
                size={16}
                className="absolute top-1/2 left-3 -translate-y-1/2 text-zinc-400"
              />
              <Input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Buscar por nome..."
                className="pl-9"
              />
            </div>
            <select
              value={statusFilter}
              onChange={(e) =>
                setStatusFilter(e.target.value as UsuarioStatus | "TODOS")
              }
              className="h-10 rounded-md border border-zinc-300 bg-white px-3 text-sm"
            >
              <option value="TODOS">Todos status</option>
              <option value="PENDENTE">Pendente</option>
              <option value="APROVADO">Aprovado</option>
              <option value="REJEITADO">Rejeitado</option>
              <option value="DESATIVADO">Desativado</option>
            </select>
            <select
              value={empresaFilter}
              onChange={(e) => setEmpresaFilter(e.target.value)}
              className="h-10 rounded-md border border-zinc-300 bg-white px-3 text-sm"
            >
              {empresas.map((e) => (
                <option key={e} value={e}>
                  {e === "todas" ? "Todas empresas" : e}
                </option>
              ))}
            </select>
            <Button
              variant="outline"
              onClick={() => void rodarOciosidade()}
              disabled={ociosidadeRodando}
              title="Executa a mesma rotina do agendamento diário, agora."
            >
              <Clock size={14} />
              {ociosidadeRodando
                ? "Verificando…"
                : "Rodar verificação de ociosidade"}
            </Button>
          </div>

          {ociosidadeResultado && (
            <div className="mt-4 rounded-md border border-zinc-200 bg-zinc-50 px-3 py-2 text-sm text-zinc-700">
              {ociosidadeResultado.emailDesabilitado ? (
                "Verificação não executada: o envio de e-mail está desabilitado no servidor."
              ) : (
                <>
                  <span className="font-semibold">
                    {ociosidadeResultado.notificados.length}
                  </span>{" "}
                  usuário(s) notificado(s) por ociosidade
                  {ociosidadeResultado.notificados.length > 0 && (
                    <span className="text-zinc-500">
                      {" "}
                      ({ociosidadeResultado.notificados.join(", ")})
                    </span>
                  )}
                  {" · "}
                  <span className="font-semibold">
                    {ociosidadeResultado.desativados.length}
                  </span>{" "}
                  desativado(s)
                  {ociosidadeResultado.desativados.length > 0 && (
                    <span className="text-zinc-500">
                      {" "}
                      ({ociosidadeResultado.desativados.join(", ")})
                    </span>
                  )}
                  .
                </>
              )}
            </div>
          )}

          {err && (
            <div className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
              {err}
            </div>
          )}

          <div className="mt-5 overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-zinc-200 border-b text-left text-xs text-zinc-500 uppercase">
                  <th className="py-3 font-semibold">Nome</th>
                  <th className="py-3 font-semibold">Empresa</th>
                  <th className="py-3 font-semibold">E-mail</th>
                  <th className="py-3 font-semibold">Perfil</th>
                  <th className="py-3 font-semibold">Criado</th>
                  <th className="py-3 font-semibold">Status</th>
                  <th className="py-3 text-right font-semibold">Ações</th>
                </tr>
              </thead>
              <tbody>
                {carregando ? (
                  <tr>
                    <td colSpan={7} className="py-10 text-center text-zinc-400">
                      Carregando…
                    </td>
                  </tr>
                ) : users.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-10 text-center text-zinc-400">
                      Nenhum usuário encontrado.
                    </td>
                  </tr>
                ) : (
                  users.map((u) => (
                    <tr
                      key={u.id}
                      className="border-zinc-100 border-b hover:bg-zinc-50"
                    >
                      <td className="py-3">
                        <p className="font-semibold text-zinc-800">{u.nome}</p>
                        {u.justificativa && (
                          <p
                            className="max-w-xs truncate text-xs text-zinc-500"
                            title={u.justificativa}
                          >
                            {u.justificativa}
                          </p>
                        )}
                      </td>
                      <td className="py-3 text-zinc-700">{u.empresa}</td>
                      <td className="py-3 text-zinc-700">{u.email}</td>
                      <td className="py-3 text-zinc-700">
                        {u.role ? (
                          u.role
                        ) : (
                          <span className="text-zinc-400">—</span>
                        )}
                      </td>
                      <td className="py-3 text-zinc-500">
                        {u.criadoEm ? formatDate(u.criadoEm) : "—"}
                      </td>
                      <td className="py-3">
                        <span
                          className={cn(
                            "rounded-full px-2 py-0.5 font-semibold text-xs capitalize",
                            statusBadge[u.status],
                          )}
                        >
                          {u.status.toLowerCase()}
                        </span>
                      </td>
                      <td className="py-3">
                        <div className="flex justify-end">
                          {ehUsuarioBase(u.email) ? (
                            <span className="text-xs text-zinc-400 italic">
                              Usuário base
                            </span>
                          ) : (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => abrirModal(u)}
                            >
                              <Settings2 size={14} /> Gerenciar
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <p className="mt-3 text-xs text-zinc-400">
            {users.length} usuário(s)
          </p>
        </CardContent>
      </Card>

      <Dialog
        open={selecionado !== null}
        onOpenChange={(o) => {
          if (!o) fecharModal();
        }}
        title="Gerenciar Usuário"
        description="Gerencie e edite as informações deste usuário."
        size="lg"
        footer={
          selecionado ? (
            <ModalFooter
              usuario={selecionado}
              isSelf={selecionado.email === user.email}
              perfilId={perfilIdNumber}
              salvando={salvando}
              onCancel={fecharModal}
              onAprovar={() =>
                executarModal(() =>
                  usuariosApi.aprovar(selecionado.id, perfilIdNumber),
                )
              }
              onRejeitar={() =>
                executarModal(() => usuariosApi.rejeitar(selecionado.id))
              }
              onSalvar={() =>
                executarModal(() =>
                  usuariosApi.atualizar(selecionado.id, {
                    idPerfil: perfilIdNumber,
                  }),
                )
              }
              onDesativar={() =>
                executarModal(() => usuariosApi.desativar(selecionado.id))
              }
              onAtivar={() =>
                executarModal(() => usuariosApi.ativar(selecionado.id))
              }
            />
          ) : null
        }
      >
        {selecionado && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Campo label="Nome" valor={selecionado.nome} />
              <Campo label="E-mail" valor={selecionado.email} />
              <Campo label="Empresa" valor={selecionado.empresa} />
              <Campo
                label="Data solicitação"
                valor={
                  selecionado.criadoEm ? formatDate(selecionado.criadoEm) : "—"
                }
              />
            </div>

            {selecionado.justificativa && (
              <Campo
                label="Justificativa"
                valor={selecionado.justificativa}
              />
            )}

            <div>
              <label className="mb-1 block font-semibold text-xs text-zinc-500 uppercase">
                Tipo de Perfil
              </label>
              <select
                value={perfilEscolhido}
                onChange={(e) =>
                  setPerfilEscolhido(
                    e.target.value === "" ? "" : Number(e.target.value),
                  )
                }
                className="h-10 w-full rounded-md border border-zinc-300 bg-white px-3 text-sm"
              >
                <option value="">Selecione</option>
                {perfis.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nomePerfil}
                  </option>
                ))}
              </select>
              {selecionado.idPerfil && (
                <p className="mt-1 text-xs text-zinc-400">
                  Perfil atual: {nomePerfil(selecionado.idPerfil) ?? "—"}
                </p>
              )}
            </div>

            {selecionado.status === "APROVADO" && (
              <div className="rounded-md border border-zinc-200 bg-zinc-50 p-3">
                <p className="mb-2 flex items-center gap-1.5 font-semibold text-xs text-zinc-600 uppercase">
                  <Clock size={13} /> Ociosidade
                </p>
                <div className="grid grid-cols-2 gap-4">
                  <Campo
                    label="Último acesso"
                    valor={
                      selecionado.ultimoAcesso
                        ? formatDate(selecionado.ultimoAcesso)
                        : "—"
                    }
                  />
                  <Campo
                    label="Notificado em"
                    valor={
                      selecionado.ociosidadeNotificadoEm
                        ? formatDate(selecionado.ociosidadeNotificadoEm)
                        : "—"
                    }
                  />
                </div>
                {user.producao === false && (
                  <>
                    <div className="mt-3 flex flex-wrap gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => void simular(undefined)}
                        disabled={salvando}
                      >
                        <Play size={13} /> Simular inatividade (&gt;4 meses)
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => void simular(5)}
                        disabled={salvando}
                      >
                        <Play size={13} /> Simular prazo vencido
                      </Button>
                    </div>
                    <p className="mt-2 text-xs text-zinc-400">
                      Ferramenta de teste (indisponível em produção). Depois de
                      simular, use “Rodar verificação de ociosidade” para
                      disparar o e-mail (inatividade) ou a desativação (prazo
                      vencido).
                    </p>
                  </>
                )}
              </div>
            )}

            {erroModal && (
              <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
                {erroModal}
              </div>
            )}
          </div>
        )}
      </Dialog>
    </div>
  );
}

function Campo({ label, valor }: { label: string; valor: string }) {
  return (
    <div>
      <p className="font-semibold text-xs text-zinc-500 uppercase">{label}</p>
      <p className="text-sm text-zinc-800">{valor}</p>
    </div>
  );
}

function ModalFooter({
  usuario,
  isSelf,
  perfilId,
  salvando,
  onCancel,
  onAprovar,
  onRejeitar,
  onSalvar,
  onDesativar,
  onAtivar,
}: {
  usuario: UsuarioResponse;
  isSelf: boolean;
  perfilId?: number;
  salvando: boolean;
  onCancel: () => void;
  onAprovar: () => void;
  onRejeitar: () => void;
  onSalvar: () => void;
  onDesativar: () => void;
  onAtivar: () => void;
}) {
  return (
    <>
      <Button variant="outline" onClick={onCancel} disabled={salvando}>
        Cancelar
      </Button>

      {usuario.status === "PENDENTE" && (
        <>
          <Button variant="outline" onClick={onRejeitar} disabled={salvando}>
            <X size={14} /> Rejeitar
          </Button>
          <Button onClick={onAprovar} disabled={salvando || perfilId == null}>
            <Check size={14} /> Aprovar
          </Button>
        </>
      )}

      {usuario.status === "APROVADO" && (
        <>
          {!isSelf && (
            <Button
              variant="outline"
              onClick={onDesativar}
              disabled={salvando}
            >
              <Power size={14} /> Desativar
            </Button>
          )}
          <Button onClick={onSalvar} disabled={salvando || perfilId == null}>
            Salvar
          </Button>
        </>
      )}

      {usuario.status === "DESATIVADO" && (
        <Button onClick={onAtivar} disabled={salvando}>
          Reativar
        </Button>
      )}
    </>
  );
}
