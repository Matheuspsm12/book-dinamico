"use client";

import { useEffect } from "react";
import { logClientEvent } from "src/lib/client-log";

export function GlobalErrorHandler() {
  useEffect(() => {
    function onError(event: ErrorEvent) {
      logClientEvent(
        "app-error",
        {
          tipo: "window-error",
          message: event.message ?? "unknown",
          arquivo: event.filename ?? "",
          linha: event.lineno,
          coluna: event.colno,
        },
        "error",
      );
    }

    function onRejection(event: PromiseRejectionEvent) {
      const motivo =
        event.reason instanceof Error
          ? event.reason.message
          : String(event.reason ?? "unknown");
      logClientEvent(
        "app-error",
        {
          tipo: "unhandledrejection",
          message: motivo,
        },
        "error",
      );
    }

    window.addEventListener("error", onError);
    window.addEventListener("unhandledrejection", onRejection);

    return () => {
      window.removeEventListener("error", onError);
      window.removeEventListener("unhandledrejection", onRejection);
    };
  }, []);

  return null;
}