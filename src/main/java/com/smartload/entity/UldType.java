package com.smartload.entity;

import jakarta.persistence.*;

/**
 * ULD (Unit Load Device) type — defines the cross-section contour and dimensions
 * of a specific cargo container or pallet type used on a given aircraft.
 *
 * Faz 2 seed: A, M, R_HIGH, R_LOW, G, LD3 for B777F.
 *
 * Coordinate units: mm.
 * pointsJson: JSON array of [x, y] pairs (mm) defining the 2D cross-section polygon.
 * Example: "[[0,0],[2440,0],[2440,1800],[1700,2950],[0,2950]]"
 *
 * The frontend divides all point values by 1000 before building Three.js geometry.
 */
@Entity
@Table(name = "uld_type")
public class UldType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    /**
     * Short code used to identify this ULD type in code and DB references.
     * Examples: "A", "M", "R_HIGH", "R_LOW", "G", "LD3"
     */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    /** Display name. Example: "Code A – Nose Container". */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** IATA contour label. Example: "Q4 (Narrow)". */
    @Column(name = "contour_label", length = 50)
    private String contourLabel;

    /** CSS hex colour for 3D rendering. Example: "#ffaa00". */
    @Column(name = "color_hex", length = 10)
    private String colorHex;

    /** Extrusion depth (length along longitudinal axis) in mm. */
    @Column(name = "length_mm", nullable = false)
    private Integer lengthMm;

    /**
     * JSON array of [x, y] points (mm) for the 2D cross-section shape.
     * Stored as plain TEXT; parsed by the frontend via JSON.parse().
     */
    @Column(name = "points_json", nullable = false, columnDefinition = "text")
    private String pointsJson;

    // ── Constructors ──────────────────────────────────────────────────────────

    public UldType() {}

    public UldType(Aircraft aircraft, String code, String name,
                   String contourLabel, String colorHex,
                   int lengthMm, String pointsJson) {
        this.aircraft     = aircraft;
        this.code         = code;
        this.name         = name;
        this.contourLabel = contourLabel;
        this.colorHex     = colorHex;
        this.lengthMm     = lengthMm;
        this.pointsJson   = pointsJson;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long     getId()                  { return id; }
    public Aircraft getAircraft()            { return aircraft; }
    public void     setAircraft(Aircraft v)  { this.aircraft = v; }
    public String   getCode()                { return code; }
    public void     setCode(String v)        { this.code = v; }
    public String   getName()                { return name; }
    public void     setName(String v)        { this.name = v; }
    public String   getContourLabel()        { return contourLabel; }
    public void     setContourLabel(String v){ this.contourLabel = v; }
    public String   getColorHex()            { return colorHex; }
    public void     setColorHex(String v)    { this.colorHex = v; }
    public Integer  getLengthMm()            { return lengthMm; }
    public void     setLengthMm(Integer v)   { this.lengthMm = v; }
    public String   getPointsJson()          { return pointsJson; }
    public void     setPointsJson(String v)  { this.pointsJson = v; }
}
