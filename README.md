# Warsaw Beauty Salon Explorer

A full-stack directory of beauty salons in Warsaw — built as a take-home project for the SumUp Warsaw Accelerator program.

408 salons sourced from Google Places API, searchable by district, service category, and rating.

**Live stack:** Spring Boot 3.5 · Next.js 15 · PostgreSQL 16 · Redis · Docker Compose

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.5, Spring Security (JWT), MapStruct |
| Database | PostgreSQL 16, Flyway migrations |
| Cache | Redis 7 (salon details, photo refs, district list) |
| Frontend | Next.js 15 App Router, TypeScript, Tailwind CSS v4, Framer Motion |
| Infrastructure | Docker Compose (4 services) |
| API Docs | Swagger UI — `/swagger-ui.html` |

---

## Quick Start (Docker)

```bash
# 1. Copy env template and fill in your Google Maps API key
cp .env.example .env

# 2. Start all 4 services (postgres, redis, backend, frontend)
docker compose up --build

# 3. Open the app
open http://localhost:3000
```

> First run applies all Flyway migrations automatically. The database starts empty — run ingestion to populate salons (see below).

---

## Local Development

**Prerequisites:** Java 21, Node 22, Docker (for postgres + redis)

```bash
# Start postgres + redis only
docker compose up -d postgres redis

# Backend (PowerShell)
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
mvn spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

App runs at `http://localhost:3000`, API at `http://localhost:8080`.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Browser / Client                  │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP
┌──────────────────────▼──────────────────────────────┐
│              Next.js 15 (port 3000)                 │
│  Server Components · /api/* → proxy → :8080         │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP
┌──────────────────────▼──────────────────────────────┐
│           Spring Boot 3.5 (port 8080)               │
│  JWT Auth · Rate Limiting · Redis Cache             │
│  Google Places photo proxy                         │
└──────┬───────────────┬────────────────┬─────────────┘
       │               │                │
  ┌────▼────┐    ┌──────▼─────┐   ┌─────▼──────────┐
  │ Postgres│    │   Redis    │   │ Google Places  │
  │   :5432 │    │   :6379    │   │    API v1      │
  └─────────┘    └────────────┘   └────────────────┘
```

---

## API Endpoints

Full interactive docs at `http://localhost:8080/swagger-ui.html`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/salons` | List salons (filter by district, service, category, rating) |
| GET | `/api/v1/salons/{id}` | Salon detail |
| GET | `/api/v1/salons/{id}/photos` | All photo URLs for a salon |
| GET | `/api/v1/districts` | All Warsaw districts |
| GET | `/api/v1/services` | All service categories |
| GET | `/api/v1/photos?ref=...` | Google Places photo proxy |
| POST | `/api/v1/auth/login` | JWT login |
| POST | `/api/v1/admin/ingest` | Run Google Places ingestion pipeline |
| POST | `/api/v1/admin/enrich` | Enrich salons with descriptions + opening hours |

Default admin credentials: `admin@salonexplorer.dev` / `admin123`

---

## Data Ingestion

Salons are fetched from the Google Places (New) API using 35 search queries covering hair, nails, spa, brows, lash, waxing, and more — both in Polish and English.

```bash
# Trigger ingestion via API (requires admin JWT)
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Authorization: Bearer <token>"

# Then enrich with descriptions + opening hours
curl -X POST http://localhost:8080/api/v1/admin/enrich \
  -H "Authorization: Bearer <token>"
```

---

## Environment Variables

See `.env.example` for the full list. Required:

| Variable | Description |
|----------|-------------|
| `GOOGLE_MAPS_API_KEY` | Google Places API (New) key |
| `POSTGRES_PASSWORD` | PostgreSQL admin password |
| `APP_DB_PASSWORD` | Application DB user password |
| `JWT_SECRET` | Base64-encoded JWT signing secret |

---

## Project Structure

```
.
├── backend/                 # Spring Boot application
│   ├── src/main/java/...
│   │   ├── controller/      # REST endpoints
│   │   ├── service/         # Business logic + photo proxy
│   │   ├── ingestion/       # Google Places client + pipeline
│   │   ├── security/        # JWT filter, rate limiter
│   │   └── mapper/          # MapStruct DTO mappers
│   └── src/main/resources/
│       └── db/migration/    # Flyway V1–V9
├── frontend/                # Next.js application
│   └── src/
│       ├── app/             # Pages (listing + detail)
│       └── components/      # UI components
├── docker/postgres/init/    # DB init scripts
├── docker-compose.yml
└── .env.example
```

---

## Author

**Mustafa Kaan Yavuz** — built for the SumUp Warsaw Accelerator take-home task, 2026.
