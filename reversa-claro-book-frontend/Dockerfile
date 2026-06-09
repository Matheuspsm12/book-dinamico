# syntax=docker/dockerfile:1.7
# Build stage — instala deps + builda Next em modo standalone
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
# Inclui optional deps (lightningcss precisa do binário per-platform)
RUN npm ci --include=optional

FROM node:20-alpine AS builder
WORKDIR /app

# NEXT_PUBLIC_* precisa estar disponível em build time (é embutido no JS estático).
# Passado via --build-arg no docker build (docker-compose já injeta localhost).
# Default = localhost (projeto roda 100% local).
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8082/book_dinamico
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL

COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# Runtime stage — só o que o servidor standalone precisa
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup -g 1001 -S nodejs && adduser -S nextjs -u 1001

# Standalone bundle inclui server.js + node_modules mínimos
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/public ./public

USER nextjs

# Render injeta PORT; localmente cai em 3000
ENV PORT=3000
EXPOSE 3000

ENV HOSTNAME=0.0.0.0
CMD ["node", "server.js"]
