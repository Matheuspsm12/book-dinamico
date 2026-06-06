// Wrapper de fetch para o backend Book.
// - Injeta Authorization: Bearer <token> quando há sessão
// - Desempacota o envelope ApiErrorBody do GlobalExceptionHandler em ApiError
// - Aceita JSON, multipart (FormData) e respostas Blob

import { getToken, clearSession } from "../auth-storage";
import { friendlyMessage } from "./errors";
import type { ApiErrorBody } from "./types";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8082/book_dinamico";

export class ApiError extends Error {
  readonly status: number;
  readonly chave?: string;

  constructor(status: number, chave: string | undefined, friendly: string) {
    super(friendly);
    this.status = status;
    this.chave = chave;
  }
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: object | FormData | null;
  /** Quando true, retorna Blob (download de arquivos). */
  asBlob?: boolean;
  /** Quando true, NÃO joga erro para 401/403 — caller trata. */
  silent401?: boolean;
  /** Timeout em ms (default: 120s p/ multipart, 30s p/ resto). */
  timeoutMs?: number;
};

async function request<T>(
  method: string,
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { body, asBlob, silent401, timeoutMs, headers: extraHeaders, ...rest } = options;
  const headers = new Headers(extraHeaders);

  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let payload: BodyInit | undefined;
  const isMultipart = body instanceof FormData;
  if (isMultipart) {
    payload = body;
    // NÃO setar Content-Type — o browser coloca multipart/form-data com boundary.
  } else if (body !== undefined && body !== null) {
    headers.set("Content-Type", "application/json");
    payload = JSON.stringify(body);
  }

  // Timeout: 120s pra upload multipart (acomoda cold start do Render free), 30s p/ resto.
  // Sem isso, fetch pode pendurar indefinidamente quando o backend dorme.
  const ctrl = new AbortController();
  const effectiveTimeout = timeoutMs ?? (isMultipart ? 120_000 : 30_000);
  const timer = setTimeout(() => ctrl.abort(), effectiveTimeout);

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      ...rest,
      headers,
      body: payload,
      signal: ctrl.signal,
    });
  } catch (e) {
    clearTimeout(timer);
    if ((e as Error).name === "AbortError") {
      throw new ApiError(
        408,
        "timeout",
        "Tempo esgotado. O servidor pode estar acordando — tente novamente em 1 minuto.",
      );
    }
    throw new ApiError(0, "network", "Falha de rede. Verifique sua conexão e tente novamente.");
  }
  clearTimeout(timer);

  if (!response.ok) {
    // Tenta interpretar envelope ApiErrorBody do backend
    let chave: string | undefined;
    let friendly = response.statusText;
    try {
      const errBody = (await response.json()) as ApiErrorBody;
      chave = errBody.message;
      friendly = friendlyMessage(errBody.message);
    } catch {
      // resposta sem JSON
    }
    if (response.status === 401 && !silent401) {
      // Token inválido/expirado → limpa sessão. O AuthContext fará o redirect.
      clearSession();
    }
    throw new ApiError(response.status, chave, friendly);
  }

  if (response.status === 204) return undefined as T;
  if (asBlob) return (await response.blob()) as T;

  // 200 sem corpo (logout) — devolve null como T se possível
  const text = await response.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as unknown as T;
  }
}

export const api = {
  get:    <T>(path: string, opts?: RequestOptions) => request<T>("GET",    path, opts),
  post:   <T>(path: string, opts?: RequestOptions) => request<T>("POST",   path, opts),
  put:    <T>(path: string, opts?: RequestOptions) => request<T>("PUT",    path, opts),
  delete: <T>(path: string, opts?: RequestOptions) => request<T>("DELETE", path, opts),
};

export { BASE_URL };
