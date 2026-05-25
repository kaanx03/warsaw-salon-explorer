# Warsaw Beauty Salon Explorer

A full-stack directory of beauty salons in Warsaw — built as a take-home project for the SumUp Warsaw Accelerator program.

**408 salons · 18 districts · 6 800+ price entries · Edition 01 · 2026**

---

## Screenshots

![Home page — salon listing with district and category filters](docs/screenshots/home.png)

![Salon detail page](docs/screenshots/salon-detail.png)

![Price list](docs/screenshots/prices.png)

---

## Running the Project

**Prerequisite:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

```bash
# 1. Clone
git clone https://github.com/mustafakaanyavuz/warsaw-salon-explorer.git
cd warsaw-salon-explorer

# 2. Create env file
cp .env.example .env
```

Open `.env` and fill in:

| Variable | Description |
|----------|-------------|
| `POSTGRES_PASSWORD` | Any strong password |
| `APP_DB_PASSWORD` | Any strong password |
| `JWT_SECRET` | Base64 string — generate with `openssl rand -base64 32` |
| `GOOGLE_MAPS_API_KEY` | Google Maps API key (needed for salon photos) |

```bash
# 3. Start everything
docker compose up --build
```

First run takes ~5 minutes (pulls Java + Node images, compiles). Subsequent starts are instant.

- **App:** http://localhost:3000
- **API docs:** http://localhost:8080/swagger-ui.html

The database comes fully pre-seeded — 408 salons and 6 800+ price entries load automatically via Flyway migrations. No extra steps needed.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.5, Spring Security (JWT), MapStruct |
| Database | PostgreSQL 16, Flyway migrations (V1–V10) |
| Cache | Redis 7 |
| Frontend | Next.js 15 App Router, TypeScript, Tailwind CSS v4, Framer Motion |
| Infrastructure | Docker Compose (4 services) |
| API Docs | Swagger UI |

---

## Architecture

```
Browser
  │
  ▼
Next.js 15 :3000   (Server Components, /api/* proxied to backend)
  │
  ▼
Spring Boot :8080  (JWT auth, rate limiting, Redis cache, Google Places photo proxy)
  │         │         │
Postgres  Redis  Google Places API
```

---

## How the Data Was Built

### 1. Salon data — Google Places API

408 salons were fetched using the Google Places (New) API with 35 search queries covering hair, nails, spa, brows, lash, waxing, and more in both Polish and English. A second enrichment pass added descriptions and opening hours.

This is reproducible via the admin API (requires JWT login as `admin@salonexplorer.dev` / `admin123`):

```bash
# Login and get token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@salonexplorer.dev","password":"admin123"}'

# Run ingestion
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Authorization: Bearer <token>"

# Enrich with descriptions + opening hours
curl -X POST http://localhost:8080/api/v1/admin/enrich \
  -H "Authorization: Bearer <token>"
```

### 2. Price lists — web scraping

Google Places API does not provide price lists. A Python scraper (`scripts/scrape_prices.py`) visited each salon's website and extracted prices using two strategies:

- **Booksy pages** — scraped service cards directly from the rendered HTML (name, price, duration)
- **Regular websites** — discovered price pages (`/cennik`, `/cennik-zabiegow`, `/oferta`, etc.) via homepage navigation, then parsed HTML tables, definition lists, and Elementor column layouts

Results (216 salons, 6 812 services) were imported into the `service_offerings` table and committed as `V10__seed_service_offerings.sql` — so they load automatically for anyone running the project.

To re-scrape or update prices:

```bash
pip install requests beautifulsoup4 psycopg2-binary python-dotenv

python scripts/scrape_prices.py          # scrape all salons → scraped_prices.json
python scripts/import_prices.py --dry-run  # preview
python scripts/import_prices.py            # write to DB
```

---

## API Endpoints

Full docs at `http://localhost:8080/swagger-ui.html`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/salons` | List salons (filter by district, service, category, rating) |
| GET | `/api/v1/salons/{id}` | Salon detail with services and price list |
| GET | `/api/v1/salons/{id}/photos` | Photo URLs for a salon |
| GET | `/api/v1/districts` | All 18 Warsaw districts |
| GET | `/api/v1/services` | All service categories |
| GET | `/api/v1/photos?ref=...` | Google Places photo proxy |
| POST | `/api/v1/auth/login` | Get JWT token |
| POST | `/api/v1/admin/ingest` | Fetch salons from Google Places |
| POST | `/api/v1/admin/enrich` | Enrich with descriptions + hours |

---

## Local Development (without Docker)

**Prerequisites:** Java 21, Node 22, Docker (for Postgres + Redis only)

```bash
# Start only the databases
docker compose up -d postgres redis

# Backend
cd backend
$env:SPRING_PROFILES_ACTIVE="local"   # PowerShell
mvn spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install && npm run dev
```

---

## Project Structure

```
.
├── backend/src/main/
│   ├── java/com/kaandev/salonexplorer/
│   │   ├── controller/      # REST endpoints
│   │   ├── service/         # Business logic, photo proxy
│   │   ├── ingestion/       # Google Places client + pipeline
│   │   ├── security/        # JWT, rate limiting
│   │   └── mapper/          # MapStruct DTO mappers
│   └── resources/db/migration/  # Flyway V1–V10
├── frontend/src/
│   ├── app/                 # Pages (listing + salon detail)
│   └── components/          # UI components
├── scripts/
│   ├── scrape_prices.py     # Price list scraper
│   └── import_prices.py     # DB importer
├── docker/postgres/init/    # DB user + seed SQL
├── docker-compose.yml
└── .env.example
```

---

## Author

**Mustafa Kaan Yavuz** — built for the SumUp Warsaw Accelerator take-home task, 2026.
