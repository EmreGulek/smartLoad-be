package com.smartload.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * LoadPlan — top-level result of a bin-packing optimization run.
 *
 * One LoadPlan ties a cargo manifest to a B777F loading configuration.
 * A manifest can have multiple load plans (versioning for "what-if" analysis).
 *
 * Status lifecycle:  DRAFT → OPTIMIZED → APPROVED
 *   DRAFT     : created, not yet run
 *   OPTIMIZED : algorithm ran successfully
 *   APPROVED  : loadmaster signed off (Faz 6)
 */
@Entity
@Table(name = "load_plans")
public class LoadPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manifest_id", nullable = false)
    private String manifestId;

    @Column(name = "aircraft_id", nullable = false)
    private Long aircraftId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;  // DRAFT | OPTIMIZED | APPROVED

    /** Algorithm identifier for academic comparison. E.g. "EXTREME_POINTS_GREEDY_V1". */
    @Column(name = "algorithm", length = 50)
    private String algorithm;

    /** Volume utilisation across all assigned ULD positions (0–100). */
    @Column(name = "utilization_pct")
    private Double utilizationPct;

    /** Number of package records successfully placed. */
    @Column(name = "placed_packages")
    private Integer placedPackages;

    /** Total package records in the manifest (including unplaced). */
    @Column(name = "total_packages")
    private Integer totalPackages;

    /** Sum of grossWeightKg for all placed packages. */
    @Column(name = "total_weight_kg")
    private Double totalWeightKg;

    /** Number of aircraft positions (ULD slots) actually used. */
    @Column(name = "used_positions")
    private Integer usedPositions;

    /**
     * Final centre-of-gravity of the plan, expressed as %MAC (OEW + cargo).
     * Recorded by the optimizer so CG is a first-class result field (benchmark
     * metric + CG feasibility tracking). Null for legacy plans created before Faz 5.5.
     */
    @Column(name = "cg_mac_pct")
    private Double cgMacPct;

    /** CG envelope status: GREEN | YELLOW_FWD | YELLOW_AFT | RED_FWD | RED_AFT. */
    @Column(name = "cg_status", length = 20)
    private String cgStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LoadPlan() {
        this.createdAt = LocalDateTime.now();
        this.status    = "DRAFT";
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long          getId()              { return id; }
    public String        getManifestId()      { return manifestId; }
    public void          setManifestId(String v)  { this.manifestId = v; }
    public Long          getAircraftId()      { return aircraftId; }
    public void          setAircraftId(Long v)    { this.aircraftId = v; }
    public String        getStatus()          { return status; }
    public void          setStatus(String v)  { this.status = v; }
    public String        getAlgorithm()       { return algorithm; }
    public void          setAlgorithm(String v)   { this.algorithm = v; }
    public Double        getUtilizationPct()  { return utilizationPct; }
    public void          setUtilizationPct(Double v) { this.utilizationPct = v; }
    public Integer       getPlacedPackages()  { return placedPackages; }
    public void          setPlacedPackages(Integer v) { this.placedPackages = v; }
    public Integer       getTotalPackages()   { return totalPackages; }
    public void          setTotalPackages(Integer v) { this.totalPackages = v; }
    public Double        getTotalWeightKg()   { return totalWeightKg; }
    public void          setTotalWeightKg(Double v) { this.totalWeightKg = v; }
    public Integer       getUsedPositions()   { return usedPositions; }
    public void          setUsedPositions(Integer v) { this.usedPositions = v; }
    public Double        getCgMacPct()        { return cgMacPct; }
    public void          setCgMacPct(Double v) { this.cgMacPct = v; }
    public String        getCgStatus()        { return cgStatus; }
    public void          setCgStatus(String v) { this.cgStatus = v; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
}
