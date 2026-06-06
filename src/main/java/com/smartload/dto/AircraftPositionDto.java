package com.smartload.dto;

import com.smartload.entity.AircraftPosition;

/**
 * DTO for AircraftPosition.
 *
 * The frontend converts armMm → Three.js Z, lateralOffsetMm → Three.js X
 * via utils/aircraftCoords.js positionToScene().
 */
public class AircraftPositionDto {

    private Long       id;
    private String     positionCode;
    private int        armMm;
    private int        lateralOffsetMm;
    private String     deck;
    private int        displayOrder;
    private UldTypeDto uldType;

    public static AircraftPositionDto from(AircraftPosition e) {
        AircraftPositionDto d = new AircraftPositionDto();
        d.id              = e.getId();
        d.positionCode    = e.getPositionCode();
        d.armMm           = e.getArmMm();
        d.lateralOffsetMm = e.getLateralOffsetMm();
        d.deck            = e.getDeck();
        d.displayOrder    = e.getDisplayOrder();
        d.uldType         = UldTypeDto.from(e.getUldType());
        return d;
    }

    public Long       getId()              { return id; }
    public String     getPositionCode()    { return positionCode; }
    public int        getArmMm()           { return armMm; }
    public int        getLateralOffsetMm() { return lateralOffsetMm; }
    public String     getDeck()            { return deck; }
    public int        getDisplayOrder()    { return displayOrder; }
    public UldTypeDto getUldType()         { return uldType; }
}
