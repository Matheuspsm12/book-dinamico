import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Formata data para pt-BR (dd/mm/aaaa).
 *
 * IMPORTANTE: lida com strings ISO "YYYY-MM-DD" sem timezone — o `new Date()`
 * nativo as interpreta como meia-noite UTC, o que provoca off-by-one quando o
 * usuário está em fuso negativo (ex: São Paulo UTC-3 → mostra dia anterior).
 * Aqui parseamos manualmente para tratar como data local.
 */
export function formatDate(d: string | Date) {
  if (d instanceof Date) return d.toLocaleDateString("pt-BR");

  // String ISO "YYYY-MM-DD" → constrói como data local (mês é 0-indexado)
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(d);
  if (m) {
    const local = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
    return local.toLocaleDateString("pt-BR");
  }

  // Outros formatos (com hora/timezone) — deixa o Date nativo
  return new Date(d).toLocaleDateString("pt-BR");
}

/**
 * Extrai um nome amigável de um documento a partir do filename. Lida com
 * formatos comuns que aparecem na operação Claro:
 *   "Book Dinâmico - Alto Giro 18 (3).xlsm"        → "Book Dinâmico"
 *   "Book_Dinamico-Alto_Giro_18.xlsm"               → "Book Dinâmico"
 *   "2026 Maio_Book de Terminais-HFC (1).pptx"      → "Book de Terminais"
 *   "2026 Maio_Book de fontes Claro-HFC (1).pptx"   → "Book de Fontes"
 */
export function inferNomeFromFilename(filename: string): string {
  // 1) tira extensão
  let name = filename.replace(/\.(xlsm|xlsx|pptx)$/i, "");
  // 2) tira prefixo "AAAA Mes_" (qualquer combinação de palavras+underscores no início)
  name = name.replace(/^\d{4}[\s_]+\p{L}+_/iu, "");
  // 3) NORMALIZA: underscores viram espaços (antes dos regex de palavras)
  name = name.replace(/_/g, " ");
  // 4) corta sufixo da operadora " Claro..."
  name = name.replace(/\s+Claro.*$/i, "");
  // 5) corta sufixo técnico "-HFC..."
  name = name.replace(/-HFC.*$/i, "");
  // 6) corta tudo a partir de hífen (com ou sem espaço): "X-Y" e "X - Y" → "X"
  name = name.replace(/\s*-.*$/, "");
  // 7) corta " (N)" de cópias do browser
  name = name.replace(/\s*\(\d+\)\s*$/, "");
  // 8) corta números soltos no fim ("18", "v2")
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

/**
 * Retorna a data de hoje em formato "YYYY-MM-DD" usando timezone LOCAL,
 * não UTC. Usado para popular inputs <input type="date"> e payloads
 * para o backend evitando off-by-one.
 */
export function hojeLocal(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
