import type { PageResponse } from "src/lib/api/types";

/**
 * Formato "cru" das respostas paginadas do backend. O Spring Data pode devolver
 * três variações conforme a versão/configuração:
 *
 * - PageImpl "as-is":   { content, totalElements, totalPages, number, size, ... }
 * - PagedModel (VIA_DTO): { content, page: { size, number, totalElements, totalPages }, _links }
 * - HATEOAS (_embedded):  { _embedded: { xxxList: [...] }, page: { ... }, _links }
 */
export type PagePayload<T> = {
  content?: T[];
  _embedded?: Record<string, unknown>;
  page?: {
    size?: number;
    number?: number;
    totalElements?: number;
    totalPages?: number;
  };
  size?: number;
  number?: number;
  totalElements?: number;
  totalPages?: number;
};

function extrairConteudo<T>(payload: PagePayload<T>): T[] {
  if (Array.isArray(payload.content)) return payload.content;

  const embedded = payload._embedded ?? {};
  const lista = Object.values(embedded).find((valor) => Array.isArray(valor));
  return Array.isArray(lista) ? (lista as T[]) : [];
}

/**
 * Normaliza qualquer uma das variações acima para o {@link PageResponse} usado
 * pela aplicação, preservando o contrato consumido pelas telas.
 */
export function normalizePage<T>(
  payload: PagePayload<T> | null | undefined,
): PageResponse<T> {
  const data = payload ?? {};
  const content = extrairConteudo(data);

  const size = data.page?.size ?? data.size ?? content.length;
  const number = data.page?.number ?? data.number ?? 0;
  const totalElements =
    data.page?.totalElements ?? data.totalElements ?? content.length;
  const totalPages =
    data.page?.totalPages ??
    data.totalPages ??
    (content.length > 0 ? 1 : 0);

  return {
    content,
    totalElements,
    totalPages,
    number,
    size,
    first: number === 0,
    last: number + 1 >= totalPages,
  };
}
