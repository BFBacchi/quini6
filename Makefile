# ══════════════════════════════════════════════════════════
# Quini6 Analytics — Makefile
# Shortcuts for common development tasks
# ══════════════════════════════════════════════════════════

.PHONY: help start stop logs test lint format build clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

start: ## Start full stack (docker compose)
	bash scripts/start.sh

start-debug: ## Start with debug tools (pgAdmin, Kibana)
	cd docker && docker compose --env .env -f docker-compose.yml -f docker-compose.override.yml --profile debug up --build

stop: ## Stop all services
	cd docker && docker compose --env .env down

stop-clean: ## Stop and remove volumes
	cd docker && docker compose --env .env down -v

logs: ## Follow logs
	cd docker && docker compose --env .env logs -f --tail=100

logs-backend: ## Follow backend logs only
	cd docker && docker compose --env .env logs -f --tail=100 backend

test: test-backend test-frontend ## Run all tests

test-backend: ## Run backend unit + integration tests
	cd backend && ./mvnw verify

test-frontend: ## Run frontend tests
	cd frontend && npm test

test-e2e: ## Run Playwright e2e tests
	cd frontend && npx playwright test

lint: lint-backend lint-frontend ## Run all linters

lint-backend: ## Check backend code style
	cd backend && ./mvnw spotless:check

lint-frontend: ## Check frontend lint
	cd frontend && npm run lint

format: format-backend format-frontend ## Format all code

format-backend: ## Apply backend formatting
	cd backend && ./mvnw spotless:apply

format-frontend: ## Apply frontend formatting
	cd frontend && npm run format

build: ## Build all Docker images
	cd docker && docker compose --env .env build

db-migrate: ## Run Flyway migrations
	cd backend && ./mvnw flyway:migrate -Dspring.profiles.active=docker

db-info: ## Show Flyway migration status
	cd backend && ./mvnw flyway:info -Dspring.profiles.active=docker

clean: ## Clean build artifacts
	cd backend && ./mvnw clean
	cd frontend && rm -rf dist node_modules
