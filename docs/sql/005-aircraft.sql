-- ============================================================
-- 005-aircraft.sql — Aircraft, ULD types, positions
-- Faz 2: Sahne Dinamikleştirme
-- Reference only — Hibernate ddl-auto=update creates tables.
-- ============================================================

CREATE TABLE aircraft (
    id                   BIGSERIAL PRIMARY KEY,
    icao_code            VARCHAR(10)  NOT NULL UNIQUE,
    name                 VARCHAR(100) NOT NULL,
    fuselage_radius_mm   INTEGER      NOT NULL,
    floor_main_deck_mm   INTEGER      NOT NULL,
    floor_lower_deck_mm  INTEGER      NOT NULL,
    max_payload_kg       INTEGER
);

CREATE TABLE uld_type (
    id             BIGSERIAL PRIMARY KEY,
    aircraft_id    BIGINT       NOT NULL REFERENCES aircraft(id),
    code           VARCHAR(20)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    contour_label  VARCHAR(50),
    color_hex      VARCHAR(10),
    length_mm      INTEGER      NOT NULL,
    points_json    TEXT         NOT NULL  -- JSON array [[x,y],…] in mm
);

CREATE TABLE aircraft_position (
    id                  BIGSERIAL PRIMARY KEY,
    aircraft_id         BIGINT      NOT NULL REFERENCES aircraft(id),
    uld_type_id         BIGINT      NOT NULL REFERENCES uld_type(id),
    position_code       VARCHAR(10) NOT NULL,
    arm_mm              INTEGER     NOT NULL,  -- longitudinal, maps to Three.js Z
    lateral_offset_mm   INTEGER     NOT NULL,  -- lateral, maps to Three.js X
    deck                VARCHAR(10) NOT NULL,  -- 'MAIN' or 'LOWER'
    display_order       INTEGER,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_aircraft_position_aircraft ON aircraft_position(aircraft_id);
CREATE INDEX idx_uld_type_aircraft          ON uld_type(aircraft_id);
