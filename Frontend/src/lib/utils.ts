import type { ClassValue } from "clsx";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(d: string | Date) {
  if (d instanceof Date) return d.toLocaleDateString("pt-BR");

  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(d);
  if (m) {
    const local = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
    return local.toLocaleDateString("pt-BR");
  }

  return new Date(d).toLocaleDateString("pt-BR");
}

export function inferNomeFromFilename(filename: string): string {
  let name = filename.replace(/\.(xlsm|xlsx|pptx)$/i, "");
  name = name.replace(/^\d{4}[\s_]+\p{L}+_/iu, "");
  name = name.replace(/_/g, " ");
  name = name.replace(/\s+Claro.*$/i, "");
  name = name.replace(/-HFC.*$/i, "");
  name = name.replace(/\s*-.*$/, "");
  name = name.replace(/\s*\(\d+\)\s*$/, "");
  name = name.replace(/\s+v?\d+\s*$/i, "");
  name = name.trim();

  const minusculas = new Set(["de", "da", "do", "das", "dos", "e", "a", "o"]);
  return name
    .split(/\s+/)
    .filter(Boolean)
    .map((w, i) => {
      const lower = w.toLowerCase();
      if (i > 0 && minusculas.has(lower)) return lower;
      return lower.charAt(0).toUpperCase() + lower.slice(1);
    })
    .join(" ");
}

export function hojeLocal(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
