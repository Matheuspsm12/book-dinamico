import type { LucideIcon } from "lucide-react";

import { cn } from "src/lib/utils";

type Props = {
  label: string;
  value: number | string;
  icon: LucideIcon;
  supraLabel?: string;
  meta?: { label: string; value: number | string }[];
  className?: string;
  valueClassName?: string;
};

export function StatCard({
  label,
  value,
  icon: Icon,
  supraLabel = "DOWNLOADS",
  meta,
  className,
  valueClassName,
}: Props) {
  return (
    <div
      className={cn(
        "flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm",
        "min-w-[200px]",
        className,
      )}
    >
      <div className="grid h-12 w-12 place-items-center rounded-full bg-zinc-100 text-zinc-700">
        <Icon size={22} />
      </div>
      <div>
        <p className="font-semibold text-[11px] text-zinc-500 uppercase tracking-wider">
          {supraLabel}
        </p>
        <p className="font-bold text-[13px] text-zinc-700 uppercase leading-tight">
          {label}
        </p>
      </div>
      <p className={cn("font-bold text-4xl text-zinc-900", valueClassName)}>
        {value}
      </p>
      {meta && meta.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {meta.map((m) => (
            <span
              key={m.label}
              className="inline-flex rounded-md bg-zinc-100 px-2 py-1 font-medium text-[11px] text-zinc-600"
            >
              {m.value === "" ? m.label : `${m.label}: ${m.value}`}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
