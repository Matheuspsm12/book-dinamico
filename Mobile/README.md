# Book Dinâmico Mobile

Aplicativo Expo/React Native para o fluxo inicial do Book Dinâmico: autenticação, dashboard responsivo, menu de navegação, listagem de documentos e download/compartilhamento autenticado.

## Preparação

```bash
cd Mobile
pnpm install --frozen-lockfile
cp .env.example .env.local
```

O projeto fixa `pnpm@10.19.0` no `package.json` e requer Node.js 20.19 ou superior. O arquivo `pnpm-workspace.yaml` mantém a instalação isolada, recusa versões publicadas há menos de 24 horas e falha quando uma dependência tenta executar um script de build não autorizado.

Ajuste `EXPO_PUBLIC_API_URL` conforme o dispositivo. No Android Emulator, `localhost` aponta para o próprio emulador; use `10.0.2.2` para acessar o host. Acrescente o context path do backend quando `BOOK_CONTEXT_PATH` estiver configurado.

## Comandos

```bash
pnpm run start
pnpm run android
pnpm run ios
pnpm run typecheck
pnpm run config
pnpm audit --prod --audit-level high
```

`android` exige Android SDK/emulador. `ios` exige macOS e Xcode.

## Estrutura

- `src/context`: sessão persistida com armazenamento seguro nativo.
- `src/navigation`: troca entre login e área autenticada.
- `src/screens`: login, dashboard inicial e catálogo de books.
- `src/components/brand`: identidade visual e proporções dos SVGs oficiais.
- `src/components/navigation`: cabeçalho, drawer e itens de navegação acessíveis.
- `src/components/download`: feedback visual e controles acessíveis da transferência.
- `src/hooks`: coordenação do ciclo de vida e do estado de downloads.
- `src/navigation/types.ts`: contrato tipado das rotas do aplicativo.
- `assets`: SVGs oficiais (`logo_claro.svg` e `logo_symbol_c.svg`) renderizados nativamente pelo Metro.
- `src/services`: contratos da API, sessão segura e transferência nativa.
- `src/styles/theme.ts`: tokens visuais compartilhados.

## Download de documentos

O aplicativo mantém uma única transferência ativa por vez. Durante o download, exibe progresso e tamanho transferido, permite cancelamento e bloqueia somente atualização, navegação, logout e o início de outra transferência. O arquivo é validado contra o tamanho informado pela API, compartilhado pela folha nativa do sistema e removido do cache ao concluir, cancelar ou falhar.

Para validar em um aparelho físico, teste pelo menos: download completo de cada extensão publicada, cancelamento, toque duplo rápido, tentativa de sair durante a transferência, sessão expirada e perda de rede.

## Escopo atual

- Implementado: login, restauração/expiração local de sessão, dashboard com os primeiros indicadores, menu, logout local, catálogo, refresh, estados de loading/erro/vazio e download autenticado com progresso, cancelamento, validação e compartilhamento nativo.
- Pendente: cadastro, recuperação de senha, fluxos administrativos, testes automatizados e configuração de assinatura/EAS para distribuição.
