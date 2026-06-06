-- ============================================================
-- 006-load-plans.sql — Bin packing results
-- Faz 3: Bin Packing Algoritması
-- Reference only — Hibernate ddl-auto=update creates tables.
-- ============================================================

CREATE TABLE load_plans (
    id               BIGSERIAL    PRIMARY KEY,
    manifest_id      VARCHAR(36)  NOT NULL,   -- FK to manifests.id (UUID)
    aircraft_id      BIGINT       NOT NULL REFERENCES aircraft(id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    algorithm        VARCHAR(50),
    utilization_pct  DOUBLE PRECISION,
    placed_packages  INTEGER,
    total_packages   INTEGER,
    total_weight_kg  DOUBLE PRECISION,
    used_positions   INTEGER,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE uld_assignments (
    id                    BIGSERIAL PRIMARY KEY,
    load_plan_id          BIGINT NOT NULL REFERENCES load_plans(id),
    aircraft_position_id  BIGINT NOT NULL REFERENCES aircraft_position(id),
    uld_type_id           BIGINT NOT NULL REFERENCES uld_type(id),
    total_weight_kg       DOUBLE PRECISION,
    utilization_pct       DOUBLE PRECISION,
    package_count         INTEGER
);

CREATE TABLE package_placements (
    id                  BIGSERIAL PRIMARY KEY,
    uld_assignment_id   BIGINT NOT NULL REFERENCES uld_assignments(id),
    package_id          VARCHAR(36) NOT NULL,  -- FK to packages.id (UUID)
    x_mm                INTEGER NOT NULL,
    y_mm                INTEGER NOT NULL,
    z_mm                INTEGER NOT NULL,
    applied_width_mm    INTEGER NOT NULL,
    applied_height_mm   INTEGER NOT NULL,
    applied_depth_mm    INTEGER NOT NULL,
    rotation_index      INTEGER NOT NULL
);

CREATE INDEX idx_load_plans_manifest   ON load_plans(manifest_id);
CREATE INDEX idx_uld_assignments_plan  ON uld_assignments(load_plan_id);
CREATE INDEX idx_pkg_placements_uld    ON package_placements(uld_assignment_id);
