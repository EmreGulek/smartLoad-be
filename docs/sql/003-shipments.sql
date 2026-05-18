-- SmartLoad: shipments table
-- Maps to: com.smartload.entity.Shipment
-- ADR-0011: Hibernate ddl-auto=update auto-creates the table from the entity in dev. This
-- script is the canonical reference; run manually only if you need to recreate the schema
-- on a fresh DB before the application boots, or in CI/prod when ddl-auto is off.

CREATE TABLE IF NOT EXISTS shipments (
    id                 VARCHAR(36)   PRIMARY KEY,
    manifest_id        VARCHAR(36)   NOT NULL,
    code               VARCHAR(255)  NOT NULL,
    origin_code        VARCHAR(8),
    destination_code   VARCHAR(8),
    can_mix            BOOLEAN,
    created_at         TIMESTAMP     NOT NULL,
    CONSTRAINT fk_shipment_manifest FOREIGN KEY (manifest_id)
        REFERENCES manifests (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shipments_manifest_id ON shipments (manifest_id);
CREATE INDEX IF NOT EXISTS idx_shipments_destination ON shipments (destination_code);
