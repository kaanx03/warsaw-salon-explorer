-- ============================================================
-- V3: Performance indexes
-- ============================================================

-- gin_trgm_ops için extension önce yüklenmeli
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Salons: en sık filtrelenen kolonlar (partial index — sadece aktif salonlar)
CREATE INDEX idx_salons_district_id  ON salons(district_id) WHERE is_active = TRUE;
CREATE INDEX idx_salons_rating       ON salons(rating DESC) WHERE is_active = TRUE;
CREATE INDEX idx_salons_price_level  ON salons(price_level) WHERE is_active = TRUE;
CREATE INDEX idx_salons_is_active    ON salons(is_active);

-- Full-text fuzzy search: salon adında trigram index
CREATE INDEX idx_salons_name_trgm
    ON salons USING gin (name gin_trgm_ops);

-- Audit log: en sık entity_type + entity_id ile sorgulanır
CREATE INDEX idx_audit_entity      ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_created_at  ON audit_log(created_at DESC);
CREATE INDEX idx_audit_user_id     ON audit_log(user_id);
