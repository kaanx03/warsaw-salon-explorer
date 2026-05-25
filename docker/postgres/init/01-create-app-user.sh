#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Application user (least privilege)
    CREATE USER ${APP_DB_USER:-salon_app} WITH PASSWORD '${APP_DB_PASSWORD:-change_me_app_password}';

    -- Schema permissions
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${APP_DB_USER:-salon_app};
    GRANT USAGE ON SCHEMA public TO ${APP_DB_USER:-salon_app};

    -- Grant privileges on tables Flyway will create later
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${APP_DB_USER:-salon_app};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO ${APP_DB_USER:-salon_app};
EOSQL
