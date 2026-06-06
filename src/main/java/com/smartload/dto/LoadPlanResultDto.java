package com.smartload.dto;

import com.smartload.entity.LoadPlan;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full load plan result DTO — returned by GET /api/load-plans/{id}.
 *
 * Contains the top-level plan summary and all ULD assignments with
 * their package placements. This is the primary payload consumed by
 * LoadPlanPage and the B777FViewer package visualisation layer.
 */
public class LoadPlanResultDto {

    private Long      id;
    private String    manifestId;
    private Long      aircraftId;
    private String    status;
    private String    algorithm;
    private double    utilizationPct;
    private int       placedPackages;
    private int       totalPackages;
    private double    totalWeightKg;
    private int       usedPositions;
    private LocalDateTime createdAt;

    private List<UldAssignmentDto> assignments;

    /** CG calculation result (OEW + cargo). Null only if assignments list is empty. */
    private CgResultDto cg;

    public static LoadPlanResultDto from(LoadPlan plan, List<UldAssignmentDto> assignments) {
        LoadPlanResultDto d = new LoadPlanResultDto();
        d.id              = plan.getId();
        d.manifestId      = plan.getManifestId();
        d.aircraftId      = plan.getAircraftId();
        d.status          = plan.getStatus();
        d.algorithm       = plan.getAlgorithm();
        d.utilizationPct  = plan.getUtilizationPct() != null ? plan.getUtilizationPct() : 0;
        d.placedPackages  = plan.getPlacedPackages() != null ? plan.getPlacedPackages() : 0;
        d.totalPackages   = plan.getTotalPackages()  != null ? plan.getTotalPackages()  : 0;
        d.totalWeightKg   = plan.getTotalWeightKg()  != null ? plan.getTotalWeightKg()  : 0;
        d.usedPositions   = plan.getUsedPositions()  != null ? plan.getUsedPositions()  : 0;
        d.createdAt       = plan.getCreatedAt();
        d.assignments     = assignments;
        d.cg              = CgResultDto.compute(assignments);
        return d;
    }

    public Long      getId()             { return id; }
    public String    getManifestId()     { return manifestId; }
    public Long      getAircraftId()     { return aircraftId; }
    public String    getStatus()         { return status; }
    public String    getAlgorithm()      { return algorithm; }
    public double    getUtilizationPct() { return utilizationPct; }
    public int       getPlacedPackages() { return placedPackages; }
    public int       getTotalPackages()  { return totalPackages; }
    public double    getTotalWeightKg()  { return totalWeightKg; }
    public int       getUsedPositions()  { return usedPositions; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public List<UldAssignmentDto> getAssignments() { return assignments; }
    public CgResultDto getCg()           { return cg; }
}
