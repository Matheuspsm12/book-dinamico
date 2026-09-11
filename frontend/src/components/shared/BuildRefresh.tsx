"use client";

import { useEffect } from "react";
import { logClientEvent } from "src/lib/client-log";

const CHECK_INTERVAL_MS = 60_000;
const CURRENT_BUILD_ID = process.env.NEXT_PUBLIC_APP_BUILD_ID;

export function BuildRefresh() {
  useEffect(() => {
    if (!CURRENT_BUILD_ID) return;

    let stopped = false;

    async function checkBuild() {
      try {
        const response = await fetch("/app-build", {
          cache: "no-store",
          headers: {
            Accept: "application/json",
          },
        });

        if (!response.ok) return;

        const data = (await response.json()) as { buildId?: string };

        if (!stopped && data.buildId && data.buildId !== CURRENT_BUILD_ID) {
          logClientEvent("build-refresh", {
            previousBuildId: CURRENT_BUILD_ID,
            currentBuildId: data.buildId,
          });
          window.location.reload();
        }
      } catch {
        // Ignore transient network failures; the next interval will check again.
      }
    }

    const intervalId = window.setInterval(checkBuild, CHECK_INTERVAL_MS);

    return () => {
      stopped = true;
      window.clearInterval(intervalId);
    };
  }, []);

  return null;
}
