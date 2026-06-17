'use client'

import { CheckCircle2 } from 'lucide-react'
import Link from 'next/link'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import * as usuariosApi from '@/services/usuarios-service'

export default function CadastroPage() {
  const [form, setForm] = useState({
    nome: '',
    empresa: '',
    email: '',
    justificativa: '',
    senha: '',
  })
  const [err, setErr] = useState<string | null>(null)
  const [ok, setOk] = useState(false)
  const [loading, setLoading] = useState(false)

  function set<K extends keyof typeof form>(k: K, v: string) {
    setForm((f) => ({ ...f, [k]: v }))
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setErr(null)
    setLoading(true)
    try {
      await usuariosApi.cadastrar(form)
      setOk(true)
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Erro ao cadastrar.')
    } finally {
      setLoading(false)
    }
  }

  if (ok) {
    return (
      <div className="text-center">
        <CheckCircle2 className="mx-auto text-emerald-500" size={56} />
        <h1 className="mt-4 font-bold text-2xl text-zinc-800">
          Solicitação enviada!
        </h1>
        <p className="mt-2 text-sm text-zinc-500">
          Sua conta está <strong>pendente</strong> de aprovação por um
          administrador. Você receberá notificação por e-mail assim que for
          liberada.
        </p>
        <Link
          href="/login"
          className="mt-8 inline-block font-semibold text-[var(--claro-red)] hover:underline"
        >
          Voltar para o login
        </Link>
      </div>
    )
  }

  return (
    <div>
      <h1 className="font-bold text-2xl text-zinc-800">Solicitar acesso</h1>
      <p className="mt-1 text-sm text-zinc-500">
        Preencha o formulário — seu acesso será analisado por um admin.
      </p>

      <form onSubmit={onSubmit} className="mt-6 space-y-4">
        <div>
          <Label htmlFor="nome">Nome completo</Label>
          <Input
            id="nome"
            required
            className="mt-1"
            value={form.nome}
            onChange={(e) => set('nome', e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="empresa">Empresa</Label>
          <Input
            id="empresa"
            required
            className="mt-1"
            value={form.empresa}
            onChange={(e) => set('empresa', e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="email">E-mail corporativo</Label>
          <Input
            id="email"
            type="email"
            required
            className="mt-1"
            value={form.email}
            onChange={(e) => set('email', e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="senha">Senha (mínimo 8 caracteres)</Label>
          <Input
            id="senha"
            type="password"
            required
            minLength={8}
            maxLength={72}
            className="mt-1"
            value={form.senha}
            onChange={(e) => set('senha', e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="just">Justificativa de acesso</Label>
          <textarea
            id="just"
            required
            rows={3}
            value={form.justificativa}
            onChange={(e) => set('justificativa', e.target.value)}
            className="mt-1 w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm outline-none focus:border-[var(--claro-red)] focus:ring-2 focus:ring-[var(--claro-red)]/20"
            placeholder="Descreva por que você precisa de acesso aos books técnicos."
          />
        </div>

        {err && (
          <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-red-700 text-sm">
            {err}
          </div>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={loading}>
          {loading ? 'Enviando...' : 'Solicitar acesso'}
        </Button>

        <p className="text-center text-sm text-zinc-500">
          Já tem conta?{' '}
          <Link
            href="/login"
            className="font-semibold text-[var(--claro-red)] hover:underline"
          >
            Entrar
          </Link>
        </p>
      </form>
    </div>
  )
}
