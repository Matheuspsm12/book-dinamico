import type { LabelHTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

export function Label({
  className,
  ...rest
}: LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    // biome-ignore lint/a11y/noLabelWithoutControl: primitivo reutilizável — htmlFor/controle vem do consumidor via {...rest}
    <label
      className={cn('font-medium text-sm text-zinc-700', className)}
      {...rest}
    />
  )
}
