# Phase 1: Database Setup (PostgreSQL + Docker)

> **Hedef:** Production-grade bir PostgreSQL instance'ı Docker üzerinde ayağa kaldırmak, schema'yı Flyway migration'ları ile versiyonlamak ve hem development hem CI/CD için tekrar üretilebilir bir veritabanı altyapısı kurmak.

---

## 📋 İçindekiler

1. [Faz Özeti](#1-faz-özeti)
2. [Ön Gereksinimler](#2-ön-gereksinimler)
3. [Klasör Yapısı](#3-klasör-yapısı)
4. [Schema Tasarımı](#4-schema-tasarımı)
5. [Docker Compose Setup](#5-docker-compose-setup)
6. [Flyway Migration Stratejisi](#6-flyway-migration-stratejisi)
7. [Spring Boot ile Bağlantı](#7-spring-boot-ile-bağlantı)
8. [Doğrulama & Test](#8-doğrulama--test)
9. [Yaygın Sorunlar](#9-yaygın-sorunlar)
10. [Mülakat Soruları](#10-mülakat-soruları)
11. [Definition of Done](#11-definition-of-done)

---

## 1. Faz Özeti

Bu fazda elle SQL çalıştırmayacağız. Her şey **kodla yönetilen**, **versiyonlanmış** ve **container içinde** çalışacak.

**Bu fazın sonunda elimizde olacaklar:**

- Docker üzerinde çalışan bir PostgreSQL 16 instance'ı
- Persistent volume (container silinse bile veri kaybolmasın)
- Bir admin user, bir application user (least privilege)
- `flyway_schema_history` tablosu + ilk migration uygulanmış
- `salons`, `districts`, `services`, `salon_services`, `users`, `audit_log` tabloları
- Index'ler, foreign key'ler, constraint'ler
- Health check'leri çalışan container

**Bu fazda YAPMAYACAĞIZ:**

- Veri toplama (Faz 2)
- REST API yazımı (Faz 3)
- Frontend (Faz 4)

---

## 2. Ön Gereksinimler

### Kurulu olması gerekenler

| Tool | Versiyon | Kontrol komutu |
|------|----------|----------------|
| Docker | 24.x+ | `docker --version` |
| Docker Compose | v2.x+ | `docker compose version` |
| Java | 21 LTS | `java --version` |
| Maven | 3.9+ | `mvn --version` |
| psql (opsiyonel) | 16.x | `psql --version` |

### Port kontrolü

PostgreSQL `5432` portunu kullanır. Eğer lokalde başka bir PostgreSQL koşuyorsa çakışır:

```bash
# macOS / Linux
lsof -i :5432

# Windows (PowerShell)
netstat -ano | findstr :5432
```

Çakışma varsa `docker-compose.yml`'da `5433:5432` gibi map'leyeceğiz.

---

## 3. Klasör Yapısı

Faz 1 sonunda proje root'u şöyle görünecek:

```
warsaw-salon-explorer/
├── .gitignore
├── .env.example
├── .env                           # ⚠️ gitignore'da
├── docker-compose.yml
├── README.md
└── backend/
    ├── pom.xml
    ├── Dockerfile                 # Faz 5'te
    └── src/
        └── main/
            ├── java/
            │   └── com/kaandev/salonexplorer/
            │       └── SalonExplorerApplication.java
            └── resources/
                ├── application.yml
                ├── application-local.yml
                └── db/
                    └── migration/
                        ├── V1__create_core_tables.sql
                        ├── V2__create_user_and_audit_tables.sql
                        ├── V3__create_indexes.sql
                        └── V4__seed_districts.sql
```

---

## 4. Schema Tasarımı

### 4.1 ERD (Entity-Relationship Diagram)

```
┌─────────────────┐         ┌─────────────────┐
│    districts    │         │     services    │
│─────────────────│         │─────────────────│
│ id (PK)         │         │ id (PK)         │
│ name UNIQUE     │         │ name UNIQUE     │
│ slug UNIQUE     │         │ category        │
│ created_at      │         │ created_at      │
└────────┬────────┘         └────────┬────────┘
         │                            │
         │ 1:N                        │ N:M
         │                            │
         ▼                            │
┌─────────────────────────────┐      │
│           salons             │      │
│──────────────────────────────│      │
│ id (PK)                      │      │
│ google_place_id UNIQUE       │      │
│ name                         │      │
│ address                      │      │
│ district_id (FK)             │      │
│ phone                        │      │
│ website                      │      │
│ latitude, longitude          │◄─────┘
│ rating, review_count         │      │
│ price_level                  │      │
│ photo_url                    │      │
│ is_active                    │      │
│ created_at, updated_at       │      │
└────────┬─────────────────────┘      │
         │                            │
         │ N:M (via salon_services)   │
         └────────────────────────────┘

┌─────────────────┐         ┌─────────────────┐
│      users      │         │   audit_log     │
│─────────────────│         │─────────────────│
│ id (PK)         │         │ id (PK)         │
│ email UNIQUE    │◄────────┤ user_id (FK)    │
│ password_hash   │  1:N    │ entity_type     │
│ role            │         │ entity_id       │
│ is_enabled      │         │ action          │
│ created_at      │         │ changes (JSONB) │
└─────────────────┘         │ created_at      │
                            └─────────────────┘
```

### 4.2 Tablo açıklamaları

#### `salons` — ana tablo

Tüm salon verisini tutar. `google_place_id` UNIQUE olduğu için **deduplikasyon otomatik** — aynı salonu iki kez yazamazsın.

| Kolon | Tip | Not |
|-------|-----|-----|
| `id` | `BIGSERIAL PRIMARY KEY` | Internal ID |
| `google_place_id` | `VARCHAR(255) UNIQUE NOT NULL` | Google'ın benzersiz ID'si |
| `name` | `VARCHAR(255) NOT NULL` | Salon adı |
| `address` | `TEXT NOT NULL` | Tam adres |
| `district_id` | `BIGINT REFERENCES districts(id)` | District (nullable, çünkü resolve edemediğimiz olabilir) |
| `phone` | `VARCHAR(50)` | E.164 formatında |
| `website` | `VARCHAR(500)` | URL |
| `latitude` | `DECIMAL(10,7)` | Harita için |
| `longitude` | `DECIMAL(10,7)` | Harita için |
| `rating` | `DECIMAL(2,1)` | 0.0 - 5.0 |
| `review_count` | `INTEGER DEFAULT 0` | |
| `price_level` | `SMALLINT` | 1-4 arası, Google standardı |
| `photo_url` | `TEXT` | Google photo reference URL |
| `is_active` | `BOOLEAN DEFAULT TRUE` | Soft delete için |
| `created_at` | `TIMESTAMPTZ DEFAULT NOW()` | |
| `updated_at` | `TIMESTAMPTZ DEFAULT NOW()` | Trigger ile auto-update |

#### `districts` — Varşova'nın 18 dzielnica'sı

Hardcoded seed data ile gelir (V4 migration).

#### `services` — sunulan hizmetler

`haircut`, `manicure`, `pedicure`, `coloring` gibi kategoriler.

#### `salon_services` — many-to-many bridge

#### `users` — admin auth için

#### `audit_log` — kim neyi değiştirdi

Write işlemleri burada loglanır (compliance + debugging).

---

## 5. Docker Compose Setup

### 5.1 `.env.example` dosyası

Repo'ya bu commit'lenir, gerçek `.env` ise gitignore'da:

```bash
# .env.example
POSTGRES_DB=salon_explorer
POSTGRES_USER=salon_admin
POSTGRES_PASSWORD=change_me_in_production
POSTGRES_PORT=5432

# Application DB user (least privilege)
APP_DB_USER=salon_app
APP_DB_PASSWORD=change_me_app_password

# Spring profile
SPRING_PROFILES_ACTIVE=local
```

### 5.2 `.gitignore`

```
.env
*.log
target/
.idea/
.vscode/
node_modules/
.DS_Store
```

### 5.3 `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: salon-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      # Performance tuning for dev
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=C --lc-ctype=C"
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

volumes:
  postgres_data:
    driver: local

networks:
  salon-network:
    driver: bridge
```

### 5.4 Init script — application user oluştur

`docker/postgres/init/01-create-app-user.sh`:

```bash
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Application user (least privilege)
    CREATE USER ${APP_DB_USER:-salon_app} WITH PASSWORD '${APP_DB_PASSWORD:-change_me_app_password}';

    -- Schema permissions
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${APP_DB_USER:-salon_app};
    GRANT USAGE ON SCHEMA public TO ${APP_DB_USER:-salon_app};

    -- Tüm tablolar için (Flyway'in oluşturacaklarına da)
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${APP_DB_USER:-salon_app};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO ${APP_DB_USER:-salon_app};
EOSQL
```

> **Neden iki user?**
> `admin` user → DDL (CREATE TABLE, ALTER) yapabilir, Flyway bunu kullanır.
> `app` user → sadece DML (SELECT, INSERT, UPDATE, DELETE). Uygulama bunu kullanır.
> Eğer uygulamada SQL injection olursa, attacker `DROP TABLE` yapamaz çünkü yetki yok. **Enterprise security pattern.**

### 5.5 Container'ı başlat

```bash
# .env dosyasını oluştur
cp .env.example .env

# Container'ı ayağa kaldır
docker compose up -d postgres

# Status kontrolü
docker compose ps

# Loglar
docker compose logs -f postgres

# Health check
docker inspect --format='{{.State.Health.Status}}' salon-postgres
# Beklenen: healthy
```

### 5.6 Bağlantı testi

```bash
# Container içinden
docker exec -it salon-postgres psql -U salon_admin -d salon_explorer

# Veya host'tan (psql kuruluysa)
psql -h localhost -p 5432 -U salon_admin -d salon_explorer
```

İlk komutlar:

```sql
\dt              -- tabloları listele (henüz boş)
\du              -- user'ları listele (admin + app görünmeli)
\l               -- database'leri listele
\q               -- çıkış
```

---

## 6. Flyway Migration Stratejisi

### 6.1 Naming convention

Flyway dosyaları şu formatta isimlendirilir:

```
V<VERSION>__<DESCRIPTION>.sql
```

- `V` zorunlu prefix
- `<VERSION>` artan sayı: `1`, `2`, `3`...
- `__` (çift underscore!) ayırıcı
- `<DESCRIPTION>` snake_case açıklama

**Örnekler:**

```
V1__create_core_tables.sql        ✅
V2__create_user_and_audit_tables.sql  ✅
V3__create_indexes.sql            ✅
V4__seed_districts.sql            ✅
V1_create_tables.sql              ❌ (çift underscore eksik)
v1__create.sql                    ❌ (küçük v)
```

### 6.2 Altın kural: migration'lar IMMUTABLE'dır

Bir kez merge ettiğin migration'ı **asla değiştirme**. Düzeltme yapacaksan yeni bir migration yaz:

```
V1__create_salons.sql        # ✅ Production'da
V2__add_email_to_salons.sql  # ✅ Düzeltme: yeni dosya
```

**Asla yapma:**
```
V1__create_salons.sql        # ✅ Production'da
V1__create_salons.sql        # ❌ İçeriği değişti → Flyway checksum hatası
```

### 6.3 `V1__create_core_tables.sql`

```sql
-- ============================================================
-- V1: Core tables (districts, services, salons, junction)
-- ============================================================

-- Auto-update updated_at fonksiyonu (reusable)
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ------------------------------------------------------------
-- districts
-- ------------------------------------------------------------
CREATE TABLE districts (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE districts IS 'Warsaw districts (dzielnice) — 18 official';
COMMENT ON COLUMN districts.slug IS 'URL-safe lowercase identifier, e.g. "mokotow"';

-- ------------------------------------------------------------
-- services
-- ------------------------------------------------------------
CREATE TABLE services (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    category    VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT services_category_check CHECK (
        category IN ('hair', 'nails', 'face', 'body', 'other')
    )
);

-- ------------------------------------------------------------
-- salons
-- ------------------------------------------------------------
CREATE TABLE salons (
    id                BIGSERIAL    PRIMARY KEY,
    google_place_id   VARCHAR(255) NOT NULL UNIQUE,
    name              VARCHAR(255) NOT NULL,
    address           TEXT         NOT NULL,
    district_id       BIGINT       REFERENCES districts(id) ON DELETE SET NULL,
    phone             VARCHAR(50),
    website           VARCHAR(500),
    latitude          DECIMAL(10,7),
    longitude         DECIMAL(10,7),
    rating            DECIMAL(2,1),
    review_count      INTEGER      NOT NULL DEFAULT 0,
    price_level       SMALLINT,
    photo_url         TEXT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT salons_rating_check       CHECK (rating IS NULL OR (rating >= 0.0 AND rating <= 5.0)),
    CONSTRAINT salons_review_count_check CHECK (review_count >= 0),
    CONSTRAINT salons_price_level_check  CHECK (price_level IS NULL OR (price_level BETWEEN 1 AND 4)),
    CONSTRAINT salons_lat_check          CHECK (latitude  IS NULL OR (latitude  BETWEEN -90  AND 90)),
    CONSTRAINT salons_lng_check          CHECK (longitude IS NULL OR (longitude BETWEEN -180 AND 180))
);

COMMENT ON COLUMN salons.google_place_id IS 'Unique identifier from Google Places API — used for deduplication';
COMMENT ON COLUMN salons.price_level     IS 'Google standard: 1=cheap, 2=moderate, 3=expensive, 4=very expensive';
COMMENT ON COLUMN salons.is_active       IS 'Soft delete flag';

-- Auto-update trigger
CREATE TRIGGER salons_set_updated_at
    BEFORE UPDATE ON salons
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

-- ------------------------------------------------------------
-- salon_services (many-to-many)
-- ------------------------------------------------------------
CREATE TABLE salon_services (
    salon_id    BIGINT NOT NULL REFERENCES salons(id)   ON DELETE CASCADE,
    service_id  BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (salon_id, service_id)
);
```

### 6.4 `V2__create_user_and_audit_tables.sql`

```sql
-- ============================================================
-- V2: User authentication and audit logging
-- ============================================================

-- ------------------------------------------------------------
-- users (admin auth)
-- ------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL    PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'ADMIN',
    is_enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'EDITOR', 'VIEWER'))
);

CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

-- ------------------------------------------------------------
-- audit_log (write işlemlerini kim yaptı)
-- ------------------------------------------------------------
CREATE TABLE audit_log (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    BIGINT       NOT NULL,
    action       VARCHAR(20)  NOT NULL,
    changes      JSONB,
    ip_address   VARCHAR(45),
    user_agent   TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT audit_action_check CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
);

COMMENT ON TABLE  audit_log         IS 'Append-only log of all write operations';
COMMENT ON COLUMN audit_log.changes IS 'JSONB diff: {"field": {"old": "...", "new": "..."}}';
```

### 6.5 `V3__create_indexes.sql`

```sql
-- ============================================================
-- V3: Performance indexes
-- ============================================================

-- Salons: en sık filtrelenen kolonlar
CREATE INDEX idx_salons_district_id  ON salons(district_id) WHERE is_active = TRUE;
CREATE INDEX idx_salons_rating       ON salons(rating DESC) WHERE is_active = TRUE;
CREATE INDEX idx_salons_price_level  ON salons(price_level) WHERE is_active = TRUE;
CREATE INDEX idx_salons_is_active    ON salons(is_active);

-- Full-text search üzerine name + address
CREATE INDEX idx_salons_name_trgm
    ON salons USING gin (name gin_trgm_ops);

-- gin_trgm_ops için extension lazım
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Audit log: en sık entity_type + entity_id ile sorgulanır
CREATE INDEX idx_audit_entity      ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_created_at  ON audit_log(created_at DESC);
CREATE INDEX idx_audit_user_id     ON audit_log(user_id);
```

> **`WHERE is_active = TRUE` partial index neden?**
> Sadece aktif salonları indexliyoruz → index daha küçük → sorgu daha hızlı. Soft-delete edilmiş kayıtlar zaten listelenmeyecek, onları indexlemek israf.

### 6.6 `V4__seed_districts.sql`

```sql
-- ============================================================
-- V4: Seed Warsaw districts (18 official dzielnice)
-- ============================================================

INSERT INTO districts (name, slug) VALUES
    ('Bemowo',          'bemowo'),
    ('Białołęka',       'bialoleka'),
    ('Bielany',         'bielany'),
    ('Mokotów',         'mokotow'),
    ('Ochota',          'ochota'),
    ('Praga-Południe',  'praga-poludnie'),
    ('Praga-Północ',    'praga-polnoc'),
    ('Rembertów',       'rembertow'),
    ('Śródmieście',     'srodmiescie'),
    ('Targówek',        'targowek'),
    ('Ursus',           'ursus'),
    ('Ursynów',         'ursynow'),
    ('Wawer',           'wawer'),
    ('Wesoła',          'wesola'),
    ('Wilanów',         'wilanow'),
    ('Włochy',          'wlochy'),
    ('Wola',            'wola'),
    ('Żoliborz',        'zoliborz')
ON CONFLICT (slug) DO NOTHING;

-- Standart hizmet kategorileri
INSERT INTO services (name, category) VALUES
    ('Haircut',          'hair'),
    ('Hair Coloring',    'hair'),
    ('Hair Styling',     'hair'),
    ('Manicure',         'nails'),
    ('Pedicure',         'nails'),
    ('Nail Art',         'nails'),
    ('Facial',           'face'),
    ('Makeup',           'face'),
    ('Eyebrow Shaping',  'face'),
    ('Massage',          'body'),
    ('Waxing',           'body')
ON CONFLICT (name) DO NOTHING;
```

---

## 7. Spring Boot ile Bağlantı

### 7.1 `pom.xml` — gerekli dependency'ler

```xml
<dependencies>
    <!-- Web (Faz 3'te kullanılacak ama şimdi de eklenebilir) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator (health check) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 7.2 `application.yml` — base config

```yaml
spring:
  application:
    name: salon-explorer

  # Flyway resmi olarak admin user kullanacak (DDL yetkisi var)
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    validate-on-migrate: true

  # JPA app user kullanacak (sadece DML)
  jpa:
    hibernate:
      ddl-auto: validate          # ✅ Sadece schema'yı doğrula, asla DDL üretme
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 25
    show-sql: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
  health:
    db:
      enabled: true
```

> **Kritik: `ddl-auto: validate`**
> Hibernate'i `create` veya `update` modunda **asla bırakma**. Schema yönetimi sadece Flyway'in işidir. `validate` modu, entity'lerin tablolarla eşleşmediğini gördüğünde uygulamayı başlatmaz → erken hata yakalanır.

### 7.3 `application-local.yml` — development config

```yaml
spring:
  config:
    activate:
      on-profile: local

  # Flyway → admin user (DDL için)
  flyway:
    url: jdbc:postgresql://localhost:5432/salon_explorer
    user: salon_admin
    password: ${POSTGRES_PASSWORD:change_me_in_production}

  # JPA datasource → app user (least privilege)
  datasource:
    url: jdbc:postgresql://localhost:5432/salon_explorer
    username: salon_app
    password: ${APP_DB_PASSWORD:change_me_app_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      pool-name: SalonHikariPool
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

logging:
  level:
    org.flywaydb: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE   # parametreleri görmek için
```

### 7.4 `SalonExplorerApplication.java`

```java
package com.kaandev.salonexplorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SalonExplorerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SalonExplorerApplication.class, args);
    }
}
```

### 7.5 İlk çalıştırma

```bash
# 1. DB container çalıştığından emin ol
docker compose ps

# 2. Spring Boot'u local profille başlat
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# Beklenen log:
# Flyway Community Edition X.Y.Z by Redgate
# Database: jdbc:postgresql://localhost:5432/salon_explorer (PostgreSQL 16.x)
# Successfully validated 4 migrations
# Migrating schema "public" to version "1 - create core tables"
# Migrating schema "public" to version "2 - create user and audit tables"
# Migrating schema "public" to version "3 - create indexes"
# Migrating schema "public" to version "4 - seed districts"
# Successfully applied 4 migrations to schema "public" in XXX ms
# Started SalonExplorerApplication in X.XXX seconds
```

---

## 8. Doğrulama & Test

### 8.1 Manuel kontrol

```bash
docker exec -it salon-postgres psql -U salon_admin -d salon_explorer
```

```sql
-- Tüm tablolar var mı?
\dt
-- Beklenen: audit_log, districts, flyway_schema_history, salon_services, salons, services, users

-- Migration history
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
-- 4 satır, hepsi success=t

-- District seed çalıştı mı?
SELECT COUNT(*) FROM districts;
-- 18

-- Service seed
SELECT COUNT(*) FROM services;
-- 11

-- Constraint test (başarısız olmalı)
INSERT INTO salons (google_place_id, name, address, rating)
VALUES ('test', 'Test', 'Addr', 9.9);
-- ERROR: salons_rating_check violation ✅

-- app user yetki testi
\c salon_explorer salon_app
DROP TABLE salons;
-- ERROR: must be owner of table salons ✅ (least privilege çalışıyor)
```

### 8.2 Spring Boot health endpoint

```bash
curl http://localhost:8080/actuator/health
```

Beklenen:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL", "validationQuery": "isValid()" } },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## 9. Yaygın Sorunlar

| Problem | Sebep | Çözüm |
|---------|-------|-------|
| `Connection refused` | Container çalışmıyor | `docker compose ps`, gerekirse `up -d` |
| `password authentication failed` | `.env` ile `application.yml` uyumsuz | İkisini de kontrol et, restart |
| `relation "salons" does not exist` | Flyway çalışmadı | Logları incele, `flyway_schema_history`'i kontrol et |
| `Validation failed: checksum mismatch` | Mevcut migration değiştirildi | Migration'ı geri al, yeni `V<n+1>` dosyası yaz |
| `permission denied for table` | App user yetkileri eksik | Init script'i tekrar çalıştır veya manuel GRANT |
| `port 5432 already in use` | Lokalde başka PG çalışıyor | `docker-compose.yml`'da `5433:5432` |
| `pg_trgm extension does not exist` | Extension'a yetki yok | Admin user ile `CREATE EXTENSION` |

---

## 10. Mülakat Soruları

> Bu fazda yaptıklarını savunabilmek için hazır cevaplar.

**S: Neden Flyway? Liquibase yerine?**
Flyway SQL-first, Liquibase XML/YAML abstraction katmanı. Spring Boot ekibinin native desteği var, ekibin SQL bilen herkes okuyabilir. Liquibase rollback'te biraz daha güçlü ama enterprise pattern'de zaten forward-only migration tercih edilir (rollback yerine yeni migration).

**S: Neden iki user (admin + app)?**
Least privilege principle. SQL injection'da attacker'ın yapabileceği maksimum hasarı sınırlandırır. Production'da bu standart pattern.

**S: Neden `ddl-auto: validate`?**
Schema management single source of truth — Flyway. Hibernate'in `create`/`update`'i prod'da felaket: çakışan migration'lar, beklenmedik şema değişiklikleri. `validate` modunda startup'ta entity ↔ tablo eşleşmesi kontrol edilir, aksi halde uygulama hiç başlamaz.

**S: Neden `BIGSERIAL` (BIGINT)?**
INTEGER overflow'u (2.1B) ileride sorun olabilir. Maliyet 4 byte vs 8 byte, ihmal edilebilir. Enterprise'da default BIGINT.

**S: `TIMESTAMPTZ` vs `TIMESTAMP`?**
TIMESTAMPTZ timezone bilgisini tutar, UTC normalize eder. Multi-region deployment, audit log, kullanıcı saat dilimi gösterimi için kritik. Polonya summer time DST yapıyor, TIMESTAMP olsa kafa karışırdı.

**S: Partial index nedir, neden kullandın?**
`WHERE is_active = TRUE` kısmıyla sadece aktif kayıtları indexledim. Soft-delete edilen kayıtlar zaten listelenmiyor, onları indexlemek index size'ı şişirir ve write performance'ı düşürür.

**S: `pg_trgm` extension neden lazım?**
Salon adında fuzzy search yapabilmek için. `WHERE name ILIKE '%anna%'` sorgusu normal B-tree index kullanamaz, `gin_trgm_ops` ile trigram index oluşturup hızlandırıyoruz.

**S: Audit log neden ayrı tablo, neden JSONB?**
Append-only design — main tablolardaki UPDATE'ler audit'i bozmasın. JSONB esnek schema sağlıyor; her entity için ayrı audit tablosu yapmak yerine generic bir yapı. JSONB'nin GIN indexi var, sorgulanabilir.

**S: `CASCADE` vs `SET NULL` neden farklı?**
`salon_services` → CASCADE: salon silinirse junction rec'ler de gitsin (mantıksız bir bağ kalmasın). `salons.district_id` → SET NULL: district silinse bile salon'u kaybetmek istemiyoruz. Domain mantığı belirliyor.

**S: Production'da farklı ne yapardın?**
- Read replica + connection pooling (PgBouncer)
- Managed service (AWS RDS, GCP Cloud SQL)
- Backup + point-in-time recovery
- pgBadger ile query analizi
- SSL/TLS enforced connections
- Secrets Manager / Vault entegrasyonu (password env var değil)
- Resource limits (CPU, memory, IOPS)

---

## 11. Definition of Done

Bu fazı bitirmiş sayılman için **hepsi** sağlanmalı:

- [ ] `docker compose up -d` ile PostgreSQL container ayağa kalkıyor
- [ ] `docker compose ps` çıktısında `Up (healthy)` görünüyor
- [ ] Admin ve app user'lar oluşmuş, yetki ayrımı çalışıyor
- [ ] 4 Flyway migration başarıyla uygulanmış (`flyway_schema_history`'de görünüyor)
- [ ] 18 district + 11 service seed'lenmiş
- [ ] Tüm constraint'ler çalışıyor (rating range, price level, vb.)
- [ ] `ddl-auto: validate` ile Spring Boot başlıyor (entity henüz yok ama Hibernate metadata kontrol edebilmeli)
- [ ] `/actuator/health` `UP` dönüyor, DB UP gözüküyor
- [ ] `.env` git'e gitmiyor, `.env.example` git'te var
- [ ] `docker-compose.yml` + tüm migration'lar + init script'ler commit'lenmiş
- [ ] `README.md`'de Phase 1 bölümünde "How to start the database" adımı yazılmış
- [ ] Container'ı durdurup tekrar başlattığında veri kayboluyor mu test edildi → kaybolmamalı (volume çalışıyor)

---

## ➡️ Sonraki Adım

**Faz 2: Data Collection (Google Places API → DB)**

Faz 2'de:
- Google Places API client'ı yazılacak
- Text search + Place Details endpoint'leri kullanılacak
- Veri normalize edilecek (phone format, district resolve)
- 100+ salon DB'ye yazılacak
- Tüm bunlar Spring Boot içinde bir `IngestionService` olarak gelecek
