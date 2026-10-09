# ══════════════════════════════════════════════════════════
# Quini6 Analytics — Backend Dockerfile
# Multi-stage: build → dev → production
# ══════════════════════════════════════════════════════════

# ─── Build stage ─────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
RUN apt-get update && apt-get install -y maven unzip && rm -rf /var/lib/apt/lists/*

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn package -DskipTests -B

# ─── Dev stage (hot-reload via volume mount) ────────────
FROM eclipse-temurin:21-jre-jammy AS dev
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.devtools.restart.enabled=true"]

# ─── Production stage (minimal image) ───────────────────
FROM eclipse-temurin:21-jre-jammy AS production
RUN groupadd -r quini6 && useradd -r -g quini6 quini6
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*
COPY --from=build --chown=quini6:quini6 /app/target/*.jar app.jar
USER quini6
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=40s \
  CMD curl -sf http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
