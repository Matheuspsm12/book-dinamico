import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Standalone build → server.js minimal + node_modules copiados pra .next/standalone.
  // Reduz a imagem Docker drasticamente (sem npm install no runtime).
  output: "standalone",
  async headers() {
    return [
      {
        source: "/(.*)", // todas as rotas (alinhado ao ReferrerPolicyFilter do backend)
        headers: [
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
