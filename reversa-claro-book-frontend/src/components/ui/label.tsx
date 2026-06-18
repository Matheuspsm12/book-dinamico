import type { LabelHTMLAttributes } from "react";

import { cn } from "src/lib/utils";

export function Label({
  className,
  ...rest
}: LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label
      className={cn("font-medium text-sm text-zinc-700", className)}
      {...rest}
    />
  );
}
