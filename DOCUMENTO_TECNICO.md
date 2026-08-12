# Quini6 Analytics — Documento Técnico

**Autor:** Estudiante de Ingeniería de Software  
**Proyecto:** Plataforma de análisis estadístico del Quini 6  
**Stack:** Java 21 + Spring Boot 3.3.2 / React 18 + TypeScript / PostgreSQL / Docker

---

## 1. Introducción y Contexto

Quini6 Analytics es una plataforma full-stack que realiza análisis descriptivo sobre datos históricos del Quini 6 (lotería argentina). El proyecto nació como ejercicio práctico de arquitectura de software, buscando aplicar patrones reales de la industria en un caso de uso concreto.

**Restricción clave:** Los sorteos son eventos independientes (i.i.d.). Ningún análisis predice resultados futuros. Esta restricción no es solo un disclaimer — permea toda la arquitectura: el generador de combinaciones es explícitamente de entretenimiento, y cada endpoint y componente incluye un disclaimer visible.

---

## 2. Arquitectura

### 2.1 Visión General

La arquitectura sigue un enfoque **monolítico modular** con separación por capas:

```
┌─────────────────────────────────────────────────────┐
│                   PRESENTATION                       │
│          React 18 + TypeScript (SPA)                 │
│              Puerto 3000 (Nginx)                     │
├─────────────────────────────────────────────────────┤
│                    API LAYER                         │
│         Spring Boot 3.3.2 (Java 21)                 │
│              Puerto 8082                             │
├──────────┬──────────────┬──────────────┬────────────┤
│ Postgres │ Elasticsearch│   RabbitMQ   │ Prometheus │
│  :5434   │    :9200     │   :5672      │   :9090    │
└──────────┴──────────────┴──────────────┴────────────┘
```

**¿Por qué monolito modular y no microservicios?** Para un proyecto de esta escala, un monolito bien estructurado es más simple de desarrollar, desplegar y mantener. Los microservicios agregan complejidad de red, distribución y observabilidad que no se justifica aquí. Sin embargo, la separación por paquetes (`controller/`, `service/`, `domain/`) permite migrar a microservicios si crece la demanda.

### 2.2 Capa de Presentación (Frontend)

**Tecnologías:** React 18, TypeScript, Vite, React Router 6, Axios, Recharts.

**Decisiones de diseño:**

- **TypeScript estricto** — La tipificación estática catching errores en compile-time es crítico en apps con múltiples endpoints. Interfaces generadas desde el backend garantizan contrato API.
- **Componentes funcionales + Hooks** — Más testables y composables que class components. Custom hooks encapsulan lógica reutilizable.
- **Vite sobre Webpack** — HMR instantáneo, build más rápido por ESM nativo. Para un SPA como este, la diferencia es significativa en productividad.
- **Nginx en producción** — SPA fallback, proxy reverso para `/api/`, gzip, cache de assets estáticos. Un solo contenedor ligero.

**Rutas principales:**

| Ruta | Componente | Función |
|------|-----------|---------|
| `/` | Dashboard | Frecuencias, chi-cuadrado, top numbers |
| `/frecuencia` | Heatmap | Grid coloreado de frecuencias |
| `/generador` | GeneradorCombinaciones | Aleatorio/ponderado (entret.) |
| `/sorteos` | BuscadorSorteos | Búsqueda por número/fecha |
| `/monitoring` | Monitoring | Health, DB, ES, RabbitMQ |

### 2.3 Capa de Negocio (Backend)

**Patrón utilizado:** Capas convencionales con Separación de Responsabilidades.

```
Controller → Service → Repository → Database
    ↓           ↓           ↓
   DTO      Business     JPA/Hibernate
            Logic         (ORM)
```

**Controllers (6):**
- `SorteoController` — CRUD de sorteos
- `EstadisticaController` — Frecuencia y chi-cuadrado
- `GeneradorController` — Generación de combinaciones
- `IngestaController` — Ingesta de datos (ADMIN)
- `AuthController` — JWT authentication
- `MonitoringController` — Health checks y métricas

**Servicios principales:**

- **`EstadisticaService`** — Frecuencias por modalidad, test chi-cuadrado. Implementa cálculos estadísticos reales (distribución uniforme esperada, estadístico de prueba, valor p).
- **`GeneradorCombinacionesService`** — Dos modos: aleatorio puro (Math.random) y ponderado por frecuencia. El后者 tiene un sesgo — por eso el disclaimer explícito.
- **`IngestaService`** — Orquesta scraping + indexado ES + publicación de eventos. Contador Micrometer para métricas.
- **`Quini6ApiClient`** + **`WebScrapingService`** — Fallback chain: API externa → scraping con Jsoup.

**Patrones aplicados:**

| Patrón | Implementación | Beneficio |
|--------|---------------|-----------|
| Repository | Spring Data JPA | Abstracción de persistencia |
| DTO | Records Java | Inmutabilidad, sin getters/setters boilerplate |
| Circuit Breaker | Resilience4j | Graceful degradation si falla API externa |
| Retry | Resilience4j | Reintentos con backoff exponencial |
| Publisher/Listener | RabbitMQ | Desacoplamiento de ingestion events |
| AOP Audit | `@Audit` annotation | Trail de cambios sin invadir lógica |
| JWT Filter | OncePerRequestFilter | Autenticación stateless |

### 2.4 Capa de Datos

**PostgreSQL** — Base de datos transaccional principal.

```sql
-- Schema simplificado
usuarios (UUID PK, username, password_hash, role)
sorteos (UUID PK, numero_sorteo UNIQUE, fecha, pozo_monto)
resultados (UUID PK, sorteo_id FK, modalidad, numero_1..6)
numeros_sorteados (BIGSERIAL PK, resultado_id FK, numero, posicion)
audit_log (BIGSERIAL PK, entidad, accion, actor, timestamp)
```

**Decisiones:**
- **UUID como PK** — Evita auto-increment expuesto, mejor para distribución, merge de datos.
- **Tabla `numeros_sorteados`** — Tabla aplanada para analytics. Normalización vs. performance: para queries de frecuencia, tener cada número en su fila permite `GROUP BY numero` directo.
- **Flyway** — Migraciones versionadas como código. Nunca se modifican migraciones ya ejecutadas.

**Elasticsearch** — Índice `quini6-sorteos` para búsquedas full-text y analytics rápidos.

**¿Por qué dos stores?** PostgreSQL para transaccionesACID, ES para búsquedas complejas y agregaciones. Patrón CQRS ligero.

**RabbitMQ** — Cola `sorteos.ingestion` para desacoplar la ingesta del indexado ES. Si ES falla, el evento queda en cola para reintentar.

---

## 3. Seguridad

### Autenticación JWT

```
Client → POST /api/auth/login (username, password)
Server → Valida BCERT, genera JWT (HS256, 24h expiry)
Client → Header: Authorization: Bearer <token>
Server → JwtAuthenticationFilter → SecurityContext
```

- **Filtro JWT** — `OncePerRequestFilter`, extrae token del header, valida firma y expiración.
- **BCrypt** — Password hashing con salt. Nunca passwords en texto plano.
- **Roles** — `ADMIN` para ingesta, público para consultas.

### Otras medidas
- CORS configurado para desarrollo
- Rate limiting implícito via Resilience4j
- Input validation con Jakarta Validation
- SQL injection prevención via JPA (parameterized queries)
- XSS prevención via React (escaping automático)

---

## 4. Infraestructura y DevOps

### Docker Multi-Stage Builds

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS build
COPY . /app && ./mvnw package

# Production stage
FROM eclipse-temurin:21-jre
COPY --from=build /app/target/*.jar app.jar
```

**Beneficio:** Imagen final ~200MB (solo JRE + JAR), sin herramientas de build.

### Observabilidad (3 pilares)

| Pilar | Herramienta | Métricas |
|-------|------------|----------|
| Métricas | Prometheus + Micrometer | Request rate, latency, JVM, HikariCP |
| Logs | ELK Stack (ES + Kibana) | Structured logs, search, dashboards |
| Tracing | (futuro) OpenTelemetry | Distributed tracing |

### CI/CD (GitHub Actions)

```
Push → Lint → Test Backend → Test Frontend → Docker Build → Trivy Scan → Deploy
```

- **Spotless** — Formato de código automático (Palantir Java Format)
- **Testcontainers** — Tests de integración con contenedores reales (PG, ES, RabbitMQ)
- **Trivy** — Escaneo de vulnerabilidades en imágenes Docker (falla en CRITICAL/HIGH)

---

## 5. Decisiones de Diseño y Trade-offs

### ¿Por qué no usar un ORM más moderno como JOOQ o Exposed?
JPA/Hibernate es el estándar en ecosistemas Spring. Tiene más comunidad, documentación y soporte. Para este proyecto, la complejidad de JOOQ no se justifica.

### ¿Redis en lugar de PostgreSQL?
Redis es excelente para caching y sesiones, pero no para datos persistentes con relaciones complejas. Podría usarse como cache de segundo nivel en el futuro.

### ¿GraphQL en lugar de REST?
REST es más simple, mejor entendido, y suficiente para este caso de uso. GraphQL aporta cuando tienes múltiples clientes con necesidades de datos muy diferentes.

### Monolito vs Microservicios
Como se mencionó, el monolito es apropiado para la escala actual. La separación por paquetes permite extracción futura si es necesario.

---

## 6. Testing

### Estrategia de Testing (Testing Pyramid)

```
        ╱  E2E  ╲         ← (futuro: Cypress/Playwright)
       ╱─────────╲
      ╱ Integration╲       ← Testcontainers (PG, ES, RabbitMQ)
     ╱───────────────╲
    ╱   Unit Tests     ╲   ← JUnit 5 + Mockito
   ╱─────────────────────╲
```

- **Unit Tests** — Servicios aislados con mocks de repositories. Cobertura mínima 75%.
- **Integration Tests** — Controllers con Testcontainers. Verifican endpoint + DB real.
- **Frontend Tests** — Vitest + Testing Library para componentes.

### Test Naming Convention
```java
@Test
void calculateChiSquared_ValidDistribution_ReturnsExpectedStatistic() { ... }
```
Formato: `metodo_Scenario_ExpectedResult`

---

## 7. Lecciones Aprendidas

1. **Type safety importa** — TypeScript evitó muchos bugs en el frontend que habrían sido runtime errors en JavaScript puro.
2. **El disclaimer no es opcional** — En apps de apuestas/lotería, el disclaimer legal es crítico. Debe ser visible en cada vista.
3. **Docker Compose es poderoso** — Levantar 8 servicios con un comando es invaluable para desarrollo y demos.
4. **Los tests de integración valen el tiempo** — Testcontainers descubrieron problemas con JPA lazy loading que los unit tests no atrapaban.
5. **Monitoring desde el día 1** — Prometheus + Grafana detectaron un memory leak en HikariCP que habría pasado desapercibido en producción.

---

## 8. Conclusión

Este proyecto demuestra la aplicación de patrones de arquitectura de software en un caso real: capas bien definidas, separación de responsabilidades, observabilidad completa, y un pipeline de CI/CD que garantiza calidad. El enfoque en monolito modular permite escalar gradualmente sin la complejidad prematura de microservicios.

Los conceptos aplicados — JWT, circuit breakers, CQRS ligero, event-driven architecture, infrastructure as code — son directamente transferibles a cualquier proyecto empresarial.
