package com.smartload.service;

import com.smartload.dto.BenchmarkRowDto;
import com.smartload.entity.LoadPlan;
import com.smartload.repository.LoadPlanRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * BenchmarkService — runs the same manifest through several algorithms and
 * collects comparable metrics for the thesis experimental chapter.
 *
 * Algorithms compared:
 *   1. FFD (volume first-fit)      — naive 1-D baseline (CG-blind).
 *   2. EP Greedy V1 (CG-blind)     — Heßler 2024 extreme points, display order.
 *   3. EP Greedy V2 (CG-aware)     — extreme points + CG-balancing placement.
 *
 * Each run persists a LoadPlan (so it can also be opened in the viewer). Timing
 * is wall-clock and includes persistence — noted as a limitation in the thesis.
 */
@Service
public class BenchmarkService {

    private final BinPackingService binPackingService;
    private final LoadPlanRepository loadPlanRepo;

    public BenchmarkService(BinPackingService binPackingService,
                            LoadPlanRepository loadPlanRepo) {
        this.binPackingService = binPackingService;
        this.loadPlanRepo = loadPlanRepo;
    }

    public List<BenchmarkRowDto> run(String manifestId, Long aircraftId, List<String> flightStops) {
        Long acId = aircraftId != null ? aircraftId : 1L;
        List<BenchmarkRowDto> rows = new ArrayList<>();
        rows.add(measure("FFD (volume first-fit)",
            () -> binPackingService.optimizeFfd(manifestId, acId, flightStops)));
        rows.add(measure("EP Greedy V1 (CG-blind)",
            () -> binPackingService.optimize(manifestId, acId, flightStops, false)));
        rows.add(measure("EP Greedy V2 (CG-aware)",
            () -> binPackingService.optimize(manifestId, acId, flightStops, true)));
        return rows;
    }

    private BenchmarkRowDto measure(String label, Supplier<Long> run) {
        long t0 = System.currentTimeMillis();
        Long planId = run.get();
        long ms = System.currentTimeMillis() - t0;
        LoadPlan plan = loadPlanRepo.findById(planId)
            .orElseThrow(() -> new IllegalStateException("Benchmark plan not found: " + planId));
        return BenchmarkRowDto.from(label, planId, ms, plan);
    }
}
