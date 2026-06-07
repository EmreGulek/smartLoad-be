package com.smartload.dto;

import com.smartload.entity.LoadPlan;

/**
 * One row of a benchmark run: the metrics for a single algorithm on a manifest.
 *
 * Used by the thesis experimental comparison (algorithm × scenario × metrics).
 */
public class BenchmarkRowDto {

    private String  label;          // human-readable algorithm name
    private String  algorithm;      // algorithm id stored on the plan
    private Long    loadPlanId;     // the persisted plan (so it can be opened/visualised)
    private long    timeMs;         // wall-clock optimisation time (includes persistence)
    private int     placedPackages;
    private int     totalPackages;
    private double  utilizationPct;
    private double  totalWeightKg;
    private int     usedPositions;
    private Double  cgMacPct;
    private String  cgStatus;
    /** True when CG is inside the safe/warning envelope (not RED). */
    private boolean cgFeasible;

    public static BenchmarkRowDto from(String label, Long planId, long timeMs, LoadPlan p) {
        BenchmarkRowDto d = new BenchmarkRowDto();
        d.label          = label;
        d.algorithm      = p.getAlgorithm();
        d.loadPlanId     = planId;
        d.timeMs         = timeMs;
        d.placedPackages = p.getPlacedPackages() != null ? p.getPlacedPackages() : 0;
        d.totalPackages  = p.getTotalPackages()  != null ? p.getTotalPackages()  : 0;
        d.utilizationPct = p.getUtilizationPct() != null ? p.getUtilizationPct() : 0;
        d.totalWeightKg  = p.getTotalWeightKg()  != null ? p.getTotalWeightKg()  : 0;
        d.usedPositions  = p.getUsedPositions()  != null ? p.getUsedPositions()  : 0;
        d.cgMacPct       = p.getCgMacPct();
        d.cgStatus       = p.getCgStatus();
        d.cgFeasible     = p.getCgStatus() != null && !p.getCgStatus().startsWith("RED");
        return d;
    }

    public String  getLabel()          { return label; }
    public String  getAlgorithm()      { return algorithm; }
    public Long    getLoadPlanId()     { return loadPlanId; }
    public long    getTimeMs()         { return timeMs; }
    public int     getPlacedPackages() { return placedPackages; }
    public int     getTotalPackages()  { return totalPackages; }
    public double  getUtilizationPct() { return utilizationPct; }
    public double  getTotalWeightKg()  { return totalWeightKg; }
    public int     getUsedPositions()  { return usedPositions; }
    public Double  getCgMacPct()       { return cgMacPct; }
    public String  getCgStatus()       { return cgStatus; }
    public boolean isCgFeasible()      { return cgFeasible; }
}
