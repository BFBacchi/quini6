# AGENTS.md — Coding Conventions for Quini6 Analytics

## CRITICAL RULE

**Todos los archivos, código, documentación y UI deben dejar claro que:**
- Los sorteos del Quini 6 son **eventos independientes (i.i.d.)**
- **Ningún análisis histórico predice el próximo resultado**
- El generador de combinaciones es de **ENTRETENIMIENTO** solamente
- La probabilidad de cualquier combinación siempre es **1/9,366,819**

Esto **no es negociable** en ningún módulo.

---

## Backend (Java 21 + Spring Boot 3.x)

### Estructura
- Paquete raíz: `com.quini6.analytics`
- Separación por capas: `controller/`, `service/`, `domain/`, `config/`, `client/`, `messaging/`
- DTOs en `dto/` — records de Java preferidos sobre clases
- Endpoints REST: `/api/{recurso}`

### Código
- Usar Palantir Java Format (Spotless)
- Preferir records sobre clases POJO para DTOs
- Null checks con Optional, nunca con null directo
- Strings con Text Blocks para queries JPQL
- Exception handling global con `@ControllerAdvice`
- Nunca exponer entidades JPA directamente en endpoints REST

### Testing
- Unit tests: `src/test/java/.../unit/`
- Integration tests: `src/test/java/.../integration/` con Testcontainers
- Nombrar tests: `metodo_Scenario_ExpectedResult`
- Cobertura mínima: 75% en servicios de negocio

### Base de datos
- Migraciones en `db/migration/V{version}__{description}.sql`
- Nunca modificar migraciones ya ejecutadas
- Flyway gestiona el schema automáticamente

---

## Frontend (React 18 + TypeScript + Vite)

### Estructura
- Componentes en `src/components/{Modulo}/`
- Servicios API en `src/services/api.ts`
- Tipos generados desde OpenAPI

### Código
- TypeScript estricto (`strict: true`)
- Funciones component sobre class components
- Hooks personalizados en `src/hooks/`
- Estilos inline o CSS modules (no Tailwind en este proyecto)

### API calls
- Todos los tipos generados desde el contrato OpenAPI del backend
- Siempre manejar errores con try/catch
- Loading states en cada petición

---

## Docker

- Multi-stage builds: `dev` para desarrollo, `production` para deploy
- Imágenes finales sin herramientas de build
- Healthchecks en todos los servicios
- Variables de entorno en `.env` (nunca hardcodeadas)

---

## Git

- Commits descriptivos en inglés o español consistente
- Branches: `feature/`, `fix/`, `chore/`
- PRs con descripción del cambio y contexto

---

## Disclaimer Pattern

Cada endpoint que retorne datos estadísticos debe incluir un campo `disclaimer` en la respuesta.
Cada componente del frontend que muestre datos debe incluir un disclaimer visible.

Ejemplo de disclaimer:
```
"Análisis descriptivo de datos históricos. Los sorteos son eventos independientes (i.i.d.).
Ningún resultado pasado predice resultados futuros."
```
