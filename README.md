# SmartLoad — Backend

Spring Boot 3.2 + Java 21 + PostgreSQL 16.

## Quick Start

1. Start PostgreSQL (from project root: `docker compose up -d postgres`)
2. Build and run:

```bash
./mvnw spring-boot:run
```

(Or use `mvn spring-boot:run` if you have Maven installed system-wide.)

3. Verify: http://localhost:8080/api/health → `{"status":"OK","phase":"0",...}`

## Stack

- Spring Boot 3.2 + Java 21
- Spring Web, Data JPA, Security, Validation
- PostgreSQL 16 driver + Flyway migration
- Apache POI 5 (Excel — Phase 1)
- OpenPDF (PDF — Phase 5; LGPL safe per ADR-0016)
- Lombok + MapStruct
- JJWT (JWT auth — Phase 6)

## Phase 0 Scope

- Application boots successfully.
- `GET /api/health` returns 200.
- Flyway runs `V1__init_schema.sql` (bootstrap table only).
- Security permits all endpoints (locked down in Phase 6).

## Configuration

`src/main/resources/application.yml` — defaults expect:
- PostgreSQL on `localhost:5432`, db `smartload`, user `smartload` / `smartload_dev`
- Override via env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, etc.

## Frontend

The React frontend lives in `../smartLoad-frontend/`.
Vite proxies `/api/*` to this backend (port 8080).
