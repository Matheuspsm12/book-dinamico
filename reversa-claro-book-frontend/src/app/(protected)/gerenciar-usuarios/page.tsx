'use client'

import { Check, Power, Search, X } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useCallback, useEffect, useMemo, useState } from 'react'

import { PageHeader } from '@/components/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/contexts/AuthContext'
import type { UsuarioResponse, UsuarioStatus } from '@/lib/api/types'
import * as usuariosApi from '@/lib/api/usuarios'
import { cn, formatDate } from '@/lib/utils'

const statusBadge: Record<UsuarioStatus, string> = {
  PENDENTE: 'bg-amber-100 text-amber-700',
  APROVADO: 'bg-emerald-100 text-emerald-700',
  REJEITADO: 'bg-red-100 text-red-700',
  DESATIVADO: 'bg-zinc-200 text-zinc-600',
}

export default function GerenciarUsuariosPage() {
  const router = useRouter()
  const { user, loading } = useAuth()
  const [users, setUsers] = useState<UsuarioResponse[]>([])
  const [carregando, setCarregando] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState<UsuarioStatus | 'TODOS'>(
    'TODOS',
  )
  const [empresaFilter, setEmpresaFilter] = useState('todas')

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErr(null)
    try {
      const filtro = {
        status: statusFilter === 'TODOS' ? undefined : statusFilter,
        empresa: empresaFilter === 'todas' ? undefined : empresaFilter,
        nome: q || undefined,
      }
      const page = await usuariosApi.paginar(filtro, 0, 100, 'criadoEm,desc')
      setUsers(page.content)
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Erro ao carregar usuários.')
    } finally {
      setCarregando(false)
    }
  }, [statusFilter, empresaFilter, q])

  // Redireciona não-admin
  useEffect(() => {
    if (!loading && user && user.role !== 'ADMIN') router.replace('/book')
  }, [user, loading, router])

  // Recarrega quando filtros mudam (debounce simples no nome)
  useEffect(() => {
    if (user?.role !== 'ADMIN') return
    const t = setTimeout(() => {
      void carregar()
    }, 250)
    return () => clearTimeout(t)
  }, [user, carregar])

  const empresas = useMemo(
    () => ['todas', ...Array.from(new Set(users.map((u) => u.empresa)))],
    [users],
  )

  async function executar(acao: () => Promise<unknown>) {
    setErr(null)
    try {
      await acao()
      await carregar()
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Erro ao executar ação.')
    }
  }

  if (loading || !user || user.role !== 'ADMIN') return null

  return (
    <div>
      <PageHeader
        title="Gerenciar Usuários"
        subtitle="Aprovar, rejeitar e controlar acesso ao portal."
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
                setStatusFilter(e.target.value as UsuarioStatus | 'TODOS')
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
                  {e === 'todas' ? 'Todas empresas' : e}
                </option>
              ))}
            </select>
          </div>

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
                  <th className="py-3 font-semibold">Criado</th>
                  <th className="py-3 font-semibold">Status</th>
                  <th className="py-3 text-right font-semibold">Ações</th>
                </tr>
              </thead>
              <tbody>
                {carregando ? (
                  <tr>
                    <td colSpan={6} className="py-10 text-center text-zinc-400">
                      Carregando…
                    </td>
                  </tr>
                ) : users.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-10 text-center text-zinc-400">
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
                      <td className="py-3 text-zinc-500">
                        {u.criadoEm ? formatDate(u.criadoEm) : '—'}
                      </td>
                      <td className="py-3">
                        <span
                          className={cn(
                            'rounded-full px-2 py-0.5 font-semibold text-xs capitalize',
                            statusBadge[u.status],
                          )}
                        >
                          {u.status.toLowerCase()}
                        </span>
                      </td>
                      <td className="py-3">
                        <div className="flex justify-end gap-2">
                          {u.status === 'PENDENTE' && (
                            <>
                              <Button
                                size="sm"
                                onClick={() =>
                                  executar(() => usuariosApi.aprovar(u.id))
                                }
                              >
                                <Check size={14} /> Aprovar
                              </Button>
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() =>
                                  executar(() => usuariosApi.rejeitar(u.id))
                                }
                              >
                                <X size={14} /> Rejeitar
                              </Button>
                            </>
                          )}
                          {u.status === 'APROVADO' &&
                            u.email !== user.email && (
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() =>
                                  executar(() => usuariosApi.desativar(u.id))
                                }
                              >
                                <Power size={14} /> Desativar
                              </Button>
                            )}
                          {u.status === 'DESATIVADO' && (
                            <Button
                              size="sm"
                              onClick={() =>
                                executar(() => usuariosApi.ativar(u.id))
                              }
                            >
                              Reativar
                            </Button>
                          )}
                          {/* REJEITADO: backend não permite reversão direta. */}
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
    </div>
  )
}
