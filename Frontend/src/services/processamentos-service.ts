import type {
  PageResponse,
  ProcessamentoResponse,
  ProcessamentoTipo,
} from "src/lib/api/types";
import api from "src/services/api";

type HateoasPage<T> = {
  _embedded?: Record<string, T[]>;
  page?: {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
  };
};

function normalizePage<T>(data: HateoasPage<T>): PageResponse<T> {
  const content = Object.values(data._embedded ?? {}).find(Array.isArray) ?? [];
  const page = data.page ?? {
    size: content.length,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    number: 0,
  };

  return {
    content,
    totalElements: page.totalElements,
    totalPages: page.totalPages,
    number: page.number,
    size: page.size,
    first: page.number === 0,
    last: page.number + 1 >= page.totalPages,
  };
}

export async function listar(page = 0, size = 20) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  const { data } = await api.get<HateoasPage<ProcessamentoResponse>>(
    `/api/processamentos?${params.toString()}`,
  );
  return normalizePage(data);
}

export async function filtrar(
  tipoProcessamento: ProcessamentoTipo | "",
  page = 0,
  size = 20,
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (tipoProcessamento) {
    params.set("tipoProcessamento", tipoProcessamento);
  }

  const { data } = await api.get<HateoasPage<ProcessamentoResponse>>(
    `/api/processamentos/filtrar?${params.toString()}`,
  );
  return normalizePage(data);
}

export async function baixar(id: number): Promise<Blob> {
  const { data } = await api.get(`/api/processamentos/download/${id}`, {
    responseType: "blob",
  });
  return data as Blob;
}

export async function reprocessar(id: number) {
  await api.get(`/api/processamentos/reprocessar/${id}`);
}
