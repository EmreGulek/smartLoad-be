package com.smartload.controller;

import com.smartload.dto.*;
import com.smartload.entity.Manifest;
import com.smartload.entity.Package;
import com.smartload.service.ManifestService;
import com.smartload.service.ManifestValidationService;
import com.smartload.service.PackageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manifests")
public class ManifestController {

    @Autowired
    private ManifestValidationService validationService;

    @Autowired
    private ManifestService manifestService;

    @Autowired
    private PackageService packageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/validate-and-preview")
    public ResponseEntity<ManifestValidationResponse> validateAndPreview(
            @RequestBody ManifestValidationRequest request) {
        ManifestValidationResponse response = validationService.validate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveManifest(@RequestBody Map<String, Object> payload) {
        try {
            ManifestSaveRequest request = objectMapper.convertValue(payload, ManifestSaveRequest.class);
            Manifest savedManifest = manifestService.saveManifest(request);

            Map<String, Object> result = new HashMap<>();
            result.put("id", savedManifest.getId());
            result.put("message", "Manifest saved successfully");
            result.put("status", savedManifest.getStatus());
            result.put("fileName", savedManifest.getFileName());
            result.put("totalPieces", savedManifest.getTotalPieces());
            result.put("totalWeightKg", savedManifest.getTotalWeightKg());
            result.put("savedAt", savedManifest.getCreatedAt());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** Summary list — no source_grid, column_mapping, or validation_result. */
    @GetMapping("/list")
    public ResponseEntity<List<ManifestSummaryDto>> listManifests() {
        return ResponseEntity.ok(manifestService.listManifestSummaries());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "OK");
        result.put("service", "ManifestValidationService");
        result.put("timestamp", System.currentTimeMillis() + "");
        return ResponseEntity.ok(result);
    }

    /** Immutable Excel grid + column mapping. */
    @GetMapping("/{id}/source")
    public ResponseEntity<?> getManifestSource(@PathVariable String id) {
        return manifestService.getManifestSource(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Working-set packages for a manifest (editable). */
    @GetMapping("/{id}/packages")
    public ResponseEntity<List<Package>> listPackages(@PathVariable String id) {
        if (manifestService.getManifestEntity(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(packageService.listByManifest(id));
    }

    @PatchMapping("/{manifestId}/packages/{packageId}")
    public ResponseEntity<?> updatePackage(
            @PathVariable String manifestId,
            @PathVariable String packageId,
            @RequestBody PackageUpdateRequest update) {
        return packageService.updatePackage(manifestId, packageId, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Metadata only — no heavy JSON blobs. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getManifest(@PathVariable String id) {
        return manifestService.getManifestSummary(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteManifest(@PathVariable String id) {
        if (manifestService.getManifestEntity(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            manifestService.deleteManifest(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
