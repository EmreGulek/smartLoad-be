package com.smartload.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Package — a single row from the validated manifest, persisted in its own queryable table.
 *
 * Why this exists (Faz 1 acceptance criterion + Faz 3 prerequisite):
 *   • The bin-packing algorithm needs to load packages by ID, filter by destination/shipment/DG,
 *     and update assignment fields (ULD, position) — none of that is possible if packages live
 *     only inside the manifest's `validation_result` JSONB blob.
 *   • The `validation_result` JSONB stays as an immutable audit snapshot of the original Excel
 *     import; the `packages` table holds the live working set the rest of the pipeline edits.
 *
 * Naming convention:
 *   • Dimensions in millimetres (Double); see ADR — F batch + Manifest entity.
 *   • Optional fields default to null (Hibernate will create nullable columns under
 *     ddl-auto=update, see ADR-0011).
 */
@Entity
@Table(name = "packages")
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Parent manifest — cascade ensures these rows go away with the manifest. */
    @Column(name = "manifest_id", nullable = false)
    private String manifestId;

    /** Parent shipment; nullable because not every Excel format has a shipment column. */
    @Column(name = "shipment_id")
    private String shipmentId;

    /** Original row number in the imported Excel (1-based, header row counted as 1). */
    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    // === 14-field canonical Package schema (see Package wiki entity, ADR-0009) ===

    @Column(name = "pieces", nullable = false)
    private Integer pieces;

    @Column(name = "length_mm", nullable = false)
    private Double lengthMm;

    @Column(name = "width_mm", nullable = false)
    private Double widthMm;

    @Column(name = "height_mm", nullable = false)
    private Double heightMm;

    @Column(name = "weight_per_piece_kg")
    private Double weightPerPieceKg;

    @Column(name = "gross_weight_kg", nullable = false)
    private Double grossWeightKg;

    @Column(name = "stackable")
    private Boolean stackable;

    @Column(name = "rotatable")
    private Boolean rotatable;

    @Column(name = "toploadable")
    private Boolean toploadable;

    @Column(name = "can_mix")
    private Boolean canMix;

    @Column(name = "uld_preference")
    private String uldPreference;

    @Column(name = "destination_code", nullable = false)
    private String destinationCode;

    @Column(name = "special_handling")
    private String specialHandling;

    @Column(name = "dg_class")
    private String dgClass;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Package() {
        this.createdAt = LocalDateTime.now();
    }

    // === Getters / setters ===
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getManifestId() { return manifestId; }
    public void setManifestId(String manifestId) { this.manifestId = manifestId; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }

    public Integer getPieces() { return pieces; }
    public void setPieces(Integer pieces) { this.pieces = pieces; }

    public Double getLengthMm() { return lengthMm; }
    public void setLengthMm(Double lengthMm) { this.lengthMm = lengthMm; }

    public Double getWidthMm() { return widthMm; }
    public void setWidthMm(Double widthMm) { this.widthMm = widthMm; }

    public Double getHeightMm() { return heightMm; }
    public void setHeightMm(Double heightMm) { this.heightMm = heightMm; }

    public Double getWeightPerPieceKg() { return weightPerPieceKg; }
    public void setWeightPerPieceKg(Double weightPerPieceKg) { this.weightPerPieceKg = weightPerPieceKg; }

    public Double getGrossWeightKg() { return grossWeightKg; }
    public void setGrossWeightKg(Double grossWeightKg) { this.grossWeightKg = grossWeightKg; }

    public Boolean getStackable() { return stackable; }
    public void setStackable(Boolean stackable) { this.stackable = stackable; }

    public Boolean getRotatable() { return rotatable; }
    public void setRotatable(Boolean rotatable) { this.rotatable = rotatable; }

    public Boolean getToploadable() { return toploadable; }
    public void setToploadable(Boolean toploadable) { this.toploadable = toploadable; }

    public Boolean getCanMix() { return canMix; }
    public void setCanMix(Boolean canMix) { this.canMix = canMix; }

    public String getUldPreference() { return uldPreference; }
    public void setUldPreference(String uldPreference) { this.uldPreference = uldPreference; }

    public String getDestinationCode() { return destinationCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }

    public String getSpecialHandling() { return specialHandling; }
    public void setSpecialHandling(String specialHandling) { this.specialHandling = specialHandling; }

    public String getDgClass() { return dgClass; }
    public void setDgClass(String dgClass) { this.dgClass = dgClass; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
