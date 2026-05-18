package com.smartload.service;

import com.smartload.dto.*;
import com.smartload.entity.Manifest;
import com.smartload.entity.Package;
import com.smartload.entity.Shipment;
import com.smartload.repository.ManifestRepository;
import com.smartload.repository.PackageRepository;
import com.smartload.repository.ShipmentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manifest persistence: immutable source layer + validate-time snapshot + editable packages.
 *
 *   • source_grid + column_mapping — frozen Excel import (UI read-only).
 *   • validation_result — validate-time audit (issues, statistics, packages at import).
 *   • packages / shipments — working set (editable; changes go to audit_log).
 */
@Service
public class ManifestService {

    @Autowired
    private ManifestRepository manifestRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PackageRepository packageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Manifest saveManifest(ManifestSaveRequest request) {
        ManifestValidationResponse validationResult = request.getValidationResult();
        if (validationResult == null || validationResult.getValidated() == null || !validationResult.getValidated()) {
            throw new IllegalArgumentException("Cannot save invalid manifest. Must pass validation first.");
        }
        if (request.getSourceGrid() == null
                || request.getSourceGrid().getHeaders() == null
                || request.getSourceGrid().getRows() == null) {
            throw new IllegalArgumentException("sourceGrid with headers and rows is required");
        }
        if (request.getColumnMapping() == null || request.getColumnMapping().isEmpty()) {
            throw new IllegalArgumentException("columnMapping is required");
        }

        String fileName = request.getFileName() != null
                ? request.getFileName()
                : "Manifest-" + System.currentTimeMillis();

        Manifest manifest = new Manifest();
        manifest.setFileName(fileName);
        manifest.setStatus("SAVED");
        manifest.setCreatedAt(LocalDateTime.now());
        manifest.setUpdatedAt(LocalDateTime.now());

        if (validationResult.getStatistics() != null) {
            manifest.setTotalPieces(validationResult.getStatistics().getTotalPieces());
            manifest.setTotalWeightKg(validationResult.getStatistics().getTotalWeightKg());
            manifest.setTotalVolumeMm3(validationResult.getStatistics().getTotalVolumeMm3());
            manifest.setCapacityPercentage(validationResult.getStatistics().getCapacityPercentage());
        }

        try {
            manifest.setSourceGrid(objectMapper.writeValueAsString(request.getSourceGrid()));
            manifest.setColumnMapping(objectMapper.writeValueAsString(request.getColumnMapping()));
            manifest.setValidationResult(objectMapper.writeValueAsString(validationResult));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize manifest JSON fields", e);
        }

        Manifest savedManifest = manifestRepository.save(manifest);
        persistPackagesAndShipments(savedManifest, validationResult.getPackages());
        return savedManifest;
    }

    private void persistPackagesAndShipments(Manifest manifest, List<PackageDto> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }

        Shipment shipment = new Shipment();
        shipment.setManifestId(manifest.getId());
        String shortId = manifest.getId().length() >= 8 ? manifest.getId().substring(0, 8) : manifest.getId();
        shipment.setCode("MANIFEST-" + shortId + "-DEFAULT");
        shipment.setDestinationCode(packages.get(0).getDestinationCode());
        shipment.setCanMix(true);
        Shipment savedShipment = shipmentRepository.save(shipment);

        int fallbackRow = 2;
        for (PackageDto dto : packages) {
            Package pkg = new Package();
            pkg.setManifestId(manifest.getId());
            pkg.setShipmentId(savedShipment.getId());
            Integer excelRow = dto.getSourceRowNumber();
            pkg.setSourceRowNumber(excelRow != null ? excelRow : fallbackRow++);
            pkg.setPieces(dto.getPieces());
            pkg.setLengthMm(dto.getLengthMm());
            pkg.setWidthMm(dto.getWidthMm());
            pkg.setHeightMm(dto.getHeightMm());
            pkg.setWeightPerPieceKg(dto.getWeightPerPieceKg());
            pkg.setGrossWeightKg(dto.getGrossWeightKg());
            pkg.setStackable(dto.getStackable());
            pkg.setRotatable(dto.getRotatable());
            pkg.setToploadable(dto.getToploadable());
            pkg.setCanMix(dto.getCanMix());
            pkg.setUldPreference(dto.getUldPreference());
            pkg.setDestinationCode(dto.getDestinationCode());
            pkg.setSpecialHandling(dto.getSpecialHandling());
            pkg.setDgClass(dto.getDgClass());
            packageRepository.save(pkg);
        }
    }

    public Optional<Manifest> getManifestEntity(String id) {
        return manifestRepository.findById(id);
    }

    public Optional<ManifestSummaryDto> getManifestSummary(String id) {
        return manifestRepository.findById(id).map(this::toSummaryDto);
    }

    public Optional<ManifestSourceResponseDto> getManifestSource(String id) {
        return manifestRepository.findById(id).map(m -> {
            ManifestSourceResponseDto dto = new ManifestSourceResponseDto();
            dto.setManifestId(m.getId());
            dto.setFileName(m.getFileName());
            dto.setSourceGrid(readSourceGrid(m));
            dto.setColumnMapping(readColumnMapping(m));
            return dto;
        });
    }

    public List<ManifestSummaryDto> listManifestSummaries() {
        return manifestRepository.findAll().stream()
                .sorted(Comparator.comparing(Manifest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteManifest(String id) {
        packageRepository.deleteByManifestId(id);
        shipmentRepository.deleteByManifestId(id);
        manifestRepository.deleteById(id);
    }

    public Manifest updateStatus(String id, String newStatus) {
        Optional<Manifest> optional = manifestRepository.findById(id);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Manifest not found: " + id);
        }
        Manifest manifest = optional.get();
        manifest.setStatus(newStatus);
        manifest.setUpdatedAt(LocalDateTime.now());
        return manifestRepository.save(manifest);
    }

    private ManifestSummaryDto toSummaryDto(Manifest m) {
        ManifestSummaryDto dto = new ManifestSummaryDto();
        dto.setId(m.getId());
        dto.setStatus(m.getStatus());
        dto.setFileName(m.getFileName());
        dto.setTotalPieces(m.getTotalPieces());
        dto.setTotalWeightKg(m.getTotalWeightKg());
        dto.setTotalVolumeMm3(m.getTotalVolumeMm3());
        dto.setCapacityPercentage(m.getCapacityPercentage());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setUpdatedAt(m.getUpdatedAt());
        dto.setHasSourceGrid(m.getSourceGrid() != null && !m.getSourceGrid().isBlank());
        return dto;
    }

    private SourceGridDto readSourceGrid(Manifest m) {
        if (m.getSourceGrid() == null || m.getSourceGrid().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(m.getSourceGrid(), SourceGridDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse source_grid for manifest " + m.getId(), e);
        }
    }

    private Map<String, Integer> readColumnMapping(Manifest m) {
        if (m.getColumnMapping() == null || m.getColumnMapping().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(m.getColumnMapping(), new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse column_mapping for manifest " + m.getId(), e);
        }
    }
}
