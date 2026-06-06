package com.smartload.entity;

import jakarta.persistence.*;

/**
 * AircraftPosition — a single ULD slot on the aircraft floor plan.
 *
 * Each row is one physical loading position (e.g. "P1L", "P3R", "P8C").
 * L = left/port, R = right/starboard, C = centreline.
 *
 * Coordinate convention (all mm, Three.js Y-up compatible):
 *   armMm           — longitudinal distance from datum; maps directly to Three.js Z.
 *                     Positive = forward (nose direction in current viewer).
 *   lateralOffsetMm — lateral distance from aircraft centreline; maps to Three.js X.
 *                     Positive = starboard (right), negative = port (left).
 *
 * The frontend converts with positionToScene() in utils/aircraftCoords.js:
 *   three.x = lateralOffsetMm / 1000
 *   three.z = armMm / 1000
 *   three.y = (floor_mm + maxContourY_mm) / 1000  (derived from aircraft + uldType)
 *
 * Note: "burun=0, kuyruk=+pozitif" datum assumption per ADR-0012.
 * Real Boeing datum calibration deferred to Faz 6.
 */
@Entity
@Table(name = "aircraft_position")
public class AircraftPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uld_type_id", nullable = false)
    private UldType uldType;

    /** Slot identifier, e.g. "P1L", "P3R", "P8C". */
    @Column(name = "position_code", nullable = false, length = 10)
    private String positionCode;

    /**
     * Longitudinal arm from datum in mm.
     * Maps to Three.js Z: scene_z = armMm / 1000.
     */
    @Column(name = "arm_mm", nullable = false)
    private Integer armMm;

    /**
     * Lateral offset from aircraft centreline in mm.
     * Positive = starboard, negative = port.
     * Maps to Three.js X: scene_x = lateralOffsetMm / 1000.
     */
    @Column(name = "lateral_offset_mm", nullable = false)
    private Integer lateralOffsetMm;

    /** "MAIN" or "LOWER". Determines which floor Y to use. */
    @Column(name = "deck", nullable = false, length = 10)
    private String deck;

    /** Front-to-back render order (lower = further forward). */
    @Column(name = "display_order")
    private Integer displayOrder;

    /** Soft-delete: false = position removed from active layout. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // ── Constructors ──────────────────────────────────────────────────────────

    public AircraftPosition() {}

    public AircraftPosition(Aircraft aircraft, UldType uldType,
                             String positionCode, int armMm, int lateralOffsetMm,
                             String deck, int displayOrder) {
        this.aircraft          = aircraft;
        this.uldType           = uldType;
        this.positionCode      = positionCode;
        this.armMm             = armMm;
        this.lateralOffsetMm   = lateralOffsetMm;
        this.deck              = deck;
        this.displayOrder      = displayOrder;
        this.isActive          = true;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long     getId()                         { return id; }
    public Aircraft getAircraft()                   { return aircraft; }
    public void     setAircraft(Aircraft v)         { this.aircraft = v; }
    public UldType  getUldType()                    { return uldType; }
    public void     setUldType(UldType v)           { this.uldType = v; }
    public String   getPositionCode()               { return positionCode; }
    public void     setPositionCode(String v)       { this.positionCode = v; }
    public Integer  getArmMm()                      { return armMm; }
    public void     setArmMm(Integer v)             { this.armMm = v; }
    public Integer  getLateralOffsetMm()            { return lateralOffsetMm; }
    public void     setLateralOffsetMm(Integer v)   { this.lateralOffsetMm = v; }
    public String   getDeck()                       { return deck; }
    public void     setDeck(String v)               { this.deck = v; }
    public Integer  getDisplayOrder()               { return displayOrder; }
    public void     setDisplayOrder(Integer v)      { this.displayOrder = v; }
    public Boolean  getIsActive()                   { return isActive; }
    public void     setIsActive(Boolean v)          { this.isActive = v; }
}
