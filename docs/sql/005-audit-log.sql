-- SmartLoad: audit_log — package edits on saved manifests (MASTER-PLAN §7)
-- Maps to: com.smartload.entity.AuditLog

CREATE TABLE IF NOT EXISTS audit_log (
    id            VARCHAR(36)       PRIMARY KEY,
    manifest_id   VARCHAR(36)       NOT NULL,
    package_id    VARCHAR(36),
    entity_type   VARCHAR(64)       NOT NULL,
    field_name    VARCHAR(128)      NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    user_email    VARCHAR(255),
    created_at    TIMESTAMP         NOT NULL,
    CONSTRAINT fk_audit_manifest FOREIGN KEY (manifest_id)
        REFERENCES manifests (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_audit_log_manifest_id ON audit_log (manifest_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_package_id ON audit_log (package_id);
