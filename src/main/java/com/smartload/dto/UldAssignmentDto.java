package com.smartload.dto;

import com.smartload.entity.AircraftPosition;
import com.smartload.entity.UldAssignment;
import com.smartload.entity.UldType;
import java.util.List;

/**
 * DTO for UldAssignment — one filled ULD at one aircraft position.
 *
 * Includes all data the frontend needs to:
 *   1. Draw the ULD contour in the 3D viewer (contourPointsJson, lengthMm, colorHex).
 *   2. Position it in the scene (armMm, lateralOffsetMm, deck).
 *   3. Draw each package box inside it (placements list).
 *   4. Show stats in the panel (totalWeightKg, utilizationPct).
 *
 * Bounding box (bboxWMm, bboxHMm) is derived from contour max values and
 * pre-computed here so the frontend doesn't need to parse the contour JSON
 * just to compute package offsets.
 */
public class UldAssignmentDto {

    private Long   id;
    private String positionCode;
    private int    armMm;
    private int    lateralOffsetMm;
    private String deck;
    private String uldTypeCode;
    private String uldTypeName;
    private String colorHex;
    private int    lengthMm;
    private String contourPointsJson;
    private int    bboxWMm;   // max x in normalised contour
    private int    bboxHMm;   // max y in normalised contour
    private double  totalWeightKg;
    private double  utilizationPct;
    private int     packageCount;
    /** LOFO loading order: 1 = first to load (aft/deepest), N = last to load (near door). Null if not computed. */
    private Integer loadingOrder;
    /** Dominant destination code of packages in this ULD (for LOFO colour coding). */
    private String  dominantDestination;

    private List<PackagePlacementDto> placements;

    public static UldAssignmentDto from(UldAssignment asgn,
                                        AircraftPosition pos,
                                        UldType uldType,
                                        int bboxW, int bboxH,
                                        List<PackagePlacementDto> placements) {
        UldAssignmentDto d = new UldAssignmentDto();
        d.id                 = asgn.getId();
        d.positionCode       = pos.getPositionCode();
        d.armMm              = pos.getArmMm();
        d.lateralOffsetMm    = pos.getLateralOffsetMm();
        d.deck               = pos.getDeck();
        d.uldTypeCode        = uldType.getCode();
        d.uldTypeName        = uldType.getName();
        d.colorHex           = uldType.getColorHex();
        d.lengthMm           = uldType.getLengthMm();
        d.contourPointsJson  = uldType.getPointsJson();
        d.bboxWMm            = bboxW;
        d.bboxHMm            = bboxH;
        d.totalWeightKg         = asgn.getTotalWeightKg() != null ? asgn.getTotalWeightKg() : 0;
        d.utilizationPct        = asgn.getUtilizationPct() != null ? asgn.getUtilizationPct() : 0;
        d.packageCount          = asgn.getPackageCount() != null ? asgn.getPackageCount() : 0;
        d.loadingOrder          = asgn.getLoadingOrder();
        d.dominantDestination   = asgn.getDominantDestination();
        d.placements            = placements;
        return d;
    }

    public Long    getId()                      { return id; }
    public String  getPositionCode()            { return positionCode; }
    public int     getArmMm()                   { return armMm; }
    public int     getLateralOffsetMm()         { return lateralOffsetMm; }
    public String  getDeck()                    { return deck; }
    public String  getUldTypeCode()             { return uldTypeCode; }
    public String  getUldTypeName()             { return uldTypeName; }
    public String  getColorHex()               { return colorHex; }
    public int     getLengthMm()               { return lengthMm; }
    public String  getContourPointsJson()      { return contourPointsJson; }
    public int     getBboxWMm()                { return bboxWMm; }
    public int     getBboxHMm()                { return bboxHMm; }
    public double  getTotalWeightKg()          { return totalWeightKg; }
    public double  getUtilizationPct()         { return utilizationPct; }
    public int     getPackageCount()           { return packageCount; }
    public Integer getLoadingOrder()           { return loadingOrder; }
    public String  getDominantDestination()    { return dominantDestination; }
    public List<PackagePlacementDto> getPlacements() { return placements; }
}
