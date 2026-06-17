import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Regras de estilo do padrão TCIA (espelha .eslintrc.json do reversa-claro-devolucao-web):
  // ponto-e-vírgula obrigatório e aspas duplas. @typescript-eslint já vem via nextTs.
  {
    rules: {
      semi: ["error", "always"],
      quotes: ["error", "double"],
      // Next 16 traz esta regra (a referência em Next 15 não tem). Carregar sessão/dados
      // no mount via efeito é padrão aceito aqui — mantemos como aviso, não erro.
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    // Arquivos de configuração (não são código de app):
    "*.config.js",
    "*.config.mjs",
    "*.config.ts",
  ]),
]);

export default eslintConfig;
