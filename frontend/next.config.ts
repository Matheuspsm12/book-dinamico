import type { NextConfig } from "next";
import path from "node:path";

const configuredApiUrl =
  process.env.BACKEND_API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "";
const backendUrl = configuredApiUrl.replace(/\/api\/?$/, "").replace(/\/$/, "");
const buildId =
  process.env.NEXT_PUBLIC_APP_BUILD_ID ??
  process.env.VERCEL_GIT_COMMIT_SHA ??
  new Date().toISOString().replace(/[^0-9A-Za-z_-]/g, "-");

const nextConfig: NextConfig = {
  generateBuildId: async () => buildId,
  env: {
    NEXT_PUBLIC_APP_BUILD_ID: buildId,
  },
  outputFileTracingRoot: path.join(__dirname, ".."),
  async rewrites() {
    if (!backendUrl) return [];

    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
      {
        source: "/autenticacao/:path*",
        destination: `${backendUrl}/autenticacao/:path*`,
      },
    ];
  },
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          {
            key: "Strict-Transport-Security",
            value: "max-age=31536000; includeSubDomains",
          },
          {
            key: "X-Content-Type-Options",
            value: "nosniff",
          },
          {
            key: "X-Frame-Options",
            value: "SAMEORIGIN",
          },
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
          {
            key: "X-Permitted-Cross-Domain-Policies",
            value: "none",
          },
          {
            key: "Cross-Origin-Opener-Policy",
            value: "same-origin",
          },
          // Sem COEP require-corp: o app consome uma API de outra origem
          // (front :443 -> back :8443). require-corp bloquearia esses recursos.
        ],
      },
    ];
  },
};

export default nextConfig;
