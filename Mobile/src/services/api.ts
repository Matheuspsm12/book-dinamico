import axios from "axios";

import { readSession } from "../storage/session";
import type { ApiErrorBody } from "../types/api";

const configuredUrl = process.env.EXPO_PUBLIC_API_URL?.trim() ?? "";

export const API_BASE_URL = configuredUrl
  .replace(/\/api\/?$/, "")
  .replace(/\/+$/, "");

export function requireApiUrl() {
  if (!API_BASE_URL) {
    throw new Error(
      "API não configurada. Defina EXPO_PUBLIC_API_URL no arquivo .env.local.",
    );
  }
}

const api = axios.create({
  baseURL: API_BASE_URL || undefined,
  timeout: 30_000,
  headers: {
    "Content-Type": "application/json",
    "X-Client-Type": "mobile",
  },
});

api.interceptors.request.use(async (config) => {
  const session = await readSession();
  if (session?.token) config.headers.Authorization = `Bearer ${session.token}`;
  return config;
});

export function friendlyApiError(error: unknown, fallback: string) {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return fallback;
  if (error.code === "ECONNABORTED") return "A solicitação demorou demais. Tente novamente.";
  if (!error.response) return "Não foi possível conectar à API. Verifique a rede e o endereço configurado.";
  return error.response.data?.message || error.response.data?.error || fallback;
}

export function isUnauthorized(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 401;
}

export default api;
