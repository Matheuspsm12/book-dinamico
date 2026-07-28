import axios from "axios";
import { clearSession, getToken } from "src/lib/auth-storage";
import { friendlyMessage } from "src/lib/api/errors";
import type { ApiErrorBody } from "src/lib/api/types";

export const BASE_URL = process.env.NEXT_PUBLIC_API_URL;

const DEFAULT_TIMEOUT_MS = 30_000;
const MULTIPART_TIMEOUT_MS = 120_000;

const api = axios.create({
  baseURL: BASE_URL,
  timeout: DEFAULT_TIMEOUT_MS,
  headers: {
    "Content-Type": "application/json",
    "X-Client-type": "web",
  },
});

api.interceptors.request.use((config) => {
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
      if (error.code === "ECONNABORTED") {
        return Promise.reject(
          new Error(
            "Tempo esgotado. O servidor pode estar acordando — tente novamente em 1 minuto.",
          ),
        );
      }
      if (!error.response) {
        return Promise.reject(
          new Error("Falha de rede. Verifique sua conexão e tente novamente."),
        );
      }

      const status = error.response.status;
      const rawMessage = (error.response.data as ApiErrorBody)?.message;
      const friendly = friendlyMessage(rawMessage);

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
        new Error(friendly || "Erro ao processar a requisição."),
      );
    }

    return Promise.reject(
      new Error("Erro inesperado ao processar a requisição."),
    );
  },
);

export default api;
