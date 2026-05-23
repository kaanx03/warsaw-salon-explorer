# Phase 5: Docker Compose ile Tam Stack Entegrasyonu

> **Hedef:** PostgreSQL + Redis + Backend (Spring Boot) + Frontend (Next.js) — tüm stack'i tek komutla (`docker compose up`) ayağa kaldırılabilir hale getirmek. Production-grade Dockerfile'lar, multi-stage build'ler, environment yönetimi, network isolation, health check'ler ve startup orchestration.

---

## 📋 İçindekiler

1. [Faz Özeti](#1-faz-özeti)
2. [Mimari Diagram](#2-mimari-diagram)
3. [Backend Dockerfile](#3-backend-dockerfile)
4. [Frontend Dockerfile](#4-frontend-dockerfile)
5. [Tam docker-compose.yml](#5-tam-docker-composeyml)
6. [Environment Yönetimi](#6-environment-yönetimi)
7. [Network & Service Discovery](#7-network--service-discovery)
8. [Health Check & Startup Order](#8-health-check--startup-order)
9. [Volume & Persistence](#9-volume--persistence)
10. [Backend Konfigürasyon Güncellemeleri](#10-backend-konfigürasyon-güncellemeleri)
11. [Frontend Konfigürasyon Güncellemeleri](#11-frontend-konfigürasyon-güncellemeleri)
12. [Build & Çalıştırma](#12-build--çalıştırma)
13. [Reverse Proxy (Opsiyonel)](#13-reverse-proxy-opsiyonel)
14. [Image Boyut Optimizasyonu](#14-image-boyut-optimizasyonu)
15. [Security Hardening](#15-security-hardening)
16. [Doğrulama & Smoke Test](#16-doğrulama--smoke-test)
17. [Yaygın Sorunlar](#17-yaygın-sorunlar)
18. [Mülakat Soruları](#18-mülakat-soruları)
19. [Definition of Done](#19-definition-of-done)

---

## 1. Faz Özeti

**Bu fazın sonunda elimizde olacaklar:**

- `Dockerfile` backend için (multi-stage, layer cache optimize)
- `Dockerfile` frontend için (multi-stage, standalone output)
- `docker-compose.yml` tüm stack için
- `.env` yönetimi, secret'lar dışarıda
- Internal network isolation (sadece reverse proxy/frontend external)
- Health check'ler — `depends_on` ile doğru startup order
- Persistent volume'lar (DB + Redis veri kaybı yok)
- Non-root user ile container'lar
- Production-ready image boyutları (Backend ~250MB, Frontend ~180MB)
- Tek komutla `docker compose up -d` → tüm stack çalışır

**Bu fazda YAPMAYACAĞIZ:**

- Production deployment (cloud provider, Kubernetes — out of scope)
- CI/CD pipeline (Faz 6'da)

---

## 2. Mimari Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Host                              │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   salon-network (bridge, internal)                   │  │
│  │                                                       │  │
│  │  ┌──────────────┐  ┌──────────────┐ ┌─────────────┐ │  │
│  │  │   postgres   │  │    redis     │ │   backend   │ │  │
│  │  │  :5432       │  │   :6379      │ │   :8080     │ │  │
│  │  │  (internal)  │  │  (internal)  │ │  (internal) │ │  │
│  │  └──────────────┘  └──────────────┘ └─────────────┘ │  │
│  │         ▲                  ▲              ▲          │  │
│  │         │                  │              │          │  │
│  │         └──────────────────┴──────────────┘          │  │
│  │                            │                          │  │
│  │                     ┌──────┴────────┐                 │  │
│  │                     │   frontend    │                 │  │
│  │                     │   :3000       │                 │  │
│  │                     └───────────────┘                 │  │
│  └─────────────────────────│──────────────────────────────┘  │
│                            │                                  │
│  Host ports:              :3000 (frontend)                    │
│                           :8080 (backend, sadece dev)         │
│                           :5432 (postgres, sadece dev)        │
│                           :6379 (redis, sadece dev)           │
└────────────────────────────│──────────────────────────────────┘
                             │
                             ▼
                       Browser :3000
```

**Önemli kararlar:**

- `postgres`, `redis`, `backend` **internal** — production'da host port expose edilmez
- `frontend` external — kullanıcı buraya bağlanır
- Backend → `postgres:5432`, `redis:6379` service name ile internal DNS
- Frontend → `backend:8080` internal, browser → frontend external

---

## 3. Backend Dockerfile

### 3.1 `backend/Dockerfile`

```dockerfile
# ──────────────────────────────────────────────────────────
# Stage 1: Build (Maven + JDK 21)
# ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Önce pom.xml — dependency layer cache için
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Dependency'leri indir (cache'lenecek katman)
RUN ./mvnw dependency:go-offline -B

# Şimdi kaynak kodu kopyala
COPY src ./src

# Build (test'leri skip — CI'da ayrı koşar)
RUN ./mvnw package -DskipTests -B

# JAR'ı extract et (layered jar — daha iyi cache)
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ──────────────────────────────────────────────────────────
# Stage 2: Runtime (sadece JRE)
# ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Security: non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# CA certificates (TLS için lazım olabilir)
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Europe/Warsaw /etc/localtime && \
    echo "Europe/Warsaw" > /etc/timezone

WORKDIR /app

# Layered jar — her layer ayrı COPY, dependency cache hit oranı yüksek
ARG EXTRACTED=/build/target/extracted
COPY --from=builder ${EXTRACTED}/dependencies/         ./
COPY --from=builder ${EXTRACTED}/spring-boot-loader/   ./
COPY --from=builder ${EXTRACTED}/snapshot-dependencies/ ./
COPY --from=builder ${EXTRACTED}/application/          ./

# Ownership
RUN chown -R spring:spring /app

USER spring

# JVM tuning — container-aware
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Health check için curl kullan
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

### 3.2 `backend/.dockerignore`

```
target/
.idea/
.vscode/
*.iml
.git/
.gitignore
README.md
HELP.md
.env
.env.local
.mvn/wrapper/maven-wrapper.jar
logs/
*.log
```

> **`.dockerignore` neden kritik?** Docker build context'i `.gitignore` dinlemez, her şeyi context'e gönderir → büyük target/ klasörü gönderilirse build yavaşlar, secret sızabilir.

### 3.3 Multi-stage avantajları (özet)

| Stage | Boyut | Ne içeriyor |
|-------|-------|-------------|
| `builder` | ~700MB | JDK + Maven + kaynak + .m2 cache |
| Final | ~250MB | JRE + sadece JAR layer'ları |

**Layered JAR** sayesinde:
- Dependencies değişmediği sürece Docker o layer'ı cache'ler
- Sadece kod değiştiğinde `application/` layer'ı rebuild olur
- Push/pull süresi büyük oranda kısalır

---

## 4. Frontend Dockerfile

### 4.1 Next.js standalone output etkinleştir

`frontend/next.config.ts`:

```typescript
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  output: 'standalone',         // ← Docker için kritik
  reactStrictMode: true,
  poweredByHeader: false,
  // ... önceki ayarlar
};

export default nextConfig;
```

**Standalone output ne yapar?** Sadece çalışmak için gerekli dosyaları (`node_modules` dahil minimum) `.next/standalone/` altına çıkarır. Final image boyutunu 5x küçültür.

### 4.2 `frontend/Dockerfile`

```dockerfile
# ──────────────────────────────────────────────────────────
# Stage 1: Dependencies (sadece npm install)
# ──────────────────────────────────────────────────────────
FROM node:20-alpine AS deps

# libc6 uyumluluk
RUN apk add --no-cache libc6-compat

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --include=dev

# ──────────────────────────────────────────────────────────
# Stage 2: Build
# ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build-time env (NEXT_PUBLIC_* değişkenleri bundle'a gömülür)
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}

ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production

RUN npm run build

# ──────────────────────────────────────────────────────────
# Stage 3: Runtime
# ──────────────────────────────────────────────────────────
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Non-root user (Next.js best practice)
RUN addgroup -S -g 1001 nodejs && \
    adduser -S -u 1001 -G nodejs nextjs

# Static dosyalar (CDN'e taşınabilir production'da)
COPY --from=builder /app/public ./public

# Standalone output (sadece runtime için lazım olanlar)
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/api/health || exit 1

CMD ["node", "server.js"]
```

### 4.3 `frontend/.dockerignore`

```
node_modules
.next
.git
.gitignore
README.md
Dockerfile
.dockerignore
.env.local
.env.development
.env.production
npm-debug.log
.DS_Store
coverage
.vscode
.idea
```

### 4.4 Health endpoint frontend'de

`frontend/src/app/api/health/route.ts`:

```typescript
import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json({ status: 'ok', timestamp: new Date().toISOString() });
}
```

---

## 5. Tam docker-compose.yml

Proje root'unda — tüm servisleri orchestrate eden tek dosya.

### 5.1 `docker-compose.yml`

```yaml
name: salon-explorer

services:
  # ──────────────────────────────────────────────────
  # Database
  # ──────────────────────────────────────────────────
  postgres:
    image: postgres:16-alpine
    container_name: salon-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      APP_DB_USER: ${APP_DB_USER}
      APP_DB_PASSWORD: ${APP_DB_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=C --lc-ctype=C"
    # ⚠️ Production'da host port'u expose ETME (sadece internal network)
    # Development için açık:
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    networks:
      - salon-network

  # ──────────────────────────────────────────────────
  # Cache & rate limit storage
  # ──────────────────────────────────────────────────
  redis:
    image: redis:7.4-alpine
    container_name: salon-redis
    restart: unless-stopped
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
      --appendonly yes
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - salon-network

  # ──────────────────────────────────────────────────
  # Backend
  # ──────────────────────────────────────────────────
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: salon-backend
    restart: unless-stopped
    environment:
      # Profile
      SPRING_PROFILES_ACTIVE: docker

      # Datasource — service name ile DNS (postgres → container)
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${APP_DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${APP_DB_PASSWORD}

      # Flyway (admin user)
      SPRING_FLYWAY_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_FLYWAY_USER: ${POSTGRES_USER}
      SPRING_FLYWAY_PASSWORD: ${POSTGRES_PASSWORD}

      # Redis
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}

      # JWT
      SECURITY_JWT_SECRET: ${JWT_SECRET}
      SECURITY_JWT_EXPIRATION_MS: ${JWT_EXPIRATION_MS:-3600000}

      # Google Places (sadece ingestion için lazım)
      GOOGLE_PLACES_API_KEY: ${GOOGLE_MAPS_API_KEY}

      # CORS
      CORS_ALLOWED_ORIGINS: http://localhost:3000,http://frontend:3000

      # JVM
      JAVA_OPTS: "-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 90s   # Spring Boot startup için bekle
    networks:
      - salon-network

  # ──────────────────────────────────────────────────
  # Frontend
  # ──────────────────────────────────────────────────
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        # Build-time: NEXT_PUBLIC_* değişkenleri bundle'a gömülür
        # Browser'dan erişileceği URL — host port
        NEXT_PUBLIC_API_URL: ${PUBLIC_API_URL:-http://localhost:8080/api/v1}
    container_name: salon-frontend
    restart: unless-stopped
    environment:
      # Runtime — Server Component'lerin backend'e ulaşması için
      # Container-to-container, service name ile
      INTERNAL_API_URL: http://backend:8080/api/v1
      NEXT_PUBLIC_API_URL: ${PUBLIC_API_URL:-http://localhost:8080/api/v1}
      SESSION_COOKIE_NAME: ${SESSION_COOKIE_NAME:-salon_session}
      NODE_ENV: production
    ports:
      - "${FRONTEND_PORT:-3000}:3000"
    depends_on:
      backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s
    networks:
      - salon-network

# ──────────────────────────────────────────────────
# Volumes & networks
# ──────────────────────────────────────────────────
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local

networks:
  salon-network:
    driver: bridge
    name: salon-network
```

### 5.2 İsteğe bağlı: ingestion için ayrı compose dosyası

`docker-compose.ingest.yml`:

```yaml
services:
  ingestion:
    extends:
      file: docker-compose.yml
      service: backend
    container_name: salon-ingestion
    environment:
      SPRING_PROFILES_ACTIVE: docker,ingest
    restart: "no"           # Tek seferlik
    depends_on:
      postgres:
        condition: service_healthy
      backend:
        condition: service_healthy
```

Çalıştır:

```bash
docker compose -f docker-compose.yml -f docker-compose.ingest.yml run --rm ingestion
```

Bu Spring Boot'u `ingest` profile ile ayağa kaldırır, ingestion bitince exit eder, container silinir.

---

## 6. Environment Yönetimi

### 6.1 `.env.example` — proje root

```bash
# ═══════════════════════════════════════════════════
# Database
# ═══════════════════════════════════════════════════
POSTGRES_DB=salon_explorer
POSTGRES_USER=salon_admin
POSTGRES_PASSWORD=__CHANGE_ME_STRONG_PASSWORD__
POSTGRES_PORT=5432

APP_DB_USER=salon_app
APP_DB_PASSWORD=__CHANGE_ME_APP_PASSWORD__

# ═══════════════════════════════════════════════════
# Redis
# ═══════════════════════════════════════════════════
REDIS_PASSWORD=__CHANGE_ME_REDIS_PASSWORD__
REDIS_PORT=6379

# ═══════════════════════════════════════════════════
# Backend
# ═══════════════════════════════════════════════════
BACKEND_PORT=8080
JWT_SECRET=__GENERATE_WITH_openssl_rand_base64_32__
JWT_EXPIRATION_MS=3600000

# ═══════════════════════════════════════════════════
# Google Places (ingestion için)
# ═══════════════════════════════════════════════════
GOOGLE_MAPS_API_KEY=__YOUR_API_KEY__

# ═══════════════════════════════════════════════════
# Frontend
# ═══════════════════════════════════════════════════
FRONTEND_PORT=3000
# Browser bu URL üzerinden backend'e gider
PUBLIC_API_URL=http://localhost:8080/api/v1
SESSION_COOKIE_NAME=salon_session
```

### 6.2 `.env` — gerçek değerler (gitignore'da)

```bash
cp .env.example .env

# Strong password üret
openssl rand -base64 24    # POSTGRES_PASSWORD için
openssl rand -base64 24    # APP_DB_PASSWORD için
openssl rand -base64 24    # REDIS_PASSWORD için
openssl rand -base64 32    # JWT_SECRET için

# .env'i düzenle, üretilen değerleri yerleştir
```

### 6.3 `.gitignore` (proje root)

```
.env
.env.local
.env.*.local
*.log
target/
node_modules/
.next/
.idea/
.vscode/
.DS_Store
```

### 6.4 Production'da secret yönetimi

Production'da `.env` dosyası **kullanmazsın**. Bunun yerine:

| Platform | Yöntem |
|----------|--------|
| AWS ECS | Secrets Manager + Task Definition |
| AWS EKS | External Secrets Operator + AWS Secrets |
| GCP Cloud Run | Secret Manager + revision env |
| Kubernetes | Sealed Secrets / External Secrets / Vault |
| Docker Swarm | `docker secret` |

> **Mülakatta:** "Development'ta `.env`, production'da Secrets Manager + env var injection. Kod hiç bir zaman secret görmez."

---

## 7. Network & Service Discovery

### 7.1 Service name = DNS hostname

Docker Compose otomatik olarak service isimlerini DNS hostname yapar. Aynı network'teki container'lar birbirine şöyle erişir:

| Source | Target | URL |
|--------|--------|-----|
| Backend | PostgreSQL | `jdbc:postgresql://postgres:5432/...` |
| Backend | Redis | `redis://redis:6379` |
| Frontend (server) | Backend | `http://backend:8080/api/v1` |
| Frontend (browser) | Backend | `http://localhost:8080/api/v1` |

**Önemli ayrım:** Frontend container içinde **iki farklı URL** lazım:

- **`INTERNAL_API_URL`** (Server Components, Route Handlers) → `http://backend:8080/...`
- **`NEXT_PUBLIC_API_URL`** (browser bundle) → `http://localhost:8080/...`

Browser, Docker network'ünün dışında — `backend` isim çözümlemesi yapamaz. O yüzden public URL host port'unu gösterir.

### 7.2 Network isolation

`salon-network` bridge driver — varsayılan davranış:
- Aynı network'tekiler birbirine ulaşır
- Network dışındakiler (host hariç) ulaşamaz
- DNS hostname'leri internal

Production'da daha katı:

```yaml
networks:
  frontend-network:
    driver: bridge
  backend-network:
    driver: bridge
    internal: true        # ← Internet erişimi yok

services:
  frontend:
    networks: [frontend-network]
  backend:
    networks: [frontend-network, backend-network]
  postgres:
    networks: [backend-network]   # ← Sadece backend ulaşır
```

---

## 8. Health Check & Startup Order

### 8.1 `depends_on` ile condition

Eski Docker Compose (`v2.x`):
- `depends_on: [postgres]` → sadece **container start** edildiyse devam et
- Container start ≠ uygulama hazır

Yeni:
- `condition: service_healthy` → **healthcheck pass** olunca devam et

Bu sayede backend, postgres `pg_isready` dönmeden başlamaz.

### 8.2 Startup sırası

```
postgres start → healthy ✓
redis start    → healthy ✓
     ↓
backend start  → Flyway migration → Spring context → /actuator/health → healthy ✓
     ↓
frontend start → /api/health → healthy ✓
```

### 8.3 Health check best practice'leri

| Service | Test | start_period | Interval |
|---------|------|--------------|----------|
| postgres | `pg_isready` | 30s | 10s |
| redis | `redis-cli ping` | 10s | 10s |
| backend | `curl /actuator/health` | 90s | 30s |
| frontend | `wget /api/health` | 30s | 30s |

> **`start_period` kritik:** Backend ayağa kalkıp Spring context oluşturup migration koşması ~60-90s sürer. Bu sürede healthcheck fail dönse de `restart` tetiklenmez.

---

## 9. Volume & Persistence

### 9.1 Named volumes

```yaml
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
```

**Named volume avantajı:**
- Docker yönetir, host path'i gizler
- `docker volume ls` ile listelenir
- `docker compose down` → silmez
- `docker compose down -v` → siler (dikkat!)

### 9.2 Bind mount (sadece config için)

```yaml
volumes:
  - ./docker/postgres/init:/docker-entrypoint-initdb.d:ro
```

`:ro` = read-only. Container içinden değiştirilemez.

### 9.3 Backup stratejisi (production notu)

Production'da volume'lar yetmez:

```bash
# PostgreSQL backup (cron'lanır)
docker exec salon-postgres pg_dump -U salon_admin salon_explorer | gzip > backup_$(date +%F).sql.gz

# Restore
gunzip -c backup_2026-05-23.sql.gz | docker exec -i salon-postgres psql -U salon_admin salon_explorer
```

Üretimde S3'e push edilir, retention policy uygulanır, point-in-time recovery için WAL archive kullanılır.

---

## 10. Backend Konfigürasyon Güncellemeleri

### 10.1 `application-docker.yml` — yeni profile

`backend/src/main/resources/application-docker.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: docker

  # Datasource — env variable'lardan (compose injection)
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      pool-name: SalonHikariPool
      maximum-pool-size: 20      # Production-grade
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  flyway:
    url: ${SPRING_FLYWAY_URL}
    user: ${SPRING_FLYWAY_USER}
    password: ${SPRING_FLYWAY_PASSWORD}

  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST}
      port: ${SPRING_DATA_REDIS_PORT}
      password: ${SPRING_DATA_REDIS_PASSWORD}
      timeout: 2000ms

  jpa:
    properties:
      hibernate:
        format_sql: false       # Production'da kapalı
    show-sql: false

security:
  jwt:
    secret: ${SECURITY_JWT_SECRET}
    expiration-ms: ${SECURITY_JWT_EXPIRATION_MS:3600000}

google:
  places:
    api-key: ${GOOGLE_PLACES_API_KEY:}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: never        # Production: detay sızdırma
      probes:
        enabled: true            # Kubernetes /readyz, /livez

server:
  port: 8080
  shutdown: graceful
  compression:
    enabled: true
    mime-types: application/json,text/html,text/css

spring.lifecycle.timeout-per-shutdown-phase: 30s

logging:
  level:
    root: INFO
    com.kaandev.salonexplorer: INFO
    org.hibernate.SQL: WARN
```

### 10.2 Spring profiles aktivasyonu

Compose'da:
```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
```

Bu hem `application.yml` (base) hem `application-docker.yml` (override) yükler.

### 10.3 Graceful shutdown

```yaml
server:
  shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 30s
```

`docker compose down` veya Kubernetes SIGTERM gönderdiğinde:
- 30 saniye boyunca in-flight request'leri tamamla
- Yeni request kabul etme
- Sonra kapan

---

## 11. Frontend Konfigürasyon Güncellemeleri

### 11.1 Build-time vs runtime env

Next.js'te env iki tip:

| Tip | Ne zaman gerek | Compose'da |
|-----|----------------|-----------|
| `NEXT_PUBLIC_*` | Build sırasında bundle'a gömülür | `build.args` |
| Diğerleri | Runtime'da `process.env` | `environment` |

### 11.2 Server Component fetch'i container içinde

```typescript
// src/lib/api/client.ts (zaten yazılmıştı)
const INTERNAL_URL = process.env.INTERNAL_API_URL ?? 'http://localhost:8080/api/v1';
```

`INTERNAL_API_URL=http://backend:8080/api/v1` Docker DNS ile çözülür.

### 11.3 Browser fetch'i

`apiClient` `NEXT_PUBLIC_API_URL`'i kullanır → build sırasında bundle'a `http://localhost:8080/api/v1` gömülür → browser direkt host port'una gider.

### 11.4 next/image için host whitelist

`next.config.ts`:

```typescript
images: {
  remotePatterns: [
    {
      protocol: 'http',
      hostname: 'localhost',
      port: '8080',
      pathname: '/api/v1/photos/**',
    },
    // Production:
    {
      protocol: 'https',
      hostname: 'api.salonexplorer.com',
      pathname: '/v1/photos/**',
    },
  ],
},
```

---

## 12. Build & Çalıştırma

### 12.1 İlk kurulum

```bash
# .env'i hazırla
cp .env.example .env
# .env'i düzenle, passwordleri üret

# Strong password generation:
echo "POSTGRES_PASSWORD=$(openssl rand -base64 24)" >> .env
echo "APP_DB_PASSWORD=$(openssl rand -base64 24)"   >> .env
echo "REDIS_PASSWORD=$(openssl rand -base64 24)"    >> .env
echo "JWT_SECRET=$(openssl rand -base64 32)"        >> .env
```

### 12.2 Build & start

```bash
# Build (ilk çalıştırmada veya kod değişince)
docker compose build

# Tüm stack'i ayağa kaldır
docker compose up -d

# Sırayla healthcheck'lerin geçmesini bekle
docker compose ps
# Beklenen STATUS:
# postgres   Up (healthy)
# redis      Up (healthy)
# backend    Up (healthy)
# frontend   Up (healthy)
```

### 12.3 Logları izle

```bash
# Tüm servislerin logu
docker compose logs -f

# Sadece backend
docker compose logs -f backend

# Son 50 satır
docker compose logs --tail=50 backend
```

### 12.4 Veri toplama

İlk kurulumdan sonra ingestion'ı tetikle:

```bash
# Opsiyon A: ayrı compose dosyası ile (önerilen)
docker compose -f docker-compose.yml -f docker-compose.ingest.yml run --rm ingestion

# Opsiyon B: çalışan backend container'ında profile change ile yeniden başlat
# (production'da bu yapılmaz, sadece dev için)
```

Beklenen log:
```
==> Starting ingestion. Queries: [beauty salon Warsaw, ...]
...
==> Ingestion complete: 100+ inserted
```

### 12.5 Smoke test

```bash
# Backend
curl http://localhost:8080/actuator/health
# {"status":"UP"}

curl http://localhost:8080/api/v1/salons | jq '.page'
# {"number":0,"size":20,"totalElements":127,"totalPages":7}

# Frontend
curl http://localhost:3000/api/health
# {"status":"ok",...}

# Browser
open http://localhost:3000
```

### 12.6 Stop & cleanup

```bash
# Durdur (volume'lar kalır)
docker compose down

# Durdur + volume'ları sil (DİKKAT: tüm veri gider)
docker compose down -v

# Image'ları da sil
docker compose down -v --rmi local
```

### 12.7 Tek service rebuild

```bash
# Sadece backend
docker compose build backend
docker compose up -d backend

# Frontend kod değişti, rebuild
docker compose build frontend
docker compose up -d frontend
```

---

## 13. Reverse Proxy (Opsiyonel)

Production'da tek bir domain üzerinden hem frontend hem backend serve etmek istersin. Bunu Caddy veya Nginx ile yaparsın.

### 13.1 Caddy ile basit setup

`Caddyfile`:

```
salonexplorer.local {
    handle /api/* {
        reverse_proxy backend:8080
    }

    handle /actuator/health {
        reverse_proxy backend:8080
    }

    handle {
        reverse_proxy frontend:3000
    }
}
```

### 13.2 `docker-compose.override.yml` (opsiyonel reverse proxy)

```yaml
services:
  caddy:
    image: caddy:2-alpine
    container_name: salon-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    depends_on:
      - frontend
      - backend
    networks:
      - salon-network

  # Production'da bunları expose etme
  backend:
    ports: []   # override main file
  frontend:
    ports: []

volumes:
  caddy_data:
  caddy_config:
```

> **Bu task için zorunlu değil.** README'de "production'da Caddy/Nginx ile reverse proxy" diye not yeterli. Implement edersen bonus.

---

## 14. Image Boyut Optimizasyonu

### 14.1 Boyut karşılaştırması

| Image | Naive | Optimized |
|-------|-------|-----------|
| Backend | `eclipse-temurin:21-jdk` 480MB | `21-jre-alpine` + layered ~250MB |
| Frontend | `node:20` + full + node_modules ~1.2GB | `node:20-alpine` + standalone ~180MB |
| PostgreSQL | `postgres:16` 380MB | `postgres:16-alpine` 230MB |
| Redis | `redis:7.4` 110MB | `redis:7.4-alpine` 35MB |

### 14.2 Optimizasyon teknikleri

**Backend:**
- ✅ `-jre-alpine` (JDK değil JRE)
- ✅ Layered JAR
- ✅ Multi-stage build
- ⚪ İleride: GraalVM native image (50MB, anında startup, ama daha kompleks)

**Frontend:**
- ✅ `output: 'standalone'`
- ✅ `node:20-alpine`
- ✅ Multi-stage (deps → build → runner)
- ✅ Production node_modules sadece runner'a kopyalanır
- ⚪ İleride: Distroless image

### 14.3 Image boyutunu kontrol et

```bash
docker images | grep salon

# REPOSITORY                    SIZE
# salon-explorer-frontend       180MB
# salon-explorer-backend        252MB
```

---

## 15. Security Hardening

### 15.1 Yapılanlar (yukarıdaki kodda)

- ✅ Non-root user (Backend `spring`, Frontend `nextjs`)
- ✅ `:ro` read-only volume mount'lar config için
- ✅ Secret'lar env variable, image'a gömülmedi
- ✅ `.dockerignore` ile context küçük
- ✅ Alpine base (daha az saldırı yüzeyi)
- ✅ Redis password ile korunmuş
- ✅ App DB user limited privilege (Faz 1)
- ✅ `poweredByHeader: false` Next.js'te

### 15.2 Production için ekstra adımlar

```yaml
# docker-compose.prod.yml (override)
services:
  backend:
    read_only: true              # FS read-only
    tmpfs:
      - /tmp                      # JVM tmp için yazılabilir
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 15.3 Image vulnerability scan

```bash
# Trivy ile tarama
trivy image salon-explorer-backend:latest
trivy image salon-explorer-frontend:latest

# Veya Docker Scout
docker scout cves salon-explorer-backend:latest
```

CI'da otomatize edilir — high/critical CVE varsa build fail olur.

### 15.4 Production network policy

```yaml
networks:
  internal:
    internal: true    # internet erişimi YOK
  external:
    driver: bridge

services:
  postgres:
    networks: [internal]    # Sadece internal
  redis:
    networks: [internal]
  backend:
    networks: [internal, external]
  frontend:
    networks: [external]    # Sadece external
```

Bu konfigte:
- DB ve Redis internet erişemez
- Backend her ikisinde de
- Frontend sadece internet'te

---

## 16. Doğrulama & Smoke Test

### 16.1 End-to-end checklist

```bash
# 1. Clean start
docker compose down -v
docker compose build

# 2. Start everything
docker compose up -d

# 3. Wait for all healthy
watch docker compose ps
# Tüm servisler "Up (healthy)" olana kadar bekle (~90 saniye)

# 4. Ingestion (ilk kez)
docker compose -f docker-compose.yml -f docker-compose.ingest.yml run --rm ingestion

# 5. Verify DB
docker exec salon-postgres psql -U salon_admin -d salon_explorer -c "SELECT COUNT(*) FROM salons;"
# 100+

# 6. Backend health
curl -s http://localhost:8080/actuator/health | jq
# {"status":"UP",...}

# 7. Backend API
curl -s http://localhost:8080/api/v1/salons?size=5 | jq '.content | length'
# 5

# 8. Frontend health
curl -s http://localhost:3000/api/health | jq
# {"status":"ok",...}

# 9. Browser
open http://localhost:3000
# Listing görünmeli, filtreler çalışmalı

# 10. Container'ları restart et, veri kalır mı?
docker compose restart
sleep 60
curl -s http://localhost:8080/api/v1/salons?size=5 | jq '.content | length'
# 5 (veri korunmalı)
```

### 16.2 Resource kullanımı

```bash
docker stats --no-stream
```

Beklenen:
```
CONTAINER         CPU %   MEM USAGE      MEM %
salon-postgres    0.5%    50MB / 8GB     0.6%
salon-redis       0.1%    8MB / 8GB      0.1%
salon-backend     2.0%    400MB / 8GB    5%
salon-frontend    1.0%    120MB / 8GB    1.5%
```

### 16.3 Volume persistence testi

```bash
# Data var mı kontrol
docker exec salon-postgres psql -U salon_admin -d salon_explorer -c "SELECT COUNT(*) FROM salons;"

# Container'ları sil (volume kalsın)
docker compose down

# Tekrar aç
docker compose up -d

# Data hâlâ var mı?
sleep 60
docker exec salon-postgres psql -U salon_admin -d salon_explorer -c "SELECT COUNT(*) FROM salons;"
# Aynı sayı olmalı ✓
```

### 16.4 Network izolasyon testi

```bash
# Host'tan postgres'e direkt erişim
docker run --rm --network salon-network postgres:16-alpine \
  psql -h postgres -U salon_admin -d salon_explorer -c "SELECT 1"
# Çalışır ✓

# Network dışından (random container)
docker run --rm postgres:16-alpine \
  psql -h postgres -U salon_admin -d salon_explorer -c "SELECT 1"
# Çalışmaz: "could not translate host name 'postgres'" ✓
```

---

## 17. Yaygın Sorunlar

| Problem | Sebep | Çözüm |
|---------|-------|-------|
| `port already allocated` | Lokalde aynı port'ta servis var | `.env`'de port'u değiştir (`5433:5432`) |
| Backend `Connection refused` postgres'e | depends_on healthy bekleme yok | `condition: service_healthy` kullan |
| Frontend backend'i göremiyor | Browser `backend` DNS'i çözemez | `NEXT_PUBLIC_API_URL` host port'u kullan |
| Migration `permission denied` | Flyway app user kullanıyor | Flyway için admin user kullan (zaten yapıldı) |
| `password authentication failed` | `.env` değişti, container eski | `docker compose down -v && up` (DİKKAT data gider) |
| Image build çok yavaş | Cache miss | `.dockerignore` ekle, Dockerfile sırasını optimize et |
| `OutOfMemoryError` Java'da | JVM heap çok küçük | `JAVA_OPTS="-XX:MaxRAMPercentage=75"` |
| Container restart loop | Healthcheck fail | `start_period`'u uzat, logları incele |
| Frontend `next.config.ts` değişti, etkili olmuyor | Image cache | `docker compose build --no-cache frontend` |
| Disk dolu | Eski image/volume birikti | `docker system prune -af --volumes` |

---

## 18. Mülakat Soruları

**S: Multi-stage build ne kazandırıyor?**
Final image'da build tool'lar (Maven, JDK, node_modules dev deps) yok. Backend ~700MB → 250MB, Frontend ~1.2GB → 180MB. Push/pull hızlı, attack surface küçük.

**S: Layered JAR avantajı?**
Spring Boot uber-jar tek dosya — herhangi bir değişiklikte tüm JAR yeniden push. Layered JAR dependency'leri ayrı katmana koyar, sadece kodun değiştiği uygulama katmanı invalidate olur. CI/CD'de bandwidth ve süre kazancı.

**S: `depends_on` ile `condition: service_healthy` farkı?**
Plain `depends_on` sadece container başlatılma sırasını belirler — uygulamanın hazır olmasını beklemez. `service_healthy` ile healthcheck pass olana kadar bekler. Spring Boot 60-90s startup'ta backend bunu beklemezse frontend "ECONNREFUSED" döner.

**S: Browser ile container içi farklı URL'ler — neden?**
Browser Docker network'ünde değil, hostta. `backend` DNS adı browser'da bilinmiyor. Server Component (container içinde) `http://backend:8080`, Client Component bundle'ı (browser'da çalışacak) `http://localhost:8080`. İki ayrı env variable.

**S: `NEXT_PUBLIC_*` build-time, neden?**
Bundle'a gömülüyor. Browser'a inen JS bundle'da literal string olarak yazılır → runtime'da env değiştiremezsin. Production'da bunu değiştirmek için image rebuild lazım. Trade-off: runtime config istiyorsan custom `/api/config` endpoint yapılır.

**S: Postgres ve Redis production'da host expose ediyor musun?**
Hayır. Sadece internal network'te. Dış erişim gerekirse SSH tunnel veya bastion host üzerinden. Compose'da `ports: []` ile override veya prod compose'a sadece backend/frontend port'larını koy.

**S: Secrets management — `.env` production'da yeterli mi?**
Hayır. `.env` dosyası: 1) Source control'a kaçma riski, 2) Sürüm yönetimi yok, 3) Erişim kontrolü dosya sistemi seviyesinde. Production'da AWS Secrets Manager + IAM role, GCP Secret Manager, HashiCorp Vault — runtime'da inject edilir, audit log var, rotation otomatik.

**S: Non-root user neden önemli?**
Container escape açığı çıkarsa, root yetkisi host'a sızabilir. Non-root user filesystem write yetkisi sınırlı, `cap_drop: ALL` ile bütün capability'leri düşürürsen daha sıkı. Kubernetes Pod Security Standards `runAsNonRoot: true` zorunlu kılıyor.

**S: Volume'lar persistent ama production data nasıl korunur?**
Volume yetmez. Production'da: 1) Managed DB (AWS RDS, Cloud SQL) — automated backup + PITR, 2) WAL archiving + S3, 3) Cron'lanan `pg_dump`'lar, 4) Multi-AZ replica, 5) Disaster recovery plan. Docker volume sadece dev/test için.

**S: Image vulnerability scan'i ne zaman koşar?**
CI pipeline'da, push'tan önce. Trivy/Snyk/Docker Scout. High/Critical CVE varsa pipeline fail. Production'da düzenli olarak periyodik scan + base image güncelleme cron'u.

**S: Network isolation ile ne kazanılıyor?**
Defense in depth. Backend'de bir RCE açığı çıkarsa attacker DB'ye direkt internet'ten ulaşamasın (postgres internal'da). Internal network internet'e çıkamasın → exfiltration zor. Compromise blast radius'ünü daraltıyor.

**S: Compose vs Kubernetes ne zaman?**
Compose: tek host, dev/test/POC, küçük ekip, basit. K8s: multi-host, autoscaling, rolling deploy, declarative state, service mesh, namespace isolation. Migrate yolu: Compose → Compose Spec'i Kustomize/Helm'e çevir → K8s manifests. Hetzner CCM, k3s ile küçük ölçek K8s de bir orta yol.

**S: Graceful shutdown nasıl çalışıyor?**
`docker compose down` SIGTERM gönderir. Spring Boot `server.shutdown=graceful` ile yeni connection kabul etmez, mevcut request'leri 30s'e kadar tamamlar. JVM hook'la JDBC pool ve Redis pool kapanır. 30s'de tamamlanmazsa SIGKILL — connection drop olur.

**S: Healthcheck endpoint'i nereye koyarsın?**
Spring Boot `/actuator/health` — liveness + readiness probe. K8s'te ikisini ayır:
- `/actuator/health/liveness` → process alive mı? Restart trigger.
- `/actuator/health/readiness` → trafik kabul edebilir mi? Service'e ekleme kararı.
Frontend `/api/health` — basit, dependency yok. Frontend Next.js process up mı bakar.

**S: `compose up -d` ile `compose run` farkı?**
`up -d` long-running daemon servisleri. `run` tek seferlik komutlar (ingestion, migration). `run --rm` exit edince container'ı silsin. Ingestion için `run` doğru — bitince çıkar, kaynak tutmaz.

**S: Tag stratejisi production'da?**
- `latest` kullanma (deploy'da hangi versiyon çalışıyor belli olmaz)
- Semantic version: `v1.2.3`
- Git SHA: `salon-backend:abc1234`
- Multi-tag: `salon-backend:v1.2.3 + salon-backend:latest + salon-backend:abc1234`
- Production'da SHA + version, rollback için her ikisi de kullanışlı

**S: Multi-arch image (ARM + AMD) nasıl?**
M1/M2 Mac developer'ları, AWS Graviton'da çalıştırma için ARM gerekli. `docker buildx build --platform linux/amd64,linux/arm64 --push`. CI'da çoğunlukla GitHub Actions matrix ile. Manifest list ile tek tag, çoklu mimari.

**S: Compose'dan Kubernetes'e ne kadar yakın?**
Compose'daki çoğu kavram K8s'te var: service → Service, container → Pod, volume → PersistentVolumeClaim, network → NetworkPolicy. Ama K8s declarative, Compose imperative. Migrate için Kompose tool var ama production K8s manifest'leri elle yazılır çoğunlukla.

---

## 19. Definition of Done

- [ ] `backend/Dockerfile` multi-stage, non-root, healthcheck'li
- [ ] `frontend/Dockerfile` multi-stage, standalone output, non-root
- [ ] `.dockerignore` her iki projede de var
- [ ] `docker-compose.yml` root'ta, 4 servis tanımlı
- [ ] `.env.example` tüm değişkenleri içeriyor, açıklamalı
- [ ] `.env` gitignore'da, gerçek secret'larla dolu
- [ ] `docker-compose.ingest.yml` opsiyonel ingestion için var
- [ ] `application-docker.yml` env variable'lardan okuyor
- [ ] Next.js `output: 'standalone'` aktif
- [ ] Frontend için `INTERNAL_API_URL` ve `NEXT_PUBLIC_API_URL` ayrımı net
- [ ] `docker compose build` hatasız tamamlanıyor
- [ ] `docker compose up -d` ile 4 servis de healthy oluyor (~90s içinde)
- [ ] `depends_on: condition: service_healthy` doğru çalışıyor (backend postgres'i bekliyor)
- [ ] Backend `/actuator/health` 200 dönüyor
- [ ] Frontend `/api/health` 200 dönüyor
- [ ] Frontend http://localhost:3000'de açılıyor, listing görünüyor
- [ ] `docker compose -f docker-compose.yml -f docker-compose.ingest.yml run --rm ingestion` başarıyla çalışıyor
- [ ] DB'de 100+ salon var
- [ ] `docker compose restart` sonrası veri korunuyor (volume çalışıyor)
- [ ] `docker compose down && up` sonrası veri korunuyor
- [ ] Image boyutları: backend < 300MB, frontend < 200MB
- [ ] Backend container içinde `id` komutu `spring` user gösteriyor (non-root ✓)
- [ ] Frontend container içinde `id` komutu `nextjs` user gösteriyor
- [ ] Hiçbir secret image'a gömülmedi (`docker history` ile kontrol)
- [ ] Resource limit'leri makul (memory, CPU)
- [ ] README'de "How to run" tek komut: `docker compose up -d`

---

## ➡️ Sonraki Adım

**Faz 6: README, GitHub & CI/CD**

Faz 6'da:
- Profesyonel README.md (mimari, screenshot, setup, API docs)
- Mimari ve ER diagram'ları (Mermaid)
- "What I'd improve with more time" — recruiter'ı etkileyen bölüm
- GitHub Actions CI pipeline (lint + test + build + scan)
- Branch protection, PR template
- Commit history temizleme (mülakatçı bakar)
- LICENSE, CONTRIBUTING.md
- Demo GIF/video çekimi
- Push & paylaşılabilir link
