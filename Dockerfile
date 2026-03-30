# syntax=docker/dockerfile:1.7

# Multi-stage Dockerfile for MessMate Spring Boot Application

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Pre-fetch dependencies; cache Maven local repo between builds
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline

# Copy source code and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime
# Hardened runtime option (switch when you no longer need in-container health tooling):
# FROM gcr.io/distroless/java21-debian12:nonroot AS runtime

LABEL maintainer="MessMate"
LABEL description="Smart Mess Subscription and Meal Management System"

WORKDIR /app

# Copy only the built artifact; do not copy secrets into image layers
COPY --from=build /app/target/MessMate-0.0.1-SNAPSHOT.jar /app/app.jar

# Install healthcheck dependency and create non-root user
RUN apk add --no-cache wget \
    && addgroup -S spring \
    && adduser -S spring -G spring \
    && chown -R spring:spring /app

# Explicitly run as non-root
USER spring:spring

EXPOSE 8080

# Container-aware memory settings
ENV JAVA_OPTS="-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=75"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
