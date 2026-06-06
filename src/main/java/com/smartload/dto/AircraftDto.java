package com.smartload.dto;

import com.smartload.entity.Aircraft;

/**
 * DTO for Aircraft — sent alongside positions so the frontend knows
 * fuselage dimensions and floor heights without hardcoding them.
 */
public class AircraftDto {

    private Long   id;
    private String icaoCode;
    private String name;
    private int    fuselageRadiusMm;
    private int    floorMainDeckMm;
    private int    floorLowerDeckMm;
    private int    maxPayloadKg;

    public static AircraftDto from(Aircraft e) {
        AircraftDto d = new AircraftDto();
        d.id                = e.getId();
        d.icaoCode          = e.getIcaoCode();
        d.name              = e.getName();
        d.fuselageRadiusMm  = e.getFuselageRadiusMm();
        d.floorMainDeckMm   = e.getFloorMainDeckMm();
        d.floorLowerDeckMm  = e.getFloorLowerDeckMm();
        d.maxPayloadKg      = e.getMaxPayloadKg() != null ? e.getMaxPayloadKg() : 0;
        return d;
    }

    public Long   getId()               { return id; }
    public String getIcaoCode()         { return icaoCode; }
    public String getName()             { return name; }
    public int    getFuselageRadiusMm() { return fuselageRadiusMm; }
    public int    getFloorMainDeckMm()  { return floorMainDeckMm; }
    public int    getFloorLowerDeckMm() { return floorLowerDeckMm; }
    public int    getMaxPayloadKg()     { return maxPayloadKg; }
}
