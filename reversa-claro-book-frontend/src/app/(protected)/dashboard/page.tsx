'use client'

import {
  CheckCircle2,
  Clock,
  FileSpreadsheet,
  FileText,
  Users,
  X,
} from 'lucide-react'
import { useEffect, useState } from 'react'

import { PageHeader } from '@/components/PageHeader'
import { StatCard } from '@/components/StatCard'
import * as docsApi from '@/lib/api/documentos'
import type { DocumentoResponse } from '@/lib/api/types'
import * as usuariosApi from '@/lib/api/usuarios'

type Counts = {
  documentos: number
  pendentes: number
  aprovados: number
  rejeitados: number
}

export default function DashboardPage() {
  const [docs, setDocs] = useState<DocumentoResponse[]>([])
  const [counts, setCounts] = useState<Counts>({
    documentos: 0,
    pendentes: 0,
    aprovados: 0,
    rejeitados: 0,
  })
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true
    Promise.all([
      docsApi.listar(),
      usuariosApi.paginar({ status: 'PENDENTE' }, 0, 1),
      usuariosApi.paginar({ status: 'APROVADO' }, 0, 1),
      usuariosApi.paginar({ status: 'REJEITADO' }, 0, 1),
    ])
      .then(([documentos, pend, aprov, rej]) => {
        if (!mounted) return
        setDocs(documentos)
        setCounts({
          documentos: documentos.length,
          pendentes: pend.totalElements,
          aprovados: aprov.totalElements,
          rejeitados: rej.totalElements,
        })
      })
      .catch((e) => {
        if (mounted)
          setErr(e instanceof Error ? e.message : 'Erro ao carregar dashboard.')
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => {
      mounted = false
    }
  }, [])

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle="Visão geral do portal Book Dinâmico."
      />

      {err && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-2 text-red-700 text-sm">
          {err}
        </div>
      )}

      <section className="mb-8">
        <h2 className="mb-4 font-semibold text-sm text-zinc-800">Usuários</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            supraLabel="USUÁRIOS"
            label="Aprovados"
            value={loading ? '…' : counts.aprovados}
            icon={CheckCircle2}
          />
          <StatCard
            supraLabel="USUÁRIOS"
            label="Pendentes"
            value={loading ? '…' : counts.pendentes}
            icon={Clock}
          />
          <StatCard
            supraLabel="USUÁRIOS"
            label="Rejeitados"
            value={loading ? '…' : counts.rejeitados}
            icon={X}
          />
          <StatCard
            supraLabel="CATÁLOGO"
            label="Documentos publicados"
            value={loading ? '…' : counts.documentos}
            icon={Users}
          />
        </div>
      </section>

      <section>
        <h2 className="mb-4 font-semibold text-sm text-zinc-800">
          Catálogo atual de documentos
        </h2>
        {loading ? (
          <p className="text-sm text-zinc-500">Carregando…</p>
        ) : docs.length === 0 ? (
          <p className="text-sm text-zinc-500">
            Nenhum documento publicado ainda. Vá em <strong>Upload Book</strong>{' '}
            para criar o primeiro.
          </p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {docs.map((d) => (
              <StatCard
                key={d.id}
                supraLabel={d.extensao}
                label="Documento"
                value={d.nome}
                valueClassName="text-2xl leading-tight"
                icon={d.extensao === 'PPTX' ? FileText : FileSpreadsheet}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
