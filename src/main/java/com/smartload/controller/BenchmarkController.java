package com.smartload.controller;

import com.smartload.dto.BenchmarkRowDto;
import com.smartload.dto.OptimizeRequest;
import com.smartload.service.BenchmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BenchmarkController — runs the algorithm comparison for the thesis.
 *
 * POST /api/benchmark/run  body: { manifestId, aircraftId?, flightStops? }
 *   → returns one metric row per algorithm (FFD / EP-V1 / EP-V2).
 */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @PostMapping("/run")
    public ResponseEntity<List<BenchmarkRowDto>> run(@RequestBody OptimizeRequest req) {
        if (req.getManifestId() == null || req.getManifestId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
            benchmarkService.run(req.getManifestId(), req.getAircraftId(), req.getFlightStops()));
    }
}
