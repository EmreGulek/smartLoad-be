package com.smartload.dto;

import com.smartload.entity.PackagePlacement;

/**
 * DTO for PackagePlacement — one package's 3D position inside a ULD container.
 *
 * xMm, yMm, zMm: bottom-front-left corner in ULD-local coordinates (mm, origin = 0,0,0).
 * applied*Mm: dimensions after rotation.
 */
public class PackagePlacementDto {

    private Long   id;
    private String packageId;
    private int    xMm;
    private int    yMm;
    private int    zMm;
    private int    appliedWidthMm;
    private int    appliedHeightMm;
    private int    appliedDepthMm;
    private int    rotationIndex;

    // Extra fields fetched from Package for frontend colour/label
    private String destinationCode;
    private String specialHandling;
    private String dgClass;
    private double grossWeightKg;

    public static PackagePlacementDto from(PackagePlacement e) {
        PackagePlacementDto d = new PackagePlacementDto();
        d.id             = e.getId();
        d.packageId      = e.getPackageId();
        d.xMm            = e.getXMm();
        d.yMm            = e.getYMm();
        d.zMm            = e.getZMm();
        d.appliedWidthMm  = e.getAppliedWidthMm();
        d.appliedHeightMm = e.getAppliedHeightMm();
        d.appliedDepthMm  = e.getAppliedDepthMm();
        d.rotationIndex  = e.getRotationIndex();
        return d;
    }

    // Setters for package-level enrichment
    public void setDestinationCode(String v) { this.destinationCode = v; }
    public void setSpecialHandling(String v) { this.specialHandling = v; }
    public void setDgClass(String v)         { this.dgClass = v; }
    public void setGrossWeightKg(double v)   { this.grossWeightKg = v; }

    public Long   getId()               { return id; }
    public String getPackageId()        { return packageId; }
    public int    getXMm()              { return xMm; }
    public int    getYMm()              { return yMm; }
    public int    getZMm()              { return zMm; }
    public int    getAppliedWidthMm()   { return appliedWidthMm; }
    public int    getAppliedHeightMm()  { return appliedHeightMm; }
    public int    getAppliedDepthMm()   { return appliedDepthMm; }
    public int    getRotationIndex()    { return rotationIndex; }
    public String getDestinationCode()  { return destinationCode; }
    public String getSpecialHandling()  { return specialHandling; }
    public String getDgClass()          { return dgClass; }
    public double getGrossWeightKg()    { return grossWeightKg; }
}
