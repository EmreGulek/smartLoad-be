package com.smartload.entity;

import jakarta.persistence.*;

/**
 * Aircraft entity — represents a cargo aircraft type.
 *
 * Faz 2: B777F is seeded as aircraft_id=1.
 * Future: A330F, B747F, etc. can be added as additional rows.
 *
 * Coordinate units: all dimensions in millimetres (mm).
 * floorMainDeckMm and floorLowerDeckMm are Y-offsets from fuselage centre
 * (negative = below centreline). These match Three.js Y-up convention
 * after dividing by 1000.
 */
@Entity
@Table(name = "aircraft")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ICAO aircraft type designator, e.g. "B77F". */
    @Column(name = "icao_code", nullable = false, unique = true, length = 10)
    private String icaoCode;

    /** Human-readable name, e.g. "Boeing 777F". */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Fuselage inner radius in mm. B777F ≈ 3095 mm. */
    @Column(name = "fuselage_radius_mm", nullable = false)
    private Integer fuselageRadiusMm;

    /**
     * Main deck floor Y offset from fuselage centre in mm.
     * Negative = below centre. B777F ≈ -680 mm.
     */
    @Column(name = "floor_main_deck_mm", nullable = false)
    private Integer floorMainDeckMm;

    /**
     * Lower (belly) deck floor Y offset from fuselage centre in mm.
     * B777F ≈ -2480 mm.
     */
    @Column(name = "floor_lower_deck_mm", nullable = false)
    private Integer floorLowerDeckMm;

    /** Maximum structural payload in kg. B777F ≈ 102 760 kg. */
    @Column(name = "max_payload_kg")
    private Integer maxPayloadKg;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Aircraft() {}

    public Aircraft(String icaoCode, String name,
                    int fuselageRadiusMm, int floorMainDeckMm,
                    int floorLowerDeckMm, int maxPayloadKg) {
        this.icaoCode        = icaoCode;
        this.name            = name;
        this.fuselageRadiusMm   = fuselageRadiusMm;
        this.floorMainDeckMm    = floorMainDeckMm;
        this.floorLowerDeckMm   = floorLowerDeckMm;
        this.maxPayloadKg       = maxPayloadKg;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long    getId()               { return id; }
    public String  getIcaoCode()         { return icaoCode; }
    public void    setIcaoCode(String v) { this.icaoCode = v; }
    public String  getName()             { return name; }
    public void    setName(String v)     { this.name = v; }
    public Integer getFuselageRadiusMm()          { return fuselageRadiusMm; }
    public void    setFuselageRadiusMm(Integer v) { this.fuselageRadiusMm = v; }
    public Integer getFloorMainDeckMm()           { return floorMainDeckMm; }
    public void    setFloorMainDeckMm(Integer v)  { this.floorMainDeckMm = v; }
    public Integer getFloorLowerDeckMm()          { return floorLowerDeckMm; }
    public void    setFloorLowerDeckMm(Integer v) { this.floorLowerDeckMm = v; }
    public Integer getMaxPayloadKg()              { return maxPayloadKg; }
    public void    setMaxPayloadKg(Integer v)     { this.maxPayloadKg = v; }
}
