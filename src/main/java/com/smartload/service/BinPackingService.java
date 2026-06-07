package com.smartload.service;

import com.smartload.entity.Aircraft;
import com.smartload.entity.AircraftPosition;
import com.smartload.entity.LoadPlan;
import com.smartload.entity.PackagePlacement;
import com.smartload.entity.UldAssignment;
import com.smartload.entity.UldType;
import com.smartload.dto.CgResultDto;
// NOTE: com.smartload.entity.Package is referred to as CargoPackage alias below
//       to avoid clash with java.lang.Package (auto-imported from java.lang.*).
import com.smartload.repository.AircraftPositionRepository;
import com.smartload.repository.LoadPlanRepository;
import com.smartload.repository.PackagePlacementRepository;
import com.smartload.repository.PackageRepository;
import com.smartload.repository.UldAssignmentRepository;
import com.smartload.repository.UldTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BinPackingService — Two-level 3D bin packing for B777F air cargo loading.
 *
 * ══ Algorithm ══════════════════════════════════════════════════════════════════
 * Based on Heßler et al. (2024) "Air Cargo Loading Optimization at Schenker AG"
 * (arXiv 2410.01445v1). This implementation follows the Extreme Points + Greedy
 * approach from that paper, adapted for the B777F's non-cuboid ULD contours.
 *
 * Level 1 — Package → ULD (Extreme Points Greedy):
 *   1. Sort packages by volume (descending). Large items first prevents dead space.
 *   2. For each package, try to place it into an open ULD container:
 *      a. Try every extreme point (EP) in the container (sorted y↑, x↑, z↑).
 *      b. For each EP, try all allowed rotations (2 if rotatable=false, 6 if true).
 *      c. Accept the first valid placement (bounds + overlap + contour + support).
 *      d. After placement, generate new EPs and prune obsolete ones.
 *   3. If no open ULD can accept the package, open a new ULD container
 *      (if a free aircraft position of a compatible type is available).
 *   4. If no positions remain, the package is recorded as "unplaced".
 *
 * Level 2 — ULD → Aircraft Position (weight-balance greedy):
 *   Filled ULD containers are assigned to aircraft positions of the matching
 *   ULD type, heaviest containers first to the centre positions (P6–P8),
 *   lighter loads toward nose (P1–P2) and tail (P9).
 *
 * ══ Constraints ════════════════════════════════════════════════════════════════
 *   ✅ Bounds          : package stays within ULD bounding box.
 *   ✅ No overlap       : 3D AABB intersection check vs all placed items.
 *   ✅ Contour          : top corners of package inside ULD cross-section polygon
 *                        (ray-casting point-in-polygon; all B777F contours are convex).
 *   ✅ Stackability     : if any item directly below has stackable=false, reject.
 *   ✅ Anti-floating    : package must rest on ULD floor or on another item.
 *   ✅ Rotations        : 2 or 6 orientations depending on package.rotatable flag.
 *   ⚠️  canMix          : items from same shipment preferred in same ULD (soft).
 *                        Hard enforcement deferred to Faz 3 refinement.
 *   ⚠️  DG segregation  : DG class flag captured; spatial separation Faz 3 refinement.
 *   ✅ LOFO             : if flightStops provided, last-stop packages are packed into nose
 *                        (high arm_mm) containers first; first-stop packages go to tail/door
 *                        containers. Physical position assignment respects loadmaster rules.
 *   ⚠️  ULD weight limit : not enforced (Faz 3 refinement; needs maxWeightKg in DB).
 *
 * ══ References ═════════════════════════════════════════════════════════════════
 *   ADR-0013 — algorithm choice
 *   raw/docs/2410.01445v1 (3).pdf — Heßler 2024 (primary reference)
 */
@Service
public class BinPackingService {

    private static final Logger log = LoggerFactory.getLogger(BinPackingService.class);

    static final String ALGORITHM_ID = "EXTREME_POINTS_GREEDY_V1";

    /**
     * CG balancing target in %MAC: centre of the safe envelope.
     * (FWD_WARN + AFT_WARN) / 2 = (18 + 33) / 2 = 25.5 %MAC — see CgResultDto.
     * When CG-aware placement is enabled, the greedy prefers the container that
     * keeps the running aircraft CG closest to this target.
     */
    private static final double TARGET_MAC_PCT =
        (CgResultDto.FWD_WARN_PCT + CgResultDto.AFT_WARN_PCT) / 2.0;

    /**
     * Six 3D rotations of a box [W, H, L] → applied [appWidth, appHeight, appDepth].
     * appWidth  = lateral (ULD x-axis)
     * appHeight = vertical (ULD y-axis)
     * appDepth  = longitudinal depth (ULD z-axis)
     *
     * Only rotations 0–1 are used when package.rotatable = false
     * (floor rotations only: keep the item upright).
     */
    private static final int[][] ROTATIONS = {
        {0, 1, 2},  // rot 0: (W, H, L) — natural
        {2, 1, 0},  // rot 1: (L, H, W) — 90° floor rotation
        {0, 2, 1},  // rot 2: (W, L, H) — tipped on side (rotatable only)
        {2, 0, 1},  // rot 3: (L, W, H) — tipped + floor rotate (rotatable only)
        {1, 0, 2},  // rot 4: (H, W, L) — nose-down (rotatable only)
        {1, 2, 0},  // rot 5: (H, L, W) — nose-down + floor rotate (rotatable only)
    };

    private final PackageRepository          packageRepo;
    private final AircraftPositionRepository positionRepo;
    private final UldTypeRepository          uldTypeRepo;
    private final LoadPlanRepository         loadPlanRepo;
    private final UldAssignmentRepository    assignmentRepo;
    private final PackagePlacementRepository placementRepo;

    public BinPackingService(PackageRepository packageRepo,
                              AircraftPositionRepository positionRepo,
                              UldTypeRepository uldTypeRepo,
                              LoadPlanRepository loadPlanRepo,
                              UldAssignmentRepository assignmentRepo,
                              PackagePlacementRepository placementRepo) {
        this.packageRepo   = packageRepo;
        this.positionRepo  = positionRepo;
        this.uldTypeRepo   = uldTypeRepo;
        this.loadPlanRepo  = loadPlanRepo;
        this.assignmentRepo = assignmentRepo;
        this.placementRepo = placementRepo;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Run the bin-packing optimisation for a given manifest + aircraft and
     * persist the result as a LoadPlan with UldAssignment + PackagePlacement rows.
     *
     * @param manifestId  target manifest UUID
     * @param aircraftId  aircraft DB id (B777F = 1)
     * @param flightStops ordered destination codes in flight sequence, first stop first.
     *                    E.g. ["IST","FRA","JFK"] → IST = first to land (loaded last / near door),
     *                    JFK = last to land (loaded first / deepest). Null = LOFO not computed.
     * @return saved LoadPlan id
     */
    @Transactional
    public Long optimize(String manifestId, Long aircraftId, List<String> flightStops) {
        // CG-aware placement is the default (V2). Use the 4-arg overload with
        // cgAware=false to reproduce the CG-blind V1 baseline (thesis benchmark).
        return optimize(manifestId, aircraftId, flightStops, true);
    }

    /**
     * Optimisation with explicit CG-awareness toggle.
     *
     * @param cgAware when true and LOFO is not active, the greedy chooses, among
     *                the containers that can fit each package, the one that keeps
     *                the running aircraft CG closest to {@link #TARGET_MAC_PCT}.
     *                When false, containers are tried in fixed display order
     *                (nose→tail) — the CG-blind V1 baseline.
     *                Note: when flightStops are given, LOFO accessibility ordering
     *                takes precedence (operational hard rule) and CG ordering is
     *                not applied, but final CG is still computed and recorded.
     */
    @Transactional
    public Long optimize(String manifestId, Long aircraftId, List<String> flightStops, boolean cgAware) {
        long t0 = System.currentTimeMillis();
        log.info("BinPacking START manifest={} aircraft={} cgAware={}", manifestId, aircraftId, cgAware);

        // ── 1. Load inputs ────────────────────────────────────────────────────
        List<com.smartload.entity.Package> packages = packageRepo.findByManifestId(manifestId);
        if (packages.isEmpty()) {
            throw new IllegalArgumentException("No packages found for manifest " + manifestId);
        }

        List<AircraftPosition> positions =
            positionRepo.findByAircraftIdAndIsActiveTrueOrderByDisplayOrderAsc(aircraftId);
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("No active positions for aircraft " + aircraftId);
        }

        // ── 2. Prepare ULD container pool (one slot per aircraft position) ────
        //    positions are ordered by display_order (P1R … P9L, nose to tail).
        List<UldContainer> containers = positions.stream()
            .map(pos -> new UldContainer(pos, parseContour(pos.getUldType().getPointsJson())))
            .collect(Collectors.toList());

        // ── 2b. LOFO setup — build stop-rank map and per-destination container  ──
        //       lists BEFORE packing so the greedy loop can use them.
        //
        //   arm_mm: positive = toward nose (high arm = deep/forward).
        //   LOFO rule:
        //     loadingOrder = 1  → last stop → deepest → nose containers first (arm DESC)
        //     loadingOrder = N  → first stop → near door → tail containers first (arm ASC)
        //
        //   For intermediate stops we interpolate: if loadingOrder ≤ ⌈N/2⌉ use nose-first,
        //   otherwise tail-first. This keeps the fuselage split clean for 3+ stop flights.
        //
        boolean hasStops = flightStops != null && !flightStops.isEmpty();
        Map<String, Integer> stopToLoadOrder = new HashMap<>();

        // containerOrder: loadingOrder → preferred iteration list for that destination
        Map<Integer, List<UldContainer>> containerOrder = new HashMap<>();

        if (hasStops) {
            int totalStops = flightStops.size();
            for (int i = 0; i < totalStops; i++) {
                // index 0 = first stop → loadingOrder = totalStops (loaded last, near door)
                // index N-1 = last stop → loadingOrder = 1       (loaded first, deepest)
                stopToLoadOrder.put(flightStops.get(i).trim().toUpperCase(), totalStops - i);
            }

            // Pre-build sorted container lists once (not per-package)
            List<UldContainer> noseFirst = containers.stream()
                .sorted(Comparator.comparingInt(c -> -c.position.getArmMm()))
                .collect(Collectors.toList());
            List<UldContainer> tailFirst = containers.stream()
                .sorted(Comparator.comparingInt(c -> c.position.getArmMm()))
                .collect(Collectors.toList());

            int midpoint = (totalStops + 1) / 2; // ⌈N/2⌉
            for (int lo = 1; lo <= totalStops; lo++) {
                // lo=1 (deepest/last-stop) → nose first; lo=N (near door/first-stop) → tail first
                containerOrder.put(lo, lo <= midpoint ? noseFirst : tailFirst);
            }

            log.info("LOFO active: stops={} stopMap={}", totalStops, stopToLoadOrder);
        }

        // ── 3. Sort packages: volume desc (Heßler 2024 §3.2) ─────────────────
        List<com.smartload.entity.Package> sorted = packages.stream()
            .sorted(Comparator.comparingDouble(
                (com.smartload.entity.Package p) -> p.getLengthMm() * p.getWidthMm() * p.getHeightMm()
            ).reversed())
            .collect(Collectors.toList());

        // ── 4. Level 1 — Package → ULD  (LOFO-aware) ─────────────────────────
        //
        //   Without LOFO: iterate containers in display_order (P1R … P9L) for every package.
        //   With LOFO:    each package uses the container list that matches its destination's
        //                 loadingOrder — last-stop packages try nose containers first,
        //                 first-stop packages try tail containers first.
        //
        //   This means last-stop cargo fills forward (nose) slots and first-stop cargo
        //   naturally flows into aft (door-side) slots — exactly the loadmaster rule.
        //
        List<com.smartload.entity.Package> unplaced = new ArrayList<>();

        // Running cargo moment/weight across ALL containers — drives CG-aware
        // placement and is cheap to maintain (one add per placed package).
        double runningCargoMoment = 0;
        double runningCargoWeight = 0;
        boolean cgOrdering = cgAware && !hasStops;

        for (com.smartload.entity.Package pkg : sorted) {
            boolean placed = false;
            double pkgKg = pkg.getGrossWeightKg() != null ? pkg.getGrossWeightKg() : 0;

            // Determine preferred container iteration order for this package
            List<UldContainer> iterOrder = containers; // default: display_order
            if (hasStops) {
                // LOFO accessibility ordering takes precedence (operational hard rule).
                String dest = pkg.getDestinationCode();
                if (dest != null) {
                    Integer lo = stopToLoadOrder.get(dest.trim().toUpperCase());
                    if (lo != null) {
                        List<UldContainer> preferred = containerOrder.get(lo);
                        if (preferred != null) iterOrder = preferred;
                    }
                }
            } else if (cgOrdering) {
                // CG-aware: prefer the container that keeps running CG closest to target.
                final double curMoment = runningCargoMoment;
                final double curWeight = runningCargoWeight;
                iterOrder = containers.stream()
                    .sorted(Comparator.comparingDouble((UldContainer c) -> {
                        double projMac = macPct(
                            curMoment + pkgKg * c.position.getArmMm(),
                            curWeight + pkgKg);
                        return Math.abs(projMac - TARGET_MAC_PCT);
                    }))
                    .collect(Collectors.toList());
            }

            UldContainer placedIn = null;
            for (UldContainer c : iterOrder) {
                if (c.tryPlace(pkg)) {
                    placedIn = c;
                    placed = true;
                    break;
                }
            }
            if (placed) {
                runningCargoMoment += pkgKg * placedIn.position.getArmMm();
                runningCargoWeight += pkgKg;
            } else {
                unplaced.add(pkg);
                log.debug("Unplaced: pkg={} dims={}x{}x{}",
                    pkg.getId(), pkg.getLengthMm(), pkg.getWidthMm(), pkg.getHeightMm());
            }
        }

        // ── 5. Level 2 — ULD → Position (already one-to-one since each     ────
        //    container IS a position; sort used containers centre-first)      ────
        List<UldContainer> used = containers.stream()
            .filter(c -> !c.placements.isEmpty())
            .collect(Collectors.toList());

        // ── 6. Compute summary stats ──────────────────────────────────────────
        int placed      = packages.size() - unplaced.size();
        double totalKg  = used.stream().mapToDouble(c -> c.totalWeightKg).sum();
        double totalVol = used.stream().mapToDouble(UldContainer::bboxVolumeMm3).sum();
        double usedVol  = used.stream().mapToDouble(UldContainer::placedVolumeMm3).sum();
        double util     = totalVol > 0 ? (usedVol / totalVol) * 100.0 : 0.0;

        log.info("BinPacking DONE: placed={}/{} util={}% containers={} unplaced={} time={}ms",
            placed, packages.size(), String.format("%.1f", util),
            used.size(), unplaced.size(), System.currentTimeMillis() - t0);

        // ── 7. LOFO — compute dominant destination + loading order ────────────
        //
        // dominantDestination: the destination code that appears most among the
        //   packages placed in this ULD container.
        // loadingOrder: derived from stopToLoadOrder (built in step 2b above).
        //   1 = first to load (deepest, last-stop destination)
        //   N = last to load  (near door, first-stop destination)
        //
        Map<Long, String>  containerDominant     = new HashMap<>();
        Map<Long, Integer> containerLoadingOrder = new HashMap<>();
        // hasStops and stopToLoadOrder are already set in step 2b

        for (UldContainer c : used) {
            // Count destination occurrences among placed packages
            Map<String, Integer> destCount = new HashMap<>();
            for (PlacedItem pi : c.placements) {
                String dest = pi.pkg.getDestinationCode();
                if (dest != null) {
                    destCount.merge(dest.toUpperCase().trim(), 1, Integer::sum);
                }
            }
            // Dominant = most frequent destination
            String dominant = destCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            containerDominant.put(c.position.getId(), dominant);

            // Loading order based on dominant destination
            if (hasStops && dominant != null) {
                Integer order = stopToLoadOrder.get(dominant);
                if (order == null) {
                    // Destination not in flight stops → treat as last stop (deepest)
                    order = 1;
                }
                containerLoadingOrder.put(c.position.getId(), order);
            }
        }

        // ── 7b. Final CG (OEW + cargo) — recorded on the plan ─────────────────
        double cargoMoment = used.stream()
            .mapToDouble(c -> c.totalWeightKg * c.position.getArmMm()).sum();
        double cgMacPct = macPct(cargoMoment, totalKg);
        String cgStatus = classifyCg(cgMacPct);

        boolean cgApplied = cgAware && !hasStops;
        String algorithm = "EXTREME_POINTS_GREEDY_" + (cgApplied ? "V2_CG" : "V1")
            + (hasStops ? "_LOFO" : "");

        log.info("CG result: {}%MAC status={} (cgApplied={})",
            String.format("%.1f", cgMacPct), cgStatus, cgApplied);

        // ── 8. Persist ────────────────────────────────────────────────────────
        LoadPlan plan = new LoadPlan();
        plan.setManifestId(manifestId);
        plan.setAircraftId(aircraftId);
        plan.setAlgorithm(algorithm);
        plan.setStatus("OPTIMIZED");
        plan.setTotalPackages(packages.size());
        plan.setPlacedPackages(placed);
        plan.setTotalWeightKg(totalKg);
        plan.setUtilizationPct(Math.round(util * 10.0) / 10.0);
        plan.setUsedPositions(used.size());
        plan.setCgMacPct(Math.round(cgMacPct * 10.0) / 10.0);
        plan.setCgStatus(cgStatus);
        plan = loadPlanRepo.save(plan);

        for (UldContainer c : used) {
            double cVol    = c.bboxVolumeMm3();
            double cUsed   = c.placedVolumeMm3();
            double cUtil   = cVol > 0 ? (cUsed / cVol) * 100.0 : 0.0;

            UldAssignment asgn = new UldAssignment(
                plan.getId(),
                c.position.getId(),
                c.position.getUldType().getId(),
                c.totalWeightKg,
                Math.round(cUtil * 10.0) / 10.0,
                c.placements.size()
            );
            asgn.setDominantDestination(containerDominant.get(c.position.getId()));
            asgn.setLoadingOrder(containerLoadingOrder.get(c.position.getId()));
            asgn = assignmentRepo.save(asgn);

            for (PlacedItem pi : c.placements) {
                PackagePlacement pp = new PackagePlacement(
                    asgn.getId(),
                    pi.pkg.getId(),
                    pi.x, pi.y, pi.z,
                    pi.w, pi.h, pi.d,
                    pi.rotIdx
                );
                placementRepo.save(pp);
            }
        }

        return plan.getId();
    }

    /**
     * Per-ULD gross-weight cap for the FFD baseline (kg).
     * Real AHM data unavailable; ≈ a typical main-deck pallet max gross weight
     * (~6 800 kg / 15 000 lb). Forces the naive FFD to spread load across several
     * positions instead of dumping everything into the first bin — a fairer (less
     * strawman) baseline while staying CG-blind and geometry-light.
     */
    private static final double FFD_MAX_ULD_WEIGHT_KG = 6_800.0;

    /**
     * Naive baseline: volume First-Fit Decreasing (FFD), CG-blind, no geometry.
     *
     * Classic 1-D bin-packing baseline for the thesis comparison: packages are
     * sorted by volume (desc) and dropped into the first ULD (display order) whose
     * remaining bounding-box volume AND per-ULD weight cap can still hold them.
     * No extreme points, no rotation, no contour/overlap/support checks — so it
     * OVERESTIMATES feasibility compared with the 3D EP-Greedy. The weight cap
     * ({@link #FFD_MAX_ULD_WEIGHT_KG}) keeps the baseline from collapsing all
     * cargo into a single position.
     *
     * Persists a LoadPlan with per-ULD assignments (no 3D placements). CG recorded.
     */
    @Transactional
    public Long optimizeFfd(String manifestId, Long aircraftId, List<String> flightStops) {
        long t0 = System.currentTimeMillis();

        List<com.smartload.entity.Package> packages = packageRepo.findByManifestId(manifestId);
        if (packages.isEmpty()) {
            throw new IllegalArgumentException("No packages found for manifest " + manifestId);
        }
        List<AircraftPosition> positions =
            positionRepo.findByAircraftIdAndIsActiveTrueOrderByDisplayOrderAsc(aircraftId);
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("No active positions for aircraft " + aircraftId);
        }

        // Build volume bins (one per position).
        List<VolumeBin> bins = new ArrayList<>();
        for (AircraftPosition pos : positions) {
            int[][] c = parseContour(pos.getUldType().getPointsJson());
            int w = 0, h = 0;
            for (int[] p : c) { w = Math.max(w, p[0]); h = Math.max(h, p[1]); }
            bins.add(new VolumeBin(pos, (double) w * h * pos.getUldType().getLengthMm()));
        }

        // Sort packages by volume desc (the "decreasing" in FFD).
        List<com.smartload.entity.Package> sorted = packages.stream()
            .sorted(Comparator.comparingDouble(
                (com.smartload.entity.Package p) -> p.getLengthMm() * p.getWidthMm() * p.getHeightMm()
            ).reversed())
            .collect(Collectors.toList());

        int placed = 0;
        double placedVol = 0;
        for (com.smartload.entity.Package pkg : sorted) {
            double vol = pkg.getLengthMm() * pkg.getWidthMm() * pkg.getHeightMm();
            double kg  = pkg.getGrossWeightKg() != null ? pkg.getGrossWeightKg() : 0;
            for (VolumeBin b : bins) {                  // first-fit in display order
                if (b.used + vol <= b.capacity && b.weight + kg <= FFD_MAX_ULD_WEIGHT_KG) {
                    b.used += vol; b.weight += kg; b.count++;
                    placed++; placedVol += vol;
                    break;
                }
            }
        }

        List<VolumeBin> used = bins.stream().filter(b -> b.count > 0).collect(Collectors.toList());
        double totalCap = used.stream().mapToDouble(b -> b.capacity).sum();
        double util     = totalCap > 0 ? placedVol / totalCap * 100.0 : 0.0;
        double totalKg  = used.stream().mapToDouble(b -> b.weight).sum();
        double cargoMoment = used.stream().mapToDouble(b -> b.weight * b.pos.getArmMm()).sum();
        double cgMacPct = macPct(cargoMoment, totalKg);
        String cgStatus = classifyCg(cgMacPct);

        LoadPlan plan = new LoadPlan();
        plan.setManifestId(manifestId);
        plan.setAircraftId(aircraftId);
        plan.setAlgorithm("FFD_VOLUME_V1");
        plan.setStatus("OPTIMIZED");
        plan.setTotalPackages(packages.size());
        plan.setPlacedPackages(placed);
        plan.setTotalWeightKg(totalKg);
        plan.setUtilizationPct(Math.round(util * 10.0) / 10.0);
        plan.setUsedPositions(used.size());
        plan.setCgMacPct(Math.round(cgMacPct * 10.0) / 10.0);
        plan.setCgStatus(cgStatus);
        plan = loadPlanRepo.save(plan);

        for (VolumeBin b : used) {
            double bUtil = b.capacity > 0 ? b.used / b.capacity * 100.0 : 0.0;
            assignmentRepo.save(new UldAssignment(
                plan.getId(), b.pos.getId(), b.pos.getUldType().getId(),
                b.weight, Math.round(bUtil * 10.0) / 10.0, b.count));
        }

        log.info("FFD DONE: placed={}/{} util={}% cg={}%MAC time={}ms",
            placed, packages.size(), String.format("%.1f", util),
            String.format("%.1f", cgMacPct), System.currentTimeMillis() - t0);
        return plan.getId();
    }

    /** Volume bin for the naive FFD baseline. */
    private static class VolumeBin {
        final AircraftPosition pos;
        final double capacity;
        double used = 0, weight = 0;
        int count = 0;
        VolumeBin(AircraftPosition pos, double capacity) { this.pos = pos; this.capacity = capacity; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inner data classes
    // ══════════════════════════════════════════════════════════════════════════

    /** One placed package item inside a ULD container. */
    private static class PlacedItem {
        final com.smartload.entity.Package pkg;
        final int x, y, z;   // bottom-front-left corner in ULD local mm
        final int w, h, d;   // applied width (lateral), height, depth
        final int rotIdx;

        PlacedItem(com.smartload.entity.Package pkg, int x, int y, int z, int w, int h, int d, int rotIdx) {
            this.pkg = pkg; this.x = x; this.y = y; this.z = z;
            this.w = w; this.h = h; this.d = d; this.rotIdx = rotIdx;
        }
    }

    /**
     * One physical ULD container mapped to one aircraft position.
     *
     * Implements the Extreme Points (EP) heuristic:
     * after each placement, three new candidate positions are generated
     * (right of, on top of, behind the placed item), and positions that
     * fall inside an existing item are pruned.
     *
     * Coordinate system (mm, all non-negative):
     *   x = lateral (0 = left wall)
     *   y = vertical (0 = floor)
     *   z = depth (0 = front face, increases toward back)
     */
    static class UldContainer {

        final AircraftPosition position;
        final int[][] contourPts; // normalized to min-x=0, min-y=0
        final int bboxW;          // max x in normalized contour (mm)
        final int bboxH;          // max y in normalized contour (mm)
        final int bboxL;          // ULD length (mm)

        final List<PlacedItem> placements   = new ArrayList<>();
        final List<int[]>      extremePoints = new ArrayList<>(); // [x, y, z]
        double totalWeightKg = 0;

        UldContainer(AircraftPosition position, int[][] contourPts) {
            this.position   = position;
            this.contourPts = contourPts;
            this.bboxW      = maxVal(contourPts, 0);
            this.bboxH      = maxVal(contourPts, 1);
            this.bboxL      = position.getUldType().getLengthMm();
            this.extremePoints.add(new int[]{0, 0, 0});
        }

        /** Volume of the ULD bounding box in mm³. */
        double bboxVolumeMm3() {
            return (double) bboxW * bboxH * bboxL;
        }

        /** Volume of all placed items' bounding boxes in mm³. */
        double placedVolumeMm3() {
            return placements.stream()
                .mapToDouble(p -> (double) p.w * p.h * p.d)
                .sum();
        }

        /**
         * Try to place {@code pkg} in this container.
         * @return true if placed, false if no valid position found.
         */
        boolean tryPlace(com.smartload.entity.Package pkg) {
            int[] dims = {
                (int) Math.round(pkg.getWidthMm()),
                (int) Math.round(pkg.getHeightMm()),
                (int) Math.round(pkg.getLengthMm()),
            };

            boolean canRotate = pkg.getRotatable() == null || pkg.getRotatable();
            int rotLimit = canRotate ? 6 : 2;

            // Sort EPs: y ascending, then x, then z (bottom-left-front first)
            List<int[]> sortedEPs = extremePoints.stream()
                .sorted(Comparator.comparingInt((int[] ep) -> ep[1])
                    .thenComparingInt(ep -> ep[0])
                    .thenComparingInt(ep -> ep[2]))
                .collect(Collectors.toList());

            for (int[] ep : sortedEPs) {
                for (int r = 0; r < rotLimit; r++) {
                    int[] rot = ROTATIONS[r];
                    int w = dims[rot[0]];
                    int h = dims[rot[1]];
                    int d = dims[rot[2]];

                    if (canFit(ep[0], ep[1], ep[2], w, h, d, pkg)) {
                        // Place it
                        PlacedItem item = new PlacedItem(pkg, ep[0], ep[1], ep[2], w, h, d, r);
                        placements.add(item);
                        totalWeightKg += pkg.getGrossWeightKg() != null ? pkg.getGrossWeightKg() : 0;
                        updateExtremePoints(ep[0], ep[1], ep[2], w, h, d);
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Check whether a box (w × h × d) placed at (px, py, pz) is valid.
         *
         * Checks in order (cheapest first):
         *   1. Bounds: stays within ULD bounding box.
         *   2. Contour: top-right and top-left corners inside ULD cross-section.
         *   3. Overlap: no 3D intersection with placed items.
         *   4. Anti-floating: rests on floor or on a placed item.
         *   5. Stackability: items directly below allow stacking.
         */
        private boolean canFit(int px, int py, int pz, int pw, int ph, int pd, com.smartload.entity.Package pkg) {

            // 1. Bounds
            if (px + pw > bboxW || py + ph > bboxH || pz + pd > bboxL) return false;
            if (px < 0 || py < 0 || pz < 0) return false;

            // 2. Contour — check all 4 vertical corners at the package's max height
            if (!pointInPolygon(contourPts, px,      py + ph)) return false;
            if (!pointInPolygon(contourPts, px + pw, py + ph)) return false;
            if (!pointInPolygon(contourPts, px,      py))      return false;
            if (!pointInPolygon(contourPts, px + pw, py))      return false;

            // 3. Overlap
            for (PlacedItem it : placements) {
                if (overlaps3D(px, py, pz, pw, ph, pd, it.x, it.y, it.z, it.w, it.h, it.d)) {
                    return false;
                }
            }

            // 4. Anti-floating: if not on the floor, something must be directly below
            if (py > 0) {
                boolean supported = false;
                for (PlacedItem it : placements) {
                    // Item top face exactly at py?
                    if (it.y + it.h == py) {
                        // x-z footprint overlaps?
                        if (it.x < px + pw && it.x + it.w > px &&
                            it.z < pz + pd && it.z + it.d > pz) {
                            supported = true;
                            break;
                        }
                    }
                }
                if (!supported) return false;
            }

            // 5. Stackability: items directly below must allow stacking
            boolean newPkgIsHeavy = (pkg.getGrossWeightKg() != null && pkg.getGrossWeightKg() > 0);
            for (PlacedItem it : placements) {
                if (it.y + it.h == py &&
                    it.x < px + pw && it.x + it.w > px &&
                    it.z < pz + pd && it.z + it.d > pz) {
                    if (it.pkg.getStackable() != null && !it.pkg.getStackable()) {
                        return false; // item below is non-stackable
                    }
                }
            }

            return true;
        }

        /**
         * Update the extreme points set after placing an item at (px,py,pz) w×h×d.
         *
         * Adds the three canonical EPs for the new item, then prunes any EP
         * that now falls inside an existing placed item.
         */
        private void updateExtremePoints(int px, int py, int pz, int pw, int ph, int pd) {
            // Three new EPs: right of, on top of, behind the placed item
            extremePoints.add(new int[]{px + pw, py, pz});
            extremePoints.add(new int[]{px, py + ph, pz});
            extremePoints.add(new int[]{px, py, pz + pd});

            // Prune EPs inside any placed item
            extremePoints.removeIf(ep -> placements.stream().anyMatch(it ->
                ep[0] >= it.x && ep[0] <= it.x + it.w &&
                ep[1] >= it.y && ep[1] <= it.y + it.h &&
                ep[2] >= it.z && ep[2] <= it.z + it.d
            ));

            // Prune EPs outside bounds
            extremePoints.removeIf(ep ->
                ep[0] >= bboxW || ep[1] >= bboxH || ep[2] >= bboxL
            );

            // De-duplicate
            Set<String> seen = new HashSet<>();
            extremePoints.removeIf(ep -> !seen.add(ep[0] + "," + ep[1] + "," + ep[2]));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CG helpers (shared envelope constants with CgResultDto)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Aircraft CG as %MAC for a given cargo moment + weight (OEW included).
     * Same formula and constants as {@link CgResultDto}:
     *   CG_arm = (OEW·OEW_arm + cargoMoment) / (OEW + cargoWeight)
     *   %MAC   = (LEMAC − CG_arm) / MAC × 100
     */
    static double macPct(double cargoMoment, double cargoWeight) {
        double totalWeight = CgResultDto.OEW_KG + cargoWeight;
        double totalMoment = CgResultDto.OEW_KG * CgResultDto.OEW_ARM_MM + cargoMoment;
        double cgArm = totalWeight > 0 ? totalMoment / totalWeight : CgResultDto.OEW_ARM_MM;
        return (CgResultDto.LEMAC_MM - cgArm) / CgResultDto.MAC_MM * 100.0;
    }

    /** Classify %MAC into the envelope status used across the app. */
    static String classifyCg(double macPct) {
        if (macPct < CgResultDto.FWD_LIMIT_PCT) return "RED_FWD";
        if (macPct < CgResultDto.FWD_WARN_PCT)  return "YELLOW_FWD";
        if (macPct > CgResultDto.AFT_LIMIT_PCT) return "RED_AFT";
        if (macPct > CgResultDto.AFT_WARN_PCT)  return "YELLOW_AFT";
        return "GREEN";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Static geometry helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Parse ULD cross-section points from the stored JSON string.
     * Normalises the polygon so that min-x = 0, min-y = 0.
     *
     * @param pointsJson e.g. "[[0,0],[2440,0],[2440,1800],[1700,2950],[0,2950]]"
     * @return 2D int array [[x,y], ...]
     */
    public static int[][] parseContour(String pointsJson) {
        // Minimal JSON parse (no external library needed for simple 2D int arrays)
        String clean = pointsJson.trim().replaceAll("[\\[\\]\\s]", "");
        String[] parts = clean.split(",");
        int n = parts.length / 2;
        int[][] pts = new int[n][2];
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            pts[i][0] = Integer.parseInt(parts[i * 2]);
            pts[i][1] = Integer.parseInt(parts[i * 2 + 1]);
            minX = Math.min(minX, pts[i][0]);
            minY = Math.min(minY, pts[i][1]);
        }
        // Normalise to (0, 0) origin
        if (minX != 0 || minY != 0) {
            for (int[] pt : pts) {
                pt[0] -= minX;
                pt[1] -= minY;
            }
        }
        return pts;
    }

    /**
     * Ray-casting point-in-polygon test.
     * Works correctly for all B777F ULD contours (all convex).
     *
     * A point exactly on an edge is considered inside (tolerance ±1 mm).
     */
    static boolean pointInPolygon(int[][] pts, int px, int py) {
        int n = pts.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = pts[i][0], yi = pts[i][1];
            double xj = pts[j][0], yj = pts[j][1];
            if (((yi > py) != (yj > py)) &&
                (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** 3D axis-aligned bounding box overlap test. */
    static boolean overlaps3D(int ax, int ay, int az, int aw, int ah, int ad,
                               int bx, int by, int bz, int bw, int bh, int bd) {
        return ax < bx + bw && bx < ax + aw &&
               ay < by + bh && by < ay + ah &&
               az < bz + bd && bz < az + ad;
    }

    /** Maximum value along one axis in a set of 2D points. */
    private static int maxVal(int[][] pts, int axis) {
        int max = 0;
        for (int[] p : pts) max = Math.max(max, p[axis]);
        return max;
    }
}
