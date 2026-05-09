-- SmartLoad initial schema
-- Phase 0: Bootstrap migration so Flyway has a baseline.
-- Phase 1+ migrations (V2__*, V3__*, ...) will create the real tables:
--   aircraft, uld_types, aircraft_positions, users, flights,
--   shipments, packages, load_plans, uld_assignments,
--   position_assignments, audit_log
-- See MASTER-PLAN.md Phase 1 + entities/*.md for the schema design.

CREATE TABLE IF NOT EXISTS schema_bootstrap (
    id          SERIAL PRIMARY KEY,
    note        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO schema_bootstrap (note) VALUES ('SmartLoad schema initialized — Phase 0');
