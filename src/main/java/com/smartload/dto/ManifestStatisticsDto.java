package com.smartload.dto;

import java.util.Map;

/**
 * Statistics calculated from validated packages.
 * Displayed in STATISTICS PREVIEW (Faz 1, adım 6).
 */
public class ManifestStatisticsDto {
    private Integer totalPieces;
    private Double totalWeightKg;
    private Double totalVolumeMm3; // canonical backend unit; UI divides by 10⁹ to display m³
    private Double averageDensity; // kg/m³ (weight ÷ volume converted to m³)
    private Map<String, Integer> destinationBreakdown; // destination → piece count
    private Map<String, Integer> specialHandlingBreakdown; // DG, AVI, FRA, etc → count
    // Size buckets: counted per piece (each row's pieces count goes into the bucket
    // determined by ONE package's L*W*H). Default thresholds are ULD-oriented for air cargo;
    // frontend may override these thresholds client-side (see sizeThresholdsM3 below).
    private Integer sizeSmall;    // single-piece vol < small threshold
    private Integer sizeMedium;   // small ≤ vol < medium threshold
    private Integer sizeLarge;    // medium ≤ vol < large threshold
    private Integer sizeOversize; // vol ≥ large threshold
    /**
     * Thresholds (m³) used to compute the size buckets above. Keys: "small", "medium", "large".
     * Backend default: { small: 0.5, medium: 1.5, large: 4.0 }. Frontend can recompute the
     * buckets client-side with different thresholds (the package list is already on the wire),
     * and the user's chosen thresholds are persisted with the manifest on save.
     */
    private Map<String, Double> sizeThresholdsM3;
    private Double capacityPercentage; // vs B777F max weight

    public ManifestStatisticsDto() {}

    public Integer getTotalPieces() { return totalPieces; }
    public void setTotalPieces(Integer totalPieces) { this.totalPieces = totalPieces; }

    public Double getTotalWeightKg() { return totalWeightKg; }
    public void setTotalWeightKg(Double totalWeightKg) { this.totalWeightKg = totalWeightKg; }

    public Double getTotalVolumeMm3() { return totalVolumeMm3; }
    public void setTotalVolumeMm3(Double totalVolumeMm3) { this.totalVolumeMm3 = totalVolumeMm3; }

    public Double getAverageDensity() { return averageDensity; }
    public void setAverageDensity(Double averageDensity) { this.averageDensity = averageDensity; }

    public Map<String, Integer> getDestinationBreakdown() { return destinationBreakdown; }
    public void setDestinationBreakdown(Map<String, Integer> destinationBreakdown) { this.destinationBreakdown = destinationBreakdown; }

    public Map<String, Integer> getSpecialHandlingBreakdown() { return specialHandlingBreakdown; }
    public void setSpecialHandlingBreakdown(Map<String, Integer> specialHandlingBreakdown) { this.specialHandlingBreakdown = specialHandlingBreakdown; }

    public Integer getSizeSmall() { return sizeSmall; }
    public void setSizeSmall(Integer sizeSmall) { this.sizeSmall = sizeSmall; }

    public Integer getSizeMedium() { return sizeMedium; }
    public void setSizeMedium(Integer sizeMedium) { this.sizeMedium = sizeMedium; }

    public Integer getSizeLarge() { return sizeLarge; }
    public void setSizeLarge(Integer sizeLarge) { this.sizeLarge = sizeLarge; }

    public Integer getSizeOversize() { return sizeOversize; }
    public void setSizeOversize(Integer sizeOversize) { this.sizeOversize = sizeOversize; }

    public Map<String, Double> getSizeThresholdsM3() { return sizeThresholdsM3; }
    public void setSizeThresholdsM3(Map<String, Double> sizeThresholdsM3) { this.sizeThresholdsM3 = sizeThresholdsM3; }

    public Double getCapacityPercentage() { return capacityPercentage; }
    public void setCapacityPercentage(Double capacityPercentage) { this.capacityPercentage = capacityPercentage; }
}
