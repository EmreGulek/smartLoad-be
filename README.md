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
- PostgreSQL 16 driver + Spring Data JPA (Hibernate)
- Apache POI 5 (Excel — Phase 1)
- OpenPDF (PDF — Phase 5; LGPL safe per ADR-0016)
- Lombok + MapStruct
- JJWT (JWT auth — Phase 6)

## Phase 0 Scope

- Application boots successfully.
- `GET /api/health` returns 200.
- Security permits all endpoints (locked down in Phase 6).

## Configuration

Secrets are **not** committed. Copy the template and fill locally:

```bash
cp .env.example .env
# Edit .env — set SECURITY_JWT_SECRET_KEY (openssl rand -base64 32) and DB password
```

`src/main/resources/application.properties` loads optional `.env` via `spring.config.import`.

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_*` | PostgreSQL (local or Neon) |
| `SECURITY_JWT_SECRET_KEY` | JWT signing (Base64, required for login) |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | SMTP (optional locally) |
| `CORS_ALLOWED_ORIGINS` | Frontend URLs (comma-separated) |

Production (Render): set the same keys in the service **Environment** tab — do not commit `.env`.

See **[DEPLOY.md](./DEPLOY.md)** for Render + Docker (`Dockerfile` in repo root).

## Frontend

The React frontend lives in `../smartLoad-frontend/`.
Vite proxies `/api/*` to this backend (port 8080).
