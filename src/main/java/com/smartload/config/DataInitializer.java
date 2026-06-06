package com.smartload.config;

import com.smartload.entity.Aircraft;
import com.smartload.entity.AircraftPosition;
import com.smartload.entity.UldType;
import com.smartload.repository.AircraftPositionRepository;
import com.smartload.repository.AircraftRepository;
import com.smartload.repository.UldTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DataInitializer — seeds Boeing 777F aircraft configuration on first startup.
 *
 * Runs only when the aircraft table is empty (idempotent).
 *
 * Coordinate sources:
 *   - raw/docs/B777F_Inspection_System.txt  (original Three.js viewer, Phase 0)
 *   - raw/docs/B777F Main Deck ULD … .pptx  (geometric truth — Phase 2 calibrated)
 *
 * All values stored in millimetres (mm).
 * Three.js scene coordinates: divide by 1000 (see utils/aircraftCoords.js).
 *
 * Position arm/lateral values are derived from the Phase 0 Three.js viewer
 * coordinates (metres × 1000 = mm). Boeing datum calibration deferred to Faz 6
 * per ADR-0012.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AircraftRepository         aircraftRepo;
    private final UldTypeRepository          uldTypeRepo;
    private final AircraftPositionRepository positionRepo;

    public DataInitializer(AircraftRepository aircraftRepo,
                           UldTypeRepository uldTypeRepo,
                           AircraftPositionRepository positionRepo) {
        this.aircraftRepo = aircraftRepo;
        this.uldTypeRepo  = uldTypeRepo;
        this.positionRepo = positionRepo;
    }

    @Override
    public void run(String... args) {
        if (aircraftRepo.count() > 0) {
            log.info("DataInitializer: aircraft table already populated — skipping seed.");
            return;
        }

        log.info("DataInitializer: seeding Boeing 777F configuration…");

        // ── 1. Aircraft ───────────────────────────────────────────────────────
        Aircraft b777f = aircraftRepo.save(new Aircraft(
            "B77F",
            "Boeing 777F",
            3095,   // fuselageRadiusMm  — IATA L-91 outer envelope radius
            -680,   // floorMainDeckMm   — main deck floor Y below centreline
            -2480,  // floorLowerDeckMm  — lower (belly) deck floor Y
            102760  // maxPayloadKg
        ));

        // ── 2. ULD Types ─────────────────────────────────────────────────────
        // Points: 2D cross-section polygon [[x,y], …] in mm.
        // Length: extrusion depth (longitudinal) in mm.
        // Derived from b777fContours.js (Phase 0, metres) × 1000.

        UldType typeA = uldTypeRepo.save(new UldType(b777f,
            "A", "Code A – Nose Container", "Q4 (Narrow)", "#ffaa00",
            3180,
            "[[0,0],[2440,0],[2440,1800],[1700,2950],[0,2950]]"
        ));

        UldType typeM = uldTypeRepo.save(new UldType(b777f,
            "M", "Code M – Standard Main Deck", "Q5 (Asym)", "#00ff88",
            3180,
            "[[0,0],[2440,0],[2440,1920],[1880,3000],[0,3000]]"
        ));

        UldType typeRHigh = uldTypeRepo.save(new UldType(b777f,
            "R_HIGH", "Code R – 16ft Pallet (High Profile)", "High Profile", "#0088ff",
            4960,
            "[[0,0],[2440,0],[2440,1350],[2360,2740],[0,2740]]"
        ));

        UldType typeRLow = uldTypeRepo.save(new UldType(b777f,
            "R_LOW", "Code R – 16ft Pallet (Low Profile)", "Low Profile", "#0055aa",
            4960,
            "[[0,0],[2440,0],[2440,1950],[2030,2430],[0,2430]]"
        ));

        UldType typeG = uldTypeRepo.save(new UldType(b777f,
            "G", "Code G – 20ft Centreline", "Q6 (Rect)", "#9900ff",
            6060,
            "[[-1220,0],[1220,0],[1220,3000],[-1220,3000]]"
        ));

        UldType typeLd3 = uldTypeRepo.save(new UldType(b777f,
            "LD3", "LD3 – Belly Container", "LD3", "#00ffff",
            1530,
            "[[0,0],[1560,0],[2000,450],[2000,1630],[0,1630]]"
        ));

        // ULD type lookup by code for convenience
        Map<String, UldType> types = Map.of(
            "A",      typeA,
            "M",      typeM,
            "R_HIGH", typeRHigh,
            "R_LOW",  typeRLow,
            "G",      typeG,
            "LD3",    typeLd3
        );

        // ── 3. Aircraft Positions — Main Deck ─────────────────────────────────
        // arm_mm      = Three.js z × 1000  (positive = forward/nose direction)
        // lateral_mm  = Three.js x × 1000  (positive = starboard/right)
        // Positions are ordered forward → aft (displayOrder ascending).

        int order = 1;

        // P1 pair — Code A (z=25 000 mm)
        positionRepo.save(new AircraftPosition(b777f, typeA, "P1R",  25000,  1300, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeA, "P1L",  25000, -1300, "MAIN", order++));
        // P2 pair — Code A (z=21 600 mm)
        positionRepo.save(new AircraftPosition(b777f, typeA, "P2R",  21600,  1300, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeA, "P2L",  21600, -1300, "MAIN", order++));
        // P3 pair — Code M (z=18 200 mm)
        positionRepo.save(new AircraftPosition(b777f, typeM, "P3R",  18200,  1350, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeM, "P3L",  18200, -1350, "MAIN", order++));
        // P4 pair — Code M (z=14 800 mm)
        positionRepo.save(new AircraftPosition(b777f, typeM, "P4R",  14800,  1350, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeM, "P4L",  14800, -1350, "MAIN", order++));
        // P5 pair — Code M (z=11 400 mm)
        positionRepo.save(new AircraftPosition(b777f, typeM, "P5R",  11400,  1350, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeM, "P5L",  11400, -1350, "MAIN", order++));
        // P6 pair — Code R High (z=7 000 mm)
        positionRepo.save(new AircraftPosition(b777f, typeRHigh, "P6R",  7000,  1350, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeRHigh, "P6L",  7000, -1350, "MAIN", order++));
        // P7 pair — Code R Low (z=1 800 mm)
        positionRepo.save(new AircraftPosition(b777f, typeRLow, "P7R",  1800,  1350, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeRLow, "P7L",  1800, -1350, "MAIN", order++));
        // P8 — Code G centreline (z=-3 900 mm)
        positionRepo.save(new AircraftPosition(b777f, typeG, "P8C", -3900,     0, "MAIN", order++));
        // P9 pair — Code R High (z=-9 900 mm, aft)
        positionRepo.save(new AircraftPosition(b777f, typeRHigh, "P9R", -9900,  1350, "MAIN", order++));
        positionRepo.save(new AircraftPosition(b777f, typeRHigh, "P9L", -9900, -1350, "MAIN", order++));

        log.info("DataInitializer: B777F seed complete — {} aircraft, {} ULD types, {} positions.",
            aircraftRepo.count(), uldTypeRepo.count(), positionRepo.count());
    }
}
