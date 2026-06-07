package com.smartload.dto;

import java.util.List;

/**
 * Request body for POST /api/load-plans/optimize.
 *
 * flightStops: ordered list of destination codes in flight sequence.
 *   Example: ["IST", "FRA", "LHR"]  →  IST = first stop, LHR = last stop.
 *   LOFO rule: LHR cargo loaded first (deepest/aft), IST cargo loaded last (near door).
 *   If null/empty, loading order is not computed.
 */
public class OptimizeRequest {
    private String       manifestId;
    private Long         aircraftId;   // default: 1 (B777F)
    private List<String> flightStops;  // ordered destination codes, first stop first
    /** Algorithm choice: "FFD" | "V1" (CG-blind) | "V2" (CG-aware). Default "V2". */
    private String       algorithm;

    public String       getManifestId()             { return manifestId; }
    public void         setManifestId(String v)     { this.manifestId = v; }
    public Long         getAircraftId()             { return aircraftId != null ? aircraftId : 1L; }
    public void         setAircraftId(Long v)       { this.aircraftId = v; }
    public List<String> getFlightStops()            { return flightStops; }
    public void         setFlightStops(List<String> v) { this.flightStops = v; }
    public String       getAlgorithm()              { return algorithm != null ? algorithm : "V2"; }
    public void         setAlgorithm(String v)      { this.algorithm = v; }
}
