package com.smartload.controller;

import com.smartload.dto.*;
import com.smartload.entity.*;
import com.smartload.entity.Package;
import com.smartload.repository.*;
import com.smartload.service.BinPackingService;
import com.smartload.service.PdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LoadPlanController — bin-packing API.
 * <p>
 * Endpoints:
 * POST /api/load-plans/optimize          — run optimisation, return plan id
 * GET  /api/load-plans/{id}              — full plan with all placements
 * GET  /api/load-plans/manifest/{mid}    — list of plans for a manifest
 */
@RestController
@RequestMapping("/api/load-plans")
public class LoadPlanController {

    private final BinPackingService binPackingService;
    private final LoadPlanRepository loadPlanRepo;
    private final UldAssignmentRepository assignmentRepo;
    private final PackagePlacementRepository placementRepo;
    private final AircraftPositionRepository positionRepo;
    private final UldTypeRepository uldTypeRepo;
    private final PackageRepository packageRepo;
    private final AircraftRepository aircraftRepo;
    private final ManifestRepository manifestRepo;
    private final PdfReportService pdfReportService;

    public LoadPlanController(BinPackingService binPackingService,
                              LoadPlanRepository loadPlanRepo,
                              UldAssignmentRepository assignmentRepo,
                              PackagePlacementRepository placementRepo,
                              AircraftPositionRepository positionRepo,
                              UldTypeRepository uldTypeRepo,
                              PackageRepository packageRepo,
                              AircraftRepository aircraftRepo,
                              ManifestRepository manifestRepo,
                              PdfReportService pdfReportService) {
        this.binPackingService = binPackingService;
        this.loadPlanRepo = loadPlanRepo;
        this.assignmentRepo = assignmentRepo;
        this.placementRepo = placementRepo;
        this.positionRepo = positionRepo;
        this.uldTypeRepo = uldTypeRepo;
        this.packageRepo = packageRepo;
        this.aircraftRepo = aircraftRepo;
        this.manifestRepo = manifestRepo;
        this.pdfReportService = pdfReportService;
    }

    /**
     * Run the bin-packing algorithm for a manifest.
     * Returns the new LoadPlan id so the client can fetch the full result.
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimize(@RequestBody OptimizeRequest req) {
        if (req.getManifestId() == null || req.getManifestId().isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "manifestId is required"));
        }
        Long planId = binPackingService.optimize(req.getManifestId(), req.getAircraftId(), req.getFlightStops());
        return ResponseEntity.ok(Map.of("loadPlanId", planId));
    }

    /**
     * Return the full load plan with all ULD assignments and package placements.
     * This is the main payload for the LoadPlanPage and 3D viewer.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LoadPlanResultDto> getLoadPlan(@PathVariable Long id) {
        LoadPlanResultDto dto = assembleResult(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    /**
     * Assemble the full LoadPlanResultDto for a plan id, or null if not found.
     * Shared by the JSON endpoint and the PDF report endpoints (Faz 5).
     */
    private LoadPlanResultDto assembleResult(Long id) {
        Optional<LoadPlan> planOpt = loadPlanRepo.findById(id);
        if (planOpt.isEmpty()) return null;

        LoadPlan plan = planOpt.get();

        // Build UldAssignmentDto list with nested placements
        List<UldAssignment> assignments = assignmentRepo.findByLoadPlanId(id);

        // Pre-fetch position and ULD type maps for efficiency
        Map<Long, AircraftPosition> posMap = positionRepo.findAll().stream()
            .collect(Collectors.toMap(AircraftPosition::getId, p -> p));
        Map<Long, UldType> uldTypeMap = uldTypeRepo.findAll().stream()
            .collect(Collectors.toMap(UldType::getId, u -> u));
        // Pre-fetch packages used in this plan for enrichment
        List<String> packageIds = placementRepo.findAll().stream()
            .filter(pp -> assignments.stream()
                .anyMatch(a -> a.getId().equals(pp.getUldAssignmentId())))
            .map(PackagePlacement::getPackageId)
            .distinct()
            .collect(Collectors.toList());
        Map<String, Package> pkgMap = packageRepo.findAllById(packageIds).stream()
            .collect(Collectors.toMap(Package::getId, p -> p));

        // Build assignment map keyed by position id for quick lookup
        Map<Long, UldAssignment> asgnByPosition = assignments.stream()
            .collect(Collectors.toMap(UldAssignment::getAircraftPositionId, a -> a));

        List<UldAssignmentDto> assignmentDtos = new ArrayList<>();

        // Iterate ALL active aircraft positions so empty slots also appear in the list
        List<AircraftPosition> allPositions =
            positionRepo.findByAircraftIdAndIsActiveTrueOrderByDisplayOrderAsc(plan.getAircraftId());

        for (AircraftPosition pos : allPositions) {
            UldType uldType = uldTypeMap.get(pos.getUldType().getId());
            if (uldType == null) continue;

            int[][] contour = BinPackingService.parseContour(uldType.getPointsJson());
            int bboxW = 0, bboxH = 0;
            for (int[] pt : contour) {
                bboxW = Math.max(bboxW, pt[0]);
                bboxH = Math.max(bboxH, pt[1]);
            }

            UldAssignment asgn = asgnByPosition.get(pos.getId());

            List<PackagePlacementDto> placementDtos = new ArrayList<>();
            if (asgn != null) {
                placementDtos = placementRepo
                    .findByUldAssignmentId(asgn.getId())
                    .stream()
                    .map(pp -> {
                        PackagePlacementDto dto = PackagePlacementDto.from(pp);
                        Package pkg = pkgMap.get(pp.getPackageId());
                        if (pkg != null) {
                            dto.setDestinationCode(pkg.getDestinationCode());
                            dto.setSpecialHandling(pkg.getSpecialHandling());
                            dto.setDgClass(pkg.getDgClass());
                            dto.setGrossWeightKg(pkg.getGrossWeightKg() != null ? pkg.getGrossWeightKg() : 0);
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());
            }

            // For empty positions, create a synthetic assignment DTO with zeros
            if (asgn == null) {
                UldAssignment empty = new UldAssignment(
                    plan.getId(), pos.getId(), uldType.getId(), 0.0, 0.0, 0);
                assignmentDtos.add(UldAssignmentDto.from(empty, pos, uldType, bboxW, bboxH, placementDtos));
            } else {
                assignmentDtos.add(UldAssignmentDto.from(asgn, pos, uldType, bboxW, bboxH, placementDtos));
            }
        }

        return LoadPlanResultDto.from(plan, assignmentDtos);
    }

    // ── Faz 5: PDF raporları ─────────────────────────────────────────────────

    /**
     * Generate the Load Instruction Report (LIR) PDF for a plan.
     * Opens inline in the browser; printable / signable by the loadmaster.
     */
    @GetMapping("/{id}/lir.pdf")
    public ResponseEntity<byte[]> getLir(@PathVariable Long id) {
        LoadPlanResultDto dto = assembleResult(id);
        if (dto == null) return ResponseEntity.notFound().build();
        Aircraft aircraft = aircraftRepo.findById(dto.getAircraftId()).orElse(null);
        Manifest manifest = manifestRepo.findById(dto.getManifestId()).orElse(null);
        byte[] pdf = pdfReportService.generateLir(dto, aircraft, manifest);
        return pdfResponse(pdf, "LIR-plan-" + id + ".pdf");
    }

    /**
     * Generate the Load Sheet (weight & balance) PDF for a plan.
     */
    @GetMapping("/{id}/load-sheet.pdf")
    public ResponseEntity<byte[]> getLoadSheet(@PathVariable Long id) {
        LoadPlanResultDto dto = assembleResult(id);
        if (dto == null) return ResponseEntity.notFound().build();
        Aircraft aircraft = aircraftRepo.findById(dto.getAircraftId()).orElse(null);
        Manifest manifest = manifestRepo.findById(dto.getManifestId()).orElse(null);
        byte[] pdf = pdfReportService.generateLoadSheet(dto, aircraft, manifest);
        return pdfResponse(pdf, "LoadSheet-plan-" + id + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .body(pdf);
    }

    /**
     * List all load plans for a manifest (summary only, no placements).
     */
    @GetMapping("/manifest/{manifestId}")
    public List<Map<String, Object>> listForManifest(@PathVariable String manifestId) {
        return loadPlanRepo.findByManifestIdOrderByCreatedAtDesc(manifestId).stream()
            .map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "status", p.getStatus(),
                "algorithm", p.getAlgorithm() != null ? p.getAlgorithm() : "",
                "utilizationPct", p.getUtilizationPct() != null ? p.getUtilizationPct() : 0,
                "placedPackages", p.getPlacedPackages() != null ? p.getPlacedPackages() : 0,
                "totalPackages", p.getTotalPackages() != null ? p.getTotalPackages() : 0,
                "createdAt", p.getCreatedAt().toString()
            ))
            .collect(Collectors.toList());
    }
}
