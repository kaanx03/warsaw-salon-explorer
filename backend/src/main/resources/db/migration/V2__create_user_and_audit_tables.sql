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
