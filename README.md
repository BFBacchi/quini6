# Quini6 Analytics

Plataforma full-stack de análisis estadístico histórico del Quini 6 (lotería argentina).

> **Disclaimer:** Esta plataforma realiza únicamente análisis descriptivo sobre datos pasados. Los sorteos del Quini 6 son eventos independientes (i.i.d.). Ningún resultado pasado predice resultados futuros. El generador de combinaciones es de entretenimiento solamente.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                         │
│              http://localhost:3000                               │
├─────────────────────────────────────────────────────────────────┤
│                        Backend (Spring Boot)                    │
│              http://localhost:8082                               │
├──────────┬──────────────┬───────────────┬───────────────────────┤
│ Postgres │ Elasticsearch│   RabbitMQ    │    Prometheus         │
│  :5434   │    :9200     │   :5672       │      :9090            │
├──────────┴──────────────┴───────────────┼───────────────────────┤
│                                         │     Grafana :3001     │
│                                         │     Kibana  :5601     │
└─────────────────────────────────────────┴───────────────────────┘
```

## Stack Tecnológico

### Backend

| Componente | Tecnología |
|------------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.2 |
| ORM | Spring Data JPA / Hibernate |
| DB | PostgreSQL 16 |
| Migraciones | Flyway |
| Search | Elasticsearch 8.14 |
| Messaging | RabbitMQ 3.13 |
| Auth | Spring Security + JWT (jjwt 0.12.6) |
| Resilience | Resilience4j 2.2.0 |
| API Docs | SpringDoc OpenAPI 2.6.0 |
| Metrics | Micrometer + Prometheus |
| Scraping | Jsoup 1.18.1 |
| Code Style | Palantir Java Format (Spotless) |
| Tests | JUnit 5 + Testcontainers |

### Frontend

| Componente | Tecnología |
|------------|------------|
| Framework | React 18 + TypeScript 5.5 |
| Build | Vite 5.4 |
| Routing | React Router 6.26 |
| HTTP | Axios 1.7 |
| Charts | Recharts 2.12 |
| Testing | Vitest + Testing Library |
| Linting | ESLint + Prettier |

### Infraestructura

| Componente | Tecnología |
|------------|------------|
| Containers | Docker (multi-stage builds) |
| Orchestration | Docker Compose |
| Reverse Proxy | Nginx 1.25 |
| Metrics | Prometheus 2.53 |
| Dashboards | Grafana 11.1 |
| Log Analytics | Kibana 8.14 |
| DB Admin | pgAdmin (debug profile) |

---

## Getting Started

### Prerrequisitos

- Java 21+
- Node.js 20+
- Docker + Docker Compose
- Maven 3.9+ (o usar el wrapper `./mvnw`)

### Instalación Rápida

```bash
# Clonar el repositorio
git clone https://github.com/TU_USUARIO/quini6-analytics.git
cd quini6-analytics

# Copiar variables de entorno
cp docker/.env.example docker/.env

# Levantar toda la infraestructura
docker compose up -d

# Verificar que todos los servicios estén arriba
docker compose ps
```

### Desarrollo Local

```bash
# Terminal 1: Levantar infraestructura (PG, ES, RabbitMQ)
cd docker
docker compose up -d postgres elasticsearch rabbitmq

# Terminal 2: Backend
cd backend
./mvnw spring-boot:run

# Terminal 3: Frontend
cd frontend
npm install
npm run dev
```

### URLs de Desarrollo

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| Frontend | http://localhost:3000 | - |
| Backend API | http://localhost:8082 | - |
| Swagger UI | http://localhost:8082/swagger-ui.html | - |
| PostgreSQL | localhost:5434 | `quini6` / `quini6_dev_2026` |
| Elasticsearch | http://localhost:9200 | `elastic` / `quini6_dev_2026` |
| RabbitMQ Management | http://localhost:15672 | `quini6` / `quini6_dev_2026` |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3001 | `admin` / `admin` |
| Kibana | http://localhost:5601 | `elastic` / `quini6_dev_2026` |

---

## Estructura del Proyecto

```
quini6/
├── backend/                    # Spring Boot API
│   ├── src/main/java/
│   │   └── com/quini6/analytics/
│   │       ├── controller/     # REST controllers (6)
│   │       ├── service/        # Business logic
│   │       ├── domain/         # Entities, enums, repositories
│   │       ├── dto/            # Data transfer objects
│   │       ├── config/         # Security, OpenAPI, RabbitMQ, Resilience4j
│   │       ├── client/         # External API + Web Scraping
│   │       ├── messaging/      # RabbitMQ publishers/listeners
│   │       ├── elasticsearch/  # ES indexing service
│   │       ├── security/       # JWT filter + provider
│   │       └── audit/          # Audit logging
│   └── src/main/resources/
│       ├── application.yml     # Main config
│       └── db/migration/       # Flyway SQL migrations
├── frontend/                   # React SPA
│   └── src/
│       ├── components/
│       │   ├── Dashboard/      # Statistical dashboard
│       │   ├── Heatmap/        # Frequency heatmap
│       │   ├── GeneradorCombinaciones/  # Combination generator
│       │   ├── BuscadorSorteos/ # Draw search
│       │   ├── Monitoring/     # System monitoring
│       │   └── common/         # Navbar, DisclaimerBanner
│       └── services/api.ts     # Typed API client
├── docker/                     # Docker Compose + configs
│   ├── docker-compose.yml
│   ├── prometheus/
│   └── grafana/
├── scripts/                    # Utility scripts
├── .github/workflows/          # CI/CD pipeline
├── AGENTS.md                   # Coding conventions
├── Makefile                    # Dev shortcuts
└── package.json                # Monorepo scripts
```

---

## API Endpoints

### Públicos (GET)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/sorteos` | Listar todos los sorteos |
| GET | `/api/sorteos/{num}` | Sorteo por número |
| GET | `/api/sorteos/rango?desde=&hasta=` | Sorteos por rango de fechas |
| GET | `/api/estadisticas/frecuencia?modalidad=` | Frecuencia de números |
| GET | `/api/estadisticas/chi-cuadrado?modalidad=` | Test de uniformidad |
| GET | `/api/generador/aleatorio` | Combinación aleatoria |
| GET | `/api/generador/ponderado?modalidad=` | Combinación ponderada |
| GET | `/api/monitoring/health` | Health check |
| GET | `/api/monitoring/database` | Estadísticas de DB |
| GET | `/api/monitoring/database/{table}` | Datos de tabla |
| GET | `/api/monitoring/database/schema` | Schema completo |

### Autenticados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login (retorna JWT) |
| POST | `/api/admin/ingesta/sorteo/{num}` | Ingerir sorteo (ADMIN) |
| POST | `/api/admin/ingesta/rango` | Ingerir rango (ADMIN) |

Documentación interactiva: http://localhost:8082/swagger-ui.html

---

## Base de Datos

### Schema (5 tablas)

```sql
usuarios          -- Auth JWT (UUID PK, username, password_hash, role)
sorteos           -- Encabezados de sorteo (UUID PK, numero_sorteo, fecha)
resultados        -- Resultados por modalidad (UUID PK, sorteo_id FK, modalidad, numero_1..6)
numeros_sorteados -- Tabla aplanada para analytics (BIGSERIAL PK, resultado_id FK, numero)
audit_log         -- Trail de auditoría (BIGSERIAL PK, entidad, accion, actor, timestamp)
```

### Modalidades

| Modalidad | Descripción |
|-----------|-------------|
| `TRADICIONAL` | Primer sorteo del día |
| `SEGUNDA` | La Segunda del Quini |
| `REVANCHA` | Sorteo Revancha |
| `SIEMPRE_SALE` | Siempre Sale |
| `POZO_EXTRA` | Pozo Extra |

---

## Docker

### Servicios

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| postgres | 5434 | PostgreSQL 16 |
| elasticsearch | 9200 | Elasticsearch 8.14 |
| rabbitmq | 5672, 15672 | RabbitMQ 3.13 + Management |
| backend | 8082 | Spring Boot API |
| frontend | 3000 | React (Nginx) |
| prometheus | 9090 | Métricas |
| grafana | 3001 | Dashboards |
| kibana | 5601 | Log analytics |

### Comandos

```bash
# Levantar todo
docker compose up -d

# Ver logs
docker compose logs -f backend

# Parar todo
docker compose down

# Parar y limpiar volúmenes
docker compose down -v
```

---

## Monitoreo

### Grafana Dashboard

Dashboard pre-configurado con 11 paneles:

- HTTP Requests/segundo
- Response Time (p95)
- Status Codes (pie chart)
- JVM Memory Used
- JVM GC Pause
- HikariCP Connections
- Resilience4j Circuit Breaker
- Sorteos Ingeridos (counter)
- Active Threads
- System Uptime
- HTTP Request Duration (avg)

### Prometheus

Scrape cada 15s, retención 30 días. Targets:
- `quini6-backend` (actuator/prometheus)
- `prometheus` (self)
- `grafana`

---

## Testing

```bash
# Backend: unit tests
cd backend
./mvnw test

# Backend: integration tests (requiere Docker)
./mvnw verify -Pintegration

# Frontend: unit tests
cd frontend
npm run test

# Frontend: type check
npm run typecheck

# Lint
npm run lint
```

---

## CI/CD

Pipeline en `.github/workflows/ci.yml`:

1. **Lint** — Spotless (backend) + ESLint (frontend)
2. **Test Backend** — Unit + Integration (Testcontainers)
3. **Test Frontend** — TypeCheck + Vitest + Build
4. **Docker** (solo main) — Build images + Trivy vulnerability scan

---

## Contribución

1. Fork el repositorio
2. Crear branch (`git checkout -b feature/nueva-feature`)
3. Commit (`git commit -m 'Add nueva feature'`)
4. Push (`git push origin feature/nueva-feature`)
5. Abrir Pull Request

### Convenciones

- Ver `AGENTS.md` para reglas de código
- Commits descriptivos en inglés o español consistente
- Branches: `feature/`, `fix/`, `chore/`
- PRs con descripción del cambio

---

## Licencia

MIT License — Ver [LICENSE](LICENSE) para detalles.

---

## Disclaimer

**IMPORTANTE:** Los sorteos del Quini 6 son eventos independientes (i.i.d.). Ningún análisis estadístico pasado predice resultados futuros. Esta plataforma es una herramienta de análisis descriptivo para fines de entretenimiento únicamente. La probabilidad de cualquier combinación siempre es **1/9,366,819**.
