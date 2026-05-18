-- SmartLoad: users table
-- Maps to: com.smartload.entity.User
-- Run against database: smartload (see application.properties)

CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL PRIMARY KEY,
    username                VARCHAR(255) NOT NULL UNIQUE,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    enabled                 BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_code       VARCHAR(255),
    verification_expiration TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_verification_code ON users (verification_code);
