import type { InputHTMLAttributes } from 'react'
import { forwardRef } from 'react'

import { cn } from '@/lib/utils'

export const Input = forwardRef<
  HTMLInputElement,
  InputHTMLAttributes<HTMLInputElement>
>(function Input({ className, ...rest }, ref) {
  return (
    <input
      ref={ref}
      className={cn(
        'h-10 w-full rounded-md border border-zinc-300 bg-white px-3 text-sm',
        'outline-none focus:border-[var(--claro-red)] focus:ring-2 focus:ring-[var(--claro-red)]/20',
        'disabled:opacity-50',
        className,
      )}
      {...rest}
    />
  )
})
