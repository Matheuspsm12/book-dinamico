import nextCoreWebVitals from "eslint-config-next/core-web-vitals";
import nextTypescript from "eslint-config-next/typescript";

const config = [
  ...nextCoreWebVitals,
  ...nextTypescript,
  {
    rules: {
      "react-hooks/set-state-in-effect": "off",
    },
  },
  {
    files: ["src/**/*.{ts,tsx}"],
    rules: {
      semi: ["error", "always"],
      quotes: ["error", "double"],
    },
  },
  {
    files: ["*.js"],
    rules: {
      "@typescript-eslint/no-require-imports": "off",
    },
  },
];

export default config;
