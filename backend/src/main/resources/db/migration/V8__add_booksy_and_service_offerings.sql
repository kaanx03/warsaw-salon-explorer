ALTER TABLE salons ADD COLUMN IF NOT EXISTS booksy_id VARCHAR(100);

CREATE TABLE IF NOT EXISTS service_offerings (
    id              BIGSERIAL PRIMARY KEY,
    salon_id        BIGINT        NOT NULL REFERENCES salons(id) ON DELETE CASCADE,
    name            VARCHAR(255)  NOT NULL,
    category        VARCHAR(100),
    price_pln       NUMERIC(8,2),
    duration_minutes INTEGER,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_service_offerings_salon ON service_offerings(salon_id);
