# syntax=docker/dockerfile:1.7

# Multi-stage Dockerfile for MessMate Spring Boot Application

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy build descriptors first so dependency layers can be cached.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Reuse the Maven repository between BuildKit builds.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="MessMate"
LABEL description="Smart Mess Subscription and Meal Management System"

WORKDIR /app

# Run as a non-root user. BusyBox wget (used by the healthcheck) ships with Alpine.
RUN addgroup -S spring && adduser -S spring -G spring

# Copy only the application artifact; secrets are supplied at runtime.
# --chown sets ownership in the same layer, avoiding a duplicate JAR layer from chown -R.
COPY --from=build --chown=spring:spring /app/target/MessMate-0.0.1-SNAPSHOT.jar /app/app.jar

USER spring:spring

EXPOSE 8080

# Keep enough memory available for the VPS OS, Docker, and Cloudflare Tunnel.
ENV JAVA_OPTS="-Xms128m -Xmx350m -XX:+UseSerialGC"

# The project does not currently include Spring Boot Actuator, so use the
# existing public OpenAPI endpoint as the container health check.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/v3/api-docs || exit 1

# Replace the shell with Java so it receives container signals directly.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
