package com.smartload.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 0 smoke-test endpoint.
 * GET /api/health  →  { status: "OK", phase: "0", timestamp: ... }
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "OK",
            "service", "smartload-backend",
            "phase", "0",
            "timestamp", Instant.now().toString()
        );
    }
}
