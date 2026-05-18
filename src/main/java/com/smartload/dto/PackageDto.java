package com.smartload.dto;

/**
 * Validated package (14 fields canonical schema).
 *
 * Dimensions are stored in MILLIMETRES (mm). Excel manifests typically arrive in centimetres;
 * the conversion happens once at parse time in ManifestValidationService.parseAndValidateRow.
 * Persisting mm matches the CG calculation needs in Faz 4 and avoids float drift from repeated
 * unit conversions during the bin-packing pipeline.
 */
public class PackageDto {
    private Integer pieces;
    private Double lengthMm;
    private Double widthMm;
    private Double heightMm;
    private Double weightPerPieceKg;
    private Double grossWeightKg;
    private Boolean stackable;
    private Boolean rotatable;
    private Boolean toploadable;
    private Boolean canMix;
    private String uldPreference;
    private String destinationCode;
    private String specialHandling;
    private String dgClass;
    /** 1-based Excel row number; header = row 1; first data row = 2. */
    private Integer sourceRowNumber;

    public PackageDto() {}

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

    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }
}
