'use client'

import { usePathname } from 'next/navigation'

import { Sidebar } from '@/components/Sidebar'
import { useAuth } from '@/contexts/AuthContext'

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const { user, loading } = useAuth()
  const pathname = usePathname()
  const fullBleed = pathname === '/book'

  if (loading || !user) return null
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className="flex-1 pl-64 transition-all duration-200 max-[1024px]:pl-20">
        {fullBleed ? (
          <div className="min-h-screen">{children}</div>
        ) : (
          <div className="mx-auto max-w-7xl px-8 py-8">{children}</div>
        )}
      </main>
    </div>
  )
}
