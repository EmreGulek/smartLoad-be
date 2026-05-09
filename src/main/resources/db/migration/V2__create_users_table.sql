-- V2: User authentication table
-- Faz 1 (auth flow) — JWT-based authentication with email verification.
-- Email verification fields will be activated in Faz 6 when SMTP is wired.

CREATE TABLE users (
    id                      BIGSERIAL    PRIMARY KEY,
    username                VARCHAR(50)  NOT NULL UNIQUE,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    enabled                 BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_code       VARCHAR(10),
    verification_expiration TIMESTAMP
);

CREATE INDEX idx_users_email             ON users(email);
CREATE INDEX idx_users_verification_code ON users(verification_code);

COMMENT ON TABLE  users                   IS 'Loadmaster authentication accounts';
COMMENT ON COLUMN users.password          IS 'BCrypt hashed password — never plaintext';
COMMENT ON COLUMN users.enabled           IS 'True after email verification (Faz 6) or manual activation';
COMMENT ON COLUMN users.verification_code IS '6-digit code; persisted but unused in Faz 1 (console log only)';
