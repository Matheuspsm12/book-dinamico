// PM2 — inicialização do BACKEND (Spring Boot / java -jar).
//
// Objetivo: dar ao deploy um jeito padronizado de subir o backend, igual o
// frontend já faz. Hoje o pipeline reinicia só o `_web`; o processo do
// backend (java -jar) não é iniciado, e o nginx do :8443 responde 502.
//
// Uso:
//   Homologação: pm2 start ecosystem.config.js
//   Produção:    pm2 start ecosystem.config.js --env production
//   Reiniciar:   pm2 restart claro-book-dinamico_api
//
// Pré-requisitos no servidor:
//   - Java 17 no PATH (ou ajuste `interpreter` para o caminho absoluto do java).
//   - O jar já buildado em ./backend/target/ (o JOB_BUILD gera).
//   - O arquivo .env do ambiente presente em ./backend/.env — o Spring o carrega
//     via `spring.config.import: optional:file:.env` (mesmas BOOK_* do app).

module.exports = {
  apps: [
    {
      name: "claro-book-dinamico_api",
      cwd: "./backend",
      script: "java",
      // interpreter "none" -> PM2 executa o `java` direto, sem passar pelo Node.
      interpreter: "none",
      args: "-jar target/book_dinamico_backend-0.0.1-SNAPSHOT.jar",
      instances: 1,
      exec_mode: "fork",
      autorestart: true,
      max_restarts: 10,
      restart_delay: 5000,
      // Perfil Spring: define qual bloco do logback/config é usado.
      // Sem isso o app cai no perfil `dev` (console). Homolog usa `hom`.
      env: {
        SPRING_PROFILES_ACTIVE: "hom",
      },
      env_production: {
        SPRING_PROFILES_ACTIVE: "prod",
      },
    },
  ],
};
