# syntax=docker/dockerfile:1.7
# Build stage — usa Maven oficial pra compilar o JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache de dependências: só re-baixa se o pom mudou
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copia o código e empacota (skip tests; CI deve rodar testes separados)
COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime stage — JRE slim
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Cria usuário não-root pra rodar a app
RUN groupadd -r app && useradd -r -g app app
RUN mkdir -p /data/book-dinamico && chown -R app:app /data /app

COPY --from=build --chown=app:app /workspace/target/*.jar app.jar

USER app

# Render injeta PORT em runtime; localmente cai em BOOK_APP_PORT (default 8082)
EXPOSE 8082

# Spring Boot vai bindar em 0.0.0.0:${PORT:-8082} via application.yml
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
