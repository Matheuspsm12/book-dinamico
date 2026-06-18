"use client";
import type { ButtonHTMLAttributes } from "react";
import { forwardRef } from "react";

import { cn } from "src/lib/utils";

type Variant = "primary" | "outline" | "ghost" | "danger";
type Size = "sm" | "md" | "lg";

const variantCls: Record<Variant, string> = {
  primary:
    "bg-[var(--claro-red)] text-white hover:bg-[var(--claro-red-dark)] active:bg-[var(--claro-red-dark)]",
  outline: "border border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-50",
  ghost: "bg-transparent hover:bg-zinc-100 text-zinc-700",
  danger: "bg-red-600 text-white hover:bg-red-700",
};
const sizeCls: Record<Size, string> = {
  sm: "h-8 px-3 text-xs",
  md: "h-10 px-4 text-sm",
  lg: "h-12 px-6 text-base",
};

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
};

export const Button = forwardRef<HTMLButtonElement, Props>(function Button(
  { variant = "primary", size = "md", className, ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-md font-semibold transition disabled:cursor-not-allowed disabled:opacity-50",
        variantCls[variant],
        sizeCls[size],
        className,
      )}
      {...rest}
    />
  );
});
