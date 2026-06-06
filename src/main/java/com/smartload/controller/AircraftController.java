package com.smartload.controller;

import com.smartload.dto.AircraftDto;
import com.smartload.dto.AircraftPositionDto;
import com.smartload.entity.Aircraft;
import com.smartload.entity.AircraftPosition;
import com.smartload.repository.AircraftPositionRepository;
import com.smartload.repository.AircraftRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * AircraftController — exposes aircraft and position configuration to the frontend.
 *
 * All endpoints are public (no JWT required). Aircraft geometry is not sensitive data;
 * the 3D viewer must load it before the user is logged in (future: guest mode).
 *
 * Endpoints:
 *   GET /api/aircraft              — list all aircraft
 *   GET /api/aircraft/{id}         — single aircraft details
 *   GET /api/aircraft/{id}/positions — all active positions for an aircraft
 */
@RestController
@RequestMapping("/api/aircraft")
public class AircraftController {

    private final AircraftRepository         aircraftRepo;
    private final AircraftPositionRepository positionRepo;

    public AircraftController(AircraftRepository aircraftRepo,
                               AircraftPositionRepository positionRepo) {
        this.aircraftRepo = aircraftRepo;
        this.positionRepo = positionRepo;
    }

    /** List all aircraft (used by future aircraft selector). */
    @GetMapping
    public List<AircraftDto> listAircraft() {
        return aircraftRepo.findAll().stream()
            .map(AircraftDto::from)
            .toList();
    }

    /** Get single aircraft details (fuselage dimensions, floor heights). */
    @GetMapping("/{id}")
    public ResponseEntity<AircraftDto> getAircraft(@PathVariable Long id) {
        return aircraftRepo.findById(id)
            .map(AircraftDto::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all active ULD positions for an aircraft, ordered front-to-back.
     * Used by B777FViewer to build the DB-driven cargo layout.
     */
    @GetMapping("/{id}/positions")
    public ResponseEntity<List<AircraftPositionDto>> getPositions(@PathVariable Long id) {
        Optional<Aircraft> aircraft = aircraftRepo.findById(id);
        if (aircraft.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<AircraftPositionDto> positions =
            positionRepo.findByAircraftIdAndIsActiveTrueOrderByDisplayOrderAsc(id)
                .stream()
                .map(AircraftPositionDto::from)
                .toList();
        return ResponseEntity.ok(positions);
    }
}
