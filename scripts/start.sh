#!/usr/bin/env bash
# ══════════════════════════════════════════════════════════
# Quini6 Analytics — Stack Orchestrator
# Levanta todo el stack con docker compose y espera healthchecks
# ══════════════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$PROJECT_ROOT/docker"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}"
echo "═══════════════════════════════════════"
echo "  Quini6 Analytics — Starting Stack"
echo "═══════════════════════════════════════"
echo -e "${NC}"

# ─── 1. Setup .env ──────────────────────────────────────
if [ ! -f "$DOCKER_DIR/.env" ]; then
  echo -e "${YELLOW}[setup]${NC} Copying .env.example → .env"
  cp "$DOCKER_DIR/.env.example" "$DOCKER_DIR/.env"
else
  echo -e "${GREEN}[setup]${NC} .env already exists"
fi

# ─── 2. Build & Start ──────────────────────────────────
echo -e "${CYAN}[docker]${NC} Building and starting services..."
cd "$DOCKER_DIR"
docker compose --env .env \
  -f docker-compose.yml \
  -f docker-compose.override.yml \
  up --build -d 2>&1

# ─── 3. Health checks ──────────────────────────────────
echo ""
echo -e "${CYAN}[health]${NC} Waiting for services to be healthy..."

wait_for_http() {
  local name="$1"
  local url="$2"
  local max_wait="${3:-120}"
  local elapsed=0
  echo -n "  $name "
  while ! curl -sf "$url" > /dev/null 2>&1; do
    sleep 2
    elapsed=$((elapsed + 2))
    if [ "$elapsed" -ge "$max_wait" ]; then
      echo -e " ${RED}TIMEOUT${NC} (${max_wait}s)"
      return 1
    fi
    echo -n "."
  done
  echo -e " ${GREEN}✓${NC} (${elapsed}s)"
}

wait_for_http "PostgreSQL"    "http://localhost:${POSTGRES_PORT:-5432}" 10 || \
  echo -e "  ${YELLOW}PostgreSQL${NC} — checking raw TCP instead..."
wait_for_http "Elasticsearch" "http://localhost:${ELASTICSEARCH_PORT:-9200}/_cluster/health" 60
wait_for_http "RabbitMQ Mgmt" "http://localhost:${RABBITMQ_MGMT_PORT:-15672}" 30 || \
  echo -e "  ${YELLOW}RabbitMQ${NC} — management UI not ready, continuing..."
wait_for_http "Backend API"   "http://localhost:${BACKEND_PORT:-8080}/actuator/health" 90
wait_for_http "Frontend"      "http://localhost:3000" 30

# ─── 4. Summary ────────────────────────────────────────
echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}  All Services Ready!${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "  Frontend:       ${CYAN}http://localhost:3000${NC}"
echo -e "  Backend API:    ${CYAN}http://localhost:${BACKEND_PORT:-8080}/api${NC}"
echo -e "  Swagger UI:     ${CYAN}http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html${NC}"
echo -e "  Actuator:       ${CYAN}http://localhost:${BACKEND_PORT:-8080}/actuator${NC}"
echo -e "  PostgreSQL:     ${CYAN}localhost:${POSTGRES_PORT:-5432}${NC}"
echo -e "  Elasticsearch:  ${CYAN}localhost:${ELASTICSEARCH_PORT:-9200}${NC}"
echo -e "  RabbitMQ Mgmt:  ${CYAN}http://localhost:${RABBITMQ_MGMT_PORT:-15672}${NC}"
echo -e "  pgAdmin:        ${CYAN}http://localhost:5050${NC} (profile: debug)"
echo -e "  Kibana:         ${CYAN}http://localhost:5601${NC} (profile: debug)"
echo -e "${GREEN}═══════════════════════════════════════${NC}"

# ─── 5. Follow logs ────────────────────────────────────
echo ""
echo -e "${CYAN}[logs]${NC} Following logs (Ctrl+C to stop)..."
docker compose --env .env logs -f --tail=50 \
  --prefix="│ " \
  postgres elasticsearch rabbitmq backend frontend
