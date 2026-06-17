// Gera e persiste um identificador estável do dispositivo (fingerprint) em localStorage.
// Enviado como header X-Device-Id pelo axios (services/api.ts) — alinhado ao claim
// `deviceId`/`fp` do JWT no backend (D3 / padrão TCIA).

import FingerprintJS from "@fingerprintjs/fingerprintjs";

const DEVICE_ID_KEY = "deviceId";

export async function ensureDeviceId(): Promise<string | null> {
  if (typeof window === "undefined") return null;

  const existing = window.localStorage.getItem(DEVICE_ID_KEY);
  if (existing) return existing;

  try {
    const fp = await FingerprintJS.load();
    const result = await fp.get();
    window.localStorage.setItem(DEVICE_ID_KEY, result.visitorId);
    return result.visitorId;
  } catch {
    return null;
  }
}
