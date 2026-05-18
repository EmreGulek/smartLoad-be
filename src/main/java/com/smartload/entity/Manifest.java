package com.smartload.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Manifest entity — saved cargo manifest with validation results and statistics.
 */
@Entity
@Table(name = "manifests")
public class Manifest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "status", nullable = false)
    private String status; // DRAFT, VALIDATED, SAVED

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_pieces")
    private Integer totalPieces;

    @Column(name = "total_weight_kg")
    private Double totalWeightKg;

    @Column(name = "total_volume_mm3")
    private Double totalVolumeMm3;

    @Column(name = "capacity_percentage")
    private Double capacityPercentage;

    /** Immutable import grid: { sheetName, headers, rows }. See docs/sql/002-manifests.sql. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_grid", columnDefinition = "jsonb")
    private String sourceGrid;

    /** Frozen canonical-field → column-index map from import. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_mapping", columnDefinition = "jsonb")
    private String columnMapping;

    /** Validate-time snapshot (issues, statistics, packages at import). Not edited after save. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_result", columnDefinition = "jsonb")
    private String validationResult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Manifest() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "DRAFT";
    }

    // Getters & Setters
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

    public String getSourceGrid() { return sourceGrid; }
    public void setSourceGrid(String sourceGrid) { this.sourceGrid = sourceGrid; }

    public String getColumnMapping() { return columnMapping; }
    public void setColumnMapping(String columnMapping) { this.columnMapping = columnMapping; }

    public String getValidationResult() { return validationResult; }
    public void setValidationResult(String validationResult) { this.validationResult = validationResult; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
