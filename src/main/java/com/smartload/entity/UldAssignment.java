package com.smartload.entity;

import jakarta.persistence.*;

/**
 * UldAssignment — one physical ULD container assigned to one aircraft position
 * within a LoadPlan.
 *
 * Example: ULD of type "Code M" assigned to position P3R carries 12 packages
 * with a total weight of 3 200 kg and 87 % volume utilisation.
 */
@Entity
@Table(name = "uld_assignments")
public class UldAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "load_plan_id", nullable = false)
    private Long loadPlanId;

    /** Which physical slot on the aircraft this ULD occupies. */
    @Column(name = "aircraft_position_id", nullable = false)
    private Long aircraftPositionId;

    /** The ULD type loaded here (determines contour & dimensions). */
    @Column(name = "uld_type_id", nullable = false)
    private Long uldTypeId;

    /** Total gross weight of all packages in this ULD (kg). */
    @Column(name = "total_weight_kg")
    private Double totalWeightKg;

    /** Percentage of ULD bounding-box volume occupied by placed packages (0–100). */
    @Column(name = "utilization_pct")
    private Double utilizationPct;

    /** Number of package records placed in this ULD. */
    @Column(name = "package_count")
    private Integer packageCount;

    /**
     * LOFO loading order: 1 = first to load (deepest/aft), N = last to load (near door/nose).
     * Null when no flight stop sequence was provided at optimisation time.
     */
    @Column(name = "loading_order")
    private Integer loadingOrder;

    /**
     * Dominant destination code for this ULD (the destination code that appears
     * most often among its packages). Used for LOFO visualisation.
     */
    @Column(name = "dominant_destination", length = 10)
    private String dominantDestination;

    // ── Constructors ──────────────────────────────────────────────────────────

    public UldAssignment() {}

    public UldAssignment(Long loadPlanId, Long aircraftPositionId, Long uldTypeId,
                          double totalWeightKg, double utilizationPct, int packageCount) {
        this.loadPlanId         = loadPlanId;
        this.aircraftPositionId = aircraftPositionId;
        this.uldTypeId          = uldTypeId;
        this.totalWeightKg      = totalWeightKg;
        this.utilizationPct     = utilizationPct;
        this.packageCount       = packageCount;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long    getId()                          { return id; }
    public Long    getLoadPlanId()                  { return loadPlanId; }
    public void    setLoadPlanId(Long v)            { this.loadPlanId = v; }
    public Long    getAircraftPositionId()          { return aircraftPositionId; }
    public void    setAircraftPositionId(Long v)    { this.aircraftPositionId = v; }
    public Long    getUldTypeId()                   { return uldTypeId; }
    public void    setUldTypeId(Long v)             { this.uldTypeId = v; }
    public Double  getTotalWeightKg()               { return totalWeightKg; }
    public void    setTotalWeightKg(Double v)       { this.totalWeightKg = v; }
    public Double  getUtilizationPct()              { return utilizationPct; }
    public void    setUtilizationPct(Double v)      { this.utilizationPct = v; }
    public Integer getPackageCount()                    { return packageCount; }
    public void    setPackageCount(Integer v)           { this.packageCount = v; }
    public Integer getLoadingOrder()                    { return loadingOrder; }
    public void    setLoadingOrder(Integer v)           { this.loadingOrder = v; }
    public String  getDominantDestination()             { return dominantDestination; }
    public void    setDominantDestination(String v)     { this.dominantDestination = v; }
}
