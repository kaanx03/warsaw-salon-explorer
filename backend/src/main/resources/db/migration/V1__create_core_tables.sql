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
