import axios from "axios";
import { clearSession, getToken } from "src/lib/auth-storage";
import { friendlyMessage } from "src/lib/api/errors";
import type { ApiErrorBody } from "src/lib/api/types";
import { logClientEvent } from "src/lib/client-log";
import { novoTraceId } from "src/lib/trace-id";

export const BASE_URL = process.env.NEXT_PUBLIC_API_URL;

export const SITE_URL = (BASE_URL ?? "").replace(/\/api\/?$/, "");

function resolveBaseURL() {
  if (typeof window === "undefined" || !SITE_URL) return SITE_URL;

  try {
    const configuredUrl = new URL(SITE_URL, window.location.origin);
    if (
      configuredUrl.protocol === window.location.protocol &&
      configuredUrl.hostname === window.location.hostname
    ) {
      return "";
    }
  } catch {
    return SITE_URL;
  }

  return SITE_URL;
}


// 45s dá margem para o backend reiniciando (boot da JVM após deploy leva
// ~30-60s); acima disso a mensagem de "servidor acordando" é esperada.
const DEFAULT_TIMEOUT_MS = 45_000;
const MULTIPART_TIMEOUT_MS = 120_000;

const api = axios.create({
  baseURL: resolveBaseURL(),
  timeout: DEFAULT_TIMEOUT_MS,
  headers: {
    "Content-Type": "application/json",
    "X-Client-type": "web",
  },
});

api.interceptors.request.use((config) => {
  config.headers["X-Correlation-Id"] = novoTraceId();

  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  if (typeof window !== "undefined") {
    const deviceId = window.localStorage.getItem("deviceId");
    if (deviceId) {
      config.headers["X-Device-Id"] = deviceId;
    }
  }

  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    config.timeout = MULTIPART_TIMEOUT_MS;
    delete config.headers["Content-Type"];
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
      const method = error.config?.method?.toUpperCase() ?? "UNKNOWN";
      const url = error.config?.url ?? "unknown";
      const correlationId = error.config?.headers?.["X-Correlation-Id"];
      const refSuffix = correlationId ? ` (ref: ${correlationId})` : "";

      if (error.code === "ECONNABORTED") {
        logClientEvent(
          "api-error",
          {
            method,
            url,
            code: error.code,
            message: "request-timeout",
            correlationId,
          },
          "warn",
        );

        return Promise.reject(
          new Error(
            "Tempo esgotado. O servidor pode estar acordando — tente novamente em 1 minuto.",
          ),
        );
      }
      if (!error.response) {
        logClientEvent(
          "api-error",
          {
            method,
            url,
            code: error.code ?? "NETWORK_ERROR",
            message: "network-error",
            correlationId,
          },
          "warn",
        );

        return Promise.reject(
          new Error("Falha de rede. Verifique sua conexão e tente novamente."),
        );
      }

      const status = error.response.status;
      const rawMessage = readErrorMessage(error.response.data);
      const friendly = friendlyMessage(rawMessage);

      logClientEvent(
        "api-error",
        {
          method,
          url,
          status,
          message: friendly || rawMessage || "api-error",
          correlationId,
        },
        status >= 500 ? "error" : "warn",
      );

      if (status === 401 || status === 403) {
        if (
          typeof window !== "undefined" &&
          !window.location.pathname.includes("/login")
        ) {
          clearSession();
          window.location.href = "/login";
        }
        return Promise.reject(new Error(friendly || "Acesso negado."));
      }

      return Promise.reject(
        new Error(`${friendly || "Erro ao processar a requisição."}${refSuffix}`),
      );
    }

    return Promise.reject(
      new Error("Erro inesperado ao processar a requisição."),
    );
  },
);

function readErrorMessage(data: unknown): string | undefined {
  if (typeof data === "string") return data;
  if (data && typeof data === "object" && "message" in data) {
    return String((data as ApiErrorBody).message);
  }
  return undefined;
}

export default api;
