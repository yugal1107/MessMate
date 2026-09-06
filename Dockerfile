# Multi-stage Dockerfile for MessMate Spring Boot Application

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

# Add labels for metadata
LABEL maintainer="MessMate"
LABEL description="Smart Mess Subscription and Meal Management System"

# # Create a non-root user to run the application
# RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/MessMate-0.0.1-SNAPSHOT.jar app.jar

# Copy Firebase service account file (commented out until private credentials are provided)
# COPY --from=build /app/src/main/resources/firebase/service-account.json /app/firebase/service-account.json

# # Change ownership to the non-root user
# RUN chown -R spring:spring /app

# # Switch to non-root user
# USER spring:spring

# Expose the application port
EXPOSE 8080

# Set JVM options for optimal performance in low-memory containers (Render 512MB limit)
ENV JAVA_OPTS="-Xmx300m -Xms128m -XX:+UseSerialGC"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/v3/api-docs || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
