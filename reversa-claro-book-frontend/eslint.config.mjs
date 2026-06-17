// O Biome (biome.json) é o linter/formatter principal — padrão do template dashboardclaromm_pm,
// e cobre os domínios Next + React. As regras nativas do ESLint ficam desativadas aqui para
// evitar duplicação (mesma estratégia do template, que remove as regras nativas do next).
// O ESLint segue no pipeline (lint: "eslint . && biome check") por consistência com o template.
const eslintConfig = [
  {
    ignores: [
      '.next/**',
      'node_modules/**',
      'dist/**',
      'build/**',
      'next-env.d.ts',
    ],
  },
]

export default eslintConfig
