# --- MessMate Backend Makefile ---

# Variables with defaults (override via command line, e.g., make docker-build TAG=v4)
IMAGE_NAME ?= devyugal1107/mess-java-backend
TAG ?= latest

# Default goal when running just 'make'
.DEFAULT_GOAL := help

# Auto-detect a JDK 21 install on this machine so builds work the same way
# regardless of what `java` on PATH points to. See scripts/find-java21.sh.
JAVA21_HOME := $(shell ./scripts/find-java21.sh)

# Fails fast with a clear message if no JDK 21 was found on this machine.
define require_java21
	@if [ -z "$(JAVA21_HOME)" ]; then \
		echo "No JDK 21 found on this machine. Install one (e.g. Temurin 21) and re-run." >&2; \
		exit 1; \
	fi
endef

# Help command to list available targets
help:
	@echo "Available commands:"
	@echo "  make run         - Run the application in PRODUCTION mode (loads .env)"
	@echo "  make dev         - Run the application in DEVELOPMENT mode (loads .env)"
	@echo "  make build       - Build the application (JAR file)"
	@echo "  make start       - Run the already BUILT JAR file (Production style)"
	@echo "  make clean       - Clean the build directory"
	@echo "  make test        - Run tests"
	@echo "  make docker-build - Build Docker image (optional: make docker-build TAG=v4)"
	@echo "  make docker-run   - Run Docker container in background (optional: make docker-run TAG=v4)"
	@echo "  make docker-stop  - Stop and remove the running Docker container"
	@echo "  make db-up        - Start local Postgres container (for dev)"
	@echo "  make db-down      - Stop local Postgres container (data is kept)"
	@echo "  make db-logs      - Tail local Postgres container logs"
	@echo "  make db-restore-from-prod - Copy current Supabase data into local Postgres"

# Run in production mode
run:
	$(require_java21)
	@set -a; . ./.env && set +a && JAVA_HOME=$(JAVA21_HOME) ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Run in development mode
dev:
	$(require_java21)
	@set -a; . ./.env && set +a && JAVA_HOME=$(JAVA21_HOME) ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build the project
build:
	$(require_java21)
	JAVA_HOME=$(JAVA21_HOME) ./mvnw clean package -DskipTests

# Run the built JAR file directly
start:
	$(require_java21)
	@set -a; . ./.env && set +a && $(JAVA21_HOME)/bin/java -jar target/MessMate-0.0.1-SNAPSHOT.jar

# Clean the project
clean:
	$(require_java21)
	JAVA_HOME=$(JAVA21_HOME) ./mvnw clean

# Run tests
test:
	$(require_java21)
	JAVA_HOME=$(JAVA21_HOME) ./mvnw test

# Build Docker image
docker-build:
	docker build -t $(IMAGE_NAME):$(TAG) .

# Stop and remove the existing container if it exists
docker-stop:
	docker stop messmate-backend || true
	docker rm messmate-backend || true

# Run Docker container in production mode with environment variables
docker-run:
	@set -a; . ./.env && set +a && \
	docker run -d \
		--name messmate-backend \
		--restart unless-stopped \
		-p 8080:8080 \
		--env-file .env \
		-e DB_PROD_URL="$$DB_PROD_URL" \
		-e EMAIL_FROM="$$EMAIL_FROM" \
		-e SPRING_PROFILES_ACTIVE=prod \
		$(IMAGE_NAME):$(TAG)

# Start local Postgres container for dev (matches DB_DEV_URL in .env)
db-up:
	docker compose up -d postgres

# Stop local Postgres container (named volume keeps the data)
db-down:
	docker compose stop postgres

# Tail local Postgres container logs
db-logs:
	docker compose logs -f postgres

# Copy current Supabase (prod) data into the local Postgres container.
# Run after `docker compose up -d --build`. Requires DB_PROD_URL in .env.
db-restore-from-prod:
	./scripts/restore-from-prod.sh
