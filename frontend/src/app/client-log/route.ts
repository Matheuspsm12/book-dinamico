import { NextRequest, NextResponse } from "next/server";

type ClientLogBody = {
  event?: string;
  level?: "info" | "warn" | "error";
  payload?: Record<string, unknown>;
};

const ALLOWED_EVENTS = new Set(["build-refresh", "api-error"]);

export async function POST(request: NextRequest) {
  let body: ClientLogBody;

  try {
    body = (await request.json()) as ClientLogBody;
  } catch {
    return NextResponse.json({ ok: false }, { status: 400 });
  }

  if (!body.event || !ALLOWED_EVENTS.has(body.event)) {
    return NextResponse.json({ ok: false }, { status: 400 });
  }

  const level = body.level ?? "info";
  const logPayload = {
    event: body.event,
    buildId: process.env.NEXT_PUBLIC_APP_BUILD_ID ?? "unknown",
    at: new Date().toISOString(),
    ...sanitizePayload(body.payload),
  };

  const message = `[frontend] ${body.event} ${JSON.stringify(logPayload)}`;

  if (level === "error") console.error(message);
  else if (level === "warn") console.warn(message);
  else console.info(message);

  return NextResponse.json({ ok: true });
}

function sanitizePayload(payload: unknown) {
  if (!payload || typeof payload !== "object") return {};

  const blockedKeys = new Set(["token", "authorization", "password", "senha"]);
  const entries = Object.entries(payload as Record<string, unknown>).filter(
    ([key]) => !blockedKeys.has(key.toLowerCase()),
  );

  return Object.fromEntries(entries);
}
