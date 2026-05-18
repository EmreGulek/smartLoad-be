-- SmartLoad: packages table
-- Maps to: com.smartload.entity.Package
-- ADR-0011: Hibernate ddl-auto=update auto-creates the table from the entity in dev. This
-- script is the canonical reference; run manually only if you need to recreate the schema
-- on a fresh DB before the application boots, or in CI/prod when ddl-auto is off.
--
-- All dimension columns are in MILLIMETRES (mm). Excel imports come in cm; conversion
-- happens once in ManifestValidationService.parseAndValidateRow.

CREATE TABLE IF NOT EXISTS packages (
    id                    VARCHAR(36)       PRIMARY KEY,
    manifest_id           VARCHAR(36)       NOT NULL,
    shipment_id           VARCHAR(36),
    source_row_number     INTEGER,
    pieces                INTEGER           NOT NULL,
    length_mm             DOUBLE PRECISION  NOT NULL,
    width_mm              DOUBLE PRECISION  NOT NULL,
    height_mm             DOUBLE PRECISION  NOT NULL,
    weight_per_piece_kg   DOUBLE PRECISION,
    gross_weight_kg       DOUBLE PRECISION  NOT NULL,
    stackable             BOOLEAN,
    rotatable             BOOLEAN,
    toploadable           BOOLEAN,
    can_mix               BOOLEAN,
    uld_preference        VARCHAR(64),
    destination_code      VARCHAR(8)        NOT NULL,
    special_handling      VARCHAR(255),
    dg_class              VARCHAR(8),
    created_at            TIMESTAMP         NOT NULL,
    CONSTRAINT fk_package_manifest FOREIGN KEY (manifest_id)
        REFERENCES manifests (id) ON DELETE CASCADE,
    CONSTRAINT fk_package_shipment FOREIGN KEY (shipment_id)
        REFERENCES shipments (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_packages_manifest_id ON packages (manifest_id);
CREATE INDEX IF NOT EXISTS idx_packages_shipment_id ON packages (shipment_id);
CREATE INDEX IF NOT EXISTS idx_packages_destination ON packages (destination_code);
CREATE INDEX IF NOT EXISTS idx_packages_dg_class ON packages (dg_class);
