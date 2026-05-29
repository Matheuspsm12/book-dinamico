import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Standalone build → server.js minimal + node_modules copiados pra .next/standalone.
  // Reduz a imagem Docker drasticamente (sem npm install no runtime).
  output: "standalone",
};

export default nextConfig;
