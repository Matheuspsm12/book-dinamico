import axios from "axios";
import type { ApiErroResponse } from "@/types";
import { getToken, clearSession } from "@/lib/auth-storage";
import { friendlyMessage } from "@/lib/friendly-errors";

export const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8082/book_dinamico";

// Timeout default 30s; uploads multipart sobem para 120s (acomoda cold start do Render free).
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

// Injeta token + fingerprint do dispositivo em cada requisição (padrão TCIA / D3).
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

  // Uploads (FormData) podem demorar — estende o timeout só para eles.
  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    config.timeout = MULTIPART_TIMEOUT_MS;
  }

  return config;
});

// Desempacota o envelope ApiErroResponse do GlobalExceptionHandler em mensagem amigável.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
      // Timeout (abort) — backend pode estar "dormindo" no Render free.
      if (error.code === "ECONNABORTED") {
        return Promise.reject(new Error(friendlyMessage("timeout")));
      }
      // Sem resposta = falha de rede.
      if (!error.response) {
        return Promise.reject(new Error(friendlyMessage("network")));
      }

      const status = error.response?.status;
      const rawMessage = (error.response?.data as ApiErroResponse)?.message;
      const friendly = friendlyMessage(rawMessage);

      if (status === 401 || status === 403) {
        // Sessão inválida/expirada: limpa e redireciona (exceto na própria tela de login).
        if (
          typeof window !== "undefined" &&
          !window.location.pathname.includes("/login")
        ) {
          clearSession();
          window.location.href = "/login";
        }
        return Promise.reject(new Error(friendly || "Acesso negado."));
      }

      return Promise.reject(new Error(friendly || "Erro ao processar a requisição."));
    }

    return Promise.reject(new Error("Erro inesperado ao processar a requisição."));
  }
);

export default api;
