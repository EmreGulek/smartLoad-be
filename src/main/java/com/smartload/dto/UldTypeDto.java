package com.smartload.dto;

import com.smartload.entity.UldType;

/**
 * DTO for UldType — sent to the frontend as part of AircraftPositionDto.
 */
public class UldTypeDto {

    private Long   id;
    private String code;
    private String name;
    private String contourLabel;
    private String colorHex;
    private int    lengthMm;
    private String pointsJson;

    public static UldTypeDto from(UldType e) {
        UldTypeDto d = new UldTypeDto();
        d.id           = e.getId();
        d.code         = e.getCode();
        d.name         = e.getName();
        d.contourLabel = e.getContourLabel();
        d.colorHex     = e.getColorHex();
        d.lengthMm     = e.getLengthMm();
        d.pointsJson   = e.getPointsJson();
        return d;
    }

    public Long   getId()           { return id; }
    public String getCode()         { return code; }
    public String getName()         { return name; }
    public String getContourLabel() { return contourLabel; }
    public String getColorHex()     { return colorHex; }
    public int    getLengthMm()     { return lengthMm; }
    public String getPointsJson()   { return pointsJson; }
}
