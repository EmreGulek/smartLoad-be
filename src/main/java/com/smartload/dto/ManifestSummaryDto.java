package com.smartload.dto;

import java.time.LocalDateTime;

/** List/detail metadata — excludes source_grid, column_mapping, validation_result. */
public class ManifestSummaryDto {
    private String id;
    private String status;
    private String fileName;
    private Integer totalPieces;
    private Double totalWeightKg;
    private Double totalVolumeMm3;
    private Double capacityPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasSourceGrid;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Integer getTotalPieces() { return totalPieces; }
    public void setTotalPieces(Integer totalPieces) { this.totalPieces = totalPieces; }

    public Double getTotalWeightKg() { return totalWeightKg; }
    public void setTotalWeightKg(Double totalWeightKg) { this.totalWeightKg = totalWeightKg; }

    public Double getTotalVolumeMm3() { return totalVolumeMm3; }
    public void setTotalVolumeMm3(Double totalVolumeMm3) { this.totalVolumeMm3 = totalVolumeMm3; }

    public Double getCapacityPercentage() { return capacityPercentage; }
    public void setCapacityPercentage(Double capacityPercentage) { this.capacityPercentage = capacityPercentage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isHasSourceGrid() { return hasSourceGrid; }
    public void setHasSourceGrid(boolean hasSourceGrid) { this.hasSourceGrid = hasSourceGrid; }
}
