export type ClientLogLevel = "info" | "warn" | "error";

export type ClientLogPayload = Record<
  string,
  string | number | boolean | null | undefined
>;

export type ClientLogEvent =
  | "build-refresh"
  | "api-error"
  | "app-error"
  | "upload-start"
  | "upload-progress"
  | "upload-success"
  | "upload-failed";

const LOG_THROTTLE_MS = 30_000;
const lastLogByKey = new Map<string, number>();

const UPLOAD_MILESTONES = [25, 50, 75, 100] as const;
const uploadMilestonesLogged = new Set<string>();

export function logClientEvent(
  event: ClientLogEvent,
  payload: ClientLogPayload,
  level: ClientLogLevel = "info",
) {
  if (typeof window === "undefined") return;

  const throttleKey = `${event}:${payload.method ?? ""}:${payload.url ?? ""}:${payload.status ?? payload.code ?? ""}`;
  const now = Date.now();
  const lastLogAt = lastLogByKey.get(throttleKey) ?? 0;

  if (now - lastLogAt < LOG_THROTTLE_MS) return;
  lastLogByKey.set(throttleKey, now);

  enviar(event, payload, level);
}

function enviar(
  event: ClientLogEvent,
  payload: ClientLogPayload,
  level: ClientLogLevel,
) {
  if (typeof window === "undefined") return;

  const body = JSON.stringify({
    event,
    level,
    payload: {
      ...payload,
      path: window.location.pathname,
      userAgent: window.navigator.userAgent,
    },
  });

  try {
    if (window.navigator.sendBeacon) {
      const sent = window.navigator.sendBeacon(
        "/client-log",
        new Blob([body], { type: "application/json" }),
      );

      if (sent) return;
    }

    void fetch("/client-log", {
      method: "POST",
      body,
      cache: "no-store",
      keepalive: true,
      headers: {
        "Content-Type": "application/json",
      },
    });
  } catch {
    // Logging must never affect the user's workflow.
  }
}

/** Marca o início de um upload (resetando os marcos de progresso do fluxo). */
export function logUploadStart(
  fluxo: string,
  bytes: number,
  nomeArquivo: string,
) {
  for (const key of [...uploadMilestonesLogged]) {
    if (key.startsWith(`${fluxo}:`)) uploadMilestonesLogged.delete(key);
  }
  enviar("upload-start", { fluxo, bytes, nomeArquivo }, "info");
}

/** Registra o cruzamento de marcos (25/50/75/100%) sem repetir. */
export function logUploadProgress(fluxo: string, percentual: number) {
  const pct = Math.round(percentual);
  for (const marco of UPLOAD_MILESTONES) {
    const chave = `${fluxo}:${marco}`;
    if (pct >= marco && !uploadMilestonesLogged.has(chave)) {
      uploadMilestonesLogged.add(chave);
      enviar("upload-progress", { fluxo, percentual: marco }, "info");
    }
  }
}

export function logUploadSuccess(
  fluxo: string,
  bytes: number,
  duracaoMs: number,
) {
  enviar("upload-success", { fluxo, bytes, duracaoMs }, "info");
}

export function logUploadFailed(
  fluxo: string,
  bytes: number,
  duracaoMs: number,
  motivo: string,
) {
  enviar("upload-failed", { fluxo, bytes, duracaoMs, motivo }, "error");
}