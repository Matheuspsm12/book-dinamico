type ClientLogLevel = "info" | "warn" | "error";

type ClientLogPayload = Record<string, string | number | boolean | null | undefined>;

const LOG_THROTTLE_MS = 30_000;
const lastLogByKey = new Map<string, number>();

export function logClientEvent(
  event: "build-refresh" | "api-error",
  payload: ClientLogPayload,
  level: ClientLogLevel = "info",
) {
  if (typeof window === "undefined") return;

  const throttleKey = `${event}:${payload.method ?? ""}:${payload.url ?? ""}:${payload.status ?? payload.code ?? ""}`;
  const now = Date.now();
  const lastLogAt = lastLogByKey.get(throttleKey) ?? 0;

  if (now - lastLogAt < LOG_THROTTLE_MS) return;
  lastLogByKey.set(throttleKey, now);

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
