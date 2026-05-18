package com.smartload.service;

import com.smartload.dto.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates raw grid data (from Excel import):
 * 1. Column mapping (auto-detect + override)
 * 2. Data type parsing (string → typed objects)
 * 3. Business rules validation (DG, unit, enums)
 * 4. Per-row error collection
 * 5. Statistics calculation
 */
@Service
public class ManifestValidationService {

    private static final double B777F_MAX_WEIGHT_KG = 140000; // Approximate MTOW
    private static final Set<String> VALID_DG_CLASSES = Set.of(
            "1", "2.1", "2.2", "2.3", "3", "4.1", "4.2", "4.3", "5.1", "5.2", "6.1", "6.2", "7", "8", "9"
    );
    private static final Set<String> VALID_SHC_CODES = Set.of("DG", "AVI", "FRA", "VAL", "PER");

    // Default size-bucket thresholds for air cargo (ULD-oriented). Stored in mm³ to match
    // the per-piece volume unit used elsewhere (canonical unit is mm — see ADR-0011 + Faz 4
    // CG requirements). Frontend may override these client-side; the chosen thresholds are
    // echoed back on the statistics DTO as sizeThresholdsM3 (m³, human-friendly).
    //   < 0.5 m³  → "small"   (fragment of an LD3)
    //   < 1.5 m³  → "medium"  (typical full LD3)
    //   < 4.0 m³  → "large"   (PMC pallet load)
    //   ≥ 4.0 m³  → "oversize" (Q5/Q6 contour, special handling)
    private static final double DEFAULT_SIZE_SMALL_THRESHOLD_MM3  =   500_000_000.0; // 0.5 m³
    private static final double DEFAULT_SIZE_MEDIUM_THRESHOLD_MM3 = 1_500_000_000.0; // 1.5 m³
    private static final double DEFAULT_SIZE_LARGE_THRESHOLD_MM3  = 4_000_000_000.0; // 4.0 m³

    // Conversion factor for Excel input. Excel manifest dimensions are entered in centimetres;
    // backend persists millimetres throughout. The conversion happens at parse time.
    private static final double CM_TO_MM = 10.0;

    /**
     * Main validation entry point.
     * Uses user-provided column mapping if available, otherwise auto-detects.
     */
    public ManifestValidationResponse validate(ManifestValidationRequest request) {
        if (request.getHeaders() == null || request.getHeaders().isEmpty()) {
            return new ManifestValidationResponse(false, Collections.emptyList(),
                    List.of(new RowErrorDto(0, "headers", "No headers provided", "ERROR")), null);
        }

        // 1. Column mapping: use user-provided mapping or auto-detect
        Map<String, Integer> columnIndexMap;
        if (request.getColumnMapping() != null && !request.getColumnMapping().isEmpty()) {
            // User provided mapping (from dialog)
            columnIndexMap = request.getColumnMapping();
        } else {
            // Auto-detect mapping (fallback)
            columnIndexMap = detectColumnMapping(request.getHeaders());
        }

        // 2. Parse & validate rows
        List<PackageDto> validatedPackages = new ArrayList<>();
        List<RowErrorDto> issues = new ArrayList<>();

        if (request.getRows() != null) {
            // Required field names
            Set<String> requiredFields = Set.of("pieces", "grossWeightKg", "lengthCm", "widthCm", "heightCm", "destinationCode");

            for (int rowIdx = 0; rowIdx < request.getRows().size(); rowIdx++) {
                List<String> row = request.getRows().get(rowIdx);

                // Skip completely empty rows
                if (row == null || row.isEmpty() || row.stream().allMatch(c -> c == null || c.trim().isEmpty())) {
                    continue;
                }

                // Skip TOTALS rows (Excel summary rows with pieces + weight but empty dimensions)
                String pieceVal = getCellValue(row, columnIndexMap, "pieces");
                String lengthVal = getCellValue(row, columnIndexMap, "lengthCm");
                String widthVal = getCellValue(row, columnIndexMap, "widthCm");
                String heightVal = getCellValue(row, columnIndexMap, "heightCm");

                if (pieceVal != null && !pieceVal.isEmpty() &&
                    (lengthVal == null || lengthVal.isEmpty()) &&
                    (widthVal == null || widthVal.isEmpty()) &&
                    (heightVal == null || heightVal.isEmpty())) {
                    // Signature of TOTALS row: pieces filled, all dimensions empty
                    System.out.println(">> SKIPPING TOTALS ROW (rowIdx=" + (rowIdx + 1) + "): " + row);
                    continue;
                }

                // Skip rows where ALL required fields are empty
                boolean allRequiredEmpty = true;
                for (String field : requiredFields) {
                    Integer colIdx = columnIndexMap.get(field);
                    if (colIdx != null && colIdx < row.size()) {
                        String cellValue = row.get(colIdx);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            allRequiredEmpty = false;
                            break;
                        }
                    }
                }
                if (allRequiredEmpty) {
                    continue; // Skip this row
                }

                List<RowErrorDto> rowErrors = new ArrayList<>();

                // Excel row 1 = headers; first data row in source_grid.rows = Excel row 2.
                int excelRowNumber = rowIdx + 2;
                try {
                    PackageDto pkg = parseAndValidateRow(row, columnIndexMap, excelRowNumber, rowErrors);
                    if (rowErrors.isEmpty() && pkg != null) {
                        validatedPackages.add(pkg);
                    } else if (rowErrors.size() > 0) {
                        issues.addAll(rowErrors);
                    }
                } catch (Exception e) {
                    issues.add(new RowErrorDto(excelRowNumber, "general", e.getMessage(), "ERROR"));
                }
            }
        }

        // 3. Calculate statistics
        ManifestStatisticsDto stats = calculateStatistics(validatedPackages);

        // 4. Return response
        boolean hasErrors = issues.stream().anyMatch(i -> "ERROR".equals(i.getSeverity()));
        boolean validated = !hasErrors && !validatedPackages.isEmpty();

        return new ManifestValidationResponse(validated, validatedPackages, issues, stats);
    }

    /**
     * Auto-detect column mapping from headers.
     * 3-level strategy:
     * 1. Exact match (normalized)
     * 2. Keyword-based match (contains key phrases)
     * 3. Fuzzy match (similarity score)
     */
    private Map<String, Integer> detectColumnMapping(List<String> headers) {
        Map<String, Integer> mapping = new HashMap<>();

        // Comprehensive mapping table (all variants)
        Map<String, String> exactMatches = Map.ofEntries(
                // Pieces
                Map.entry("PCS", "pieces"),
                Map.entry("PCSADET", "pieces"),
                Map.entry("PIECES", "pieces"),
                Map.entry("ADET", "pieces"),
                Map.entry("PERWEIGHT", "weightPerPieceKg"),
                Map.entry("PERPIECE", "weightPerPieceKg"),
                // Gross Weight
                Map.entry("WEIGHT", "grossWeightKg"),
                Map.entry("GROSSWEIGHT", "grossWeightKg"),
                Map.entry("BRUTWEIGHT", "grossWeightKg"),
                Map.entry("GROSS", "grossWeightKg"),
                Map.entry("BRUT", "grossWeightKg"),
                // Length
                Map.entry("LENGTH", "lengthCm"),
                Map.entry("LENGHT", "lengthCm"),
                Map.entry("L", "lengthCm"),
                Map.entry("BOY", "lengthCm"),
                // Width
                Map.entry("WIDTH", "widthCm"),
                Map.entry("W", "widthCm"),
                Map.entry("EN", "widthCm"),
                // Height
                Map.entry("HEIGHT", "heightCm"),
                Map.entry("H", "heightCm"),
                // Stackable
                Map.entry("STACKABLE", "stackable"),
                Map.entry("STACK", "stackable"),
                // Rotatable
                Map.entry("ROTATABLE", "rotatable"),
                Map.entry("TURNABLE", "rotatable"),
                Map.entry("ROTATE", "rotatable"),
                // Toploadable
                Map.entry("TOPLOADABLE", "toploadable"),
                Map.entry("TOPLOAD", "toploadable"),
                // CanMix
                Map.entry("CANMIX", "canMix"),
                Map.entry("CAN_MIX", "canMix"),
                // ULD Preference
                Map.entry("ULDPREFERENCE", "uldPreference"),
                Map.entry("PREFERENCE", "uldPreference"),
                Map.entry("ULD", "uldPreference"),
                // Destination
                Map.entry("DESTINATION", "destinationCode"),
                Map.entry("DEST", "destinationCode"),
                Map.entry("DESTINATIONTO", "destinationCode"),
                Map.entry("ORIGIN", "destinationCode"),
                Map.entry("FROM", "destinationCode"),
                Map.entry("ORIGINFROM", "destinationCode"),
                // Special Handling
                Map.entry("SPECIALHANDLING", "specialHandling"),
                Map.entry("SHC", "specialHandling"),
                Map.entry("HANDLING", "specialHandling"),
                // DG Class
                Map.entry("DGCLASS", "dgClass"),
                Map.entry("DG", "dgClass"),
                Map.entry("CLASS", "dgClass")
        );

        // Keyword-based detection (fallback)
        Map<String, String> keywordMatches = Map.ofEntries(
                Map.entry("PCS", "pieces"),
                Map.entry("ADET", "pieces"),
                Map.entry("GROSS", "grossWeightKg"),
                Map.entry("BRUT", "grossWeightKg"),
                Map.entry("WEIGHT", "grossWeightKg"),
                Map.entry("LENGTH", "lengthCm"),
                Map.entry("LENGHT", "lengthCm"),
                Map.entry("WIDTH", "widthCm"),
                Map.entry("HEIGHT", "heightCm"),
                Map.entry("STACK", "stackable"),
                Map.entry("TURN", "rotatable"),
                Map.entry("TOPLOAD", "toploadable"),
                Map.entry("DEST", "destinationCode"),
                Map.entry("ORIGIN", "destinationCode"),
                Map.entry("SHC", "specialHandling"),
                Map.entry("DG", "dgClass")
        );

        // Level 1: Exact match (normalized)
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toUpperCase().trim().replaceAll("\\s+", "");
            String field = exactMatches.get(header);
            if (field != null && !mapping.containsKey(field)) {
                mapping.put(field, i);
            }
        }

        // Level 2: Keyword-based match (for missing fields)
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toUpperCase().trim();
            for (Map.Entry<String, String> entry : keywordMatches.entrySet()) {
                if (header.contains(entry.getKey()) && !mapping.containsKey(entry.getValue())) {
                    mapping.put(entry.getValue(), i);
                    break; // Stop after first match
                }
            }
        }

        return mapping;
    }

    /**
     * Get cell value from row by field name (using column mapping).
     */
    private String getCellValue(List<String> row, Map<String, Integer> columnIndexMap, String fieldName) {
        Integer colIdx = columnIndexMap.get(fieldName);
        if (colIdx == null || colIdx >= row.size()) {
            return null;
        }
        String value = row.get(colIdx);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /**
     * Parse double with error handling.
     */
    private Double parseDouble(String fieldName, String value, boolean required,
                               int rowNumber, List<RowErrorDto> rowErrors) {
        try {
            if (value == null || value.isEmpty()) {
                if (required) {
                    rowErrors.add(new RowErrorDto(rowNumber, fieldName, fieldName + " is required", "ERROR"));
                }
                return null;
            }
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            rowErrors.add(new RowErrorDto(rowNumber, fieldName, fieldName + " must be a number", "ERROR"));
            return null;
        }
    }

    /**
     * Parse integer with error handling.
     */
    private Integer parseInteger(String fieldName, String value, boolean required,
                                int rowNumber, List<RowErrorDto> rowErrors) {
        try {
            if (value == null || value.isEmpty()) {
                if (required) {
                    rowErrors.add(new RowErrorDto(rowNumber, fieldName, fieldName + " is required", "ERROR"));
                }
                return null;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            rowErrors.add(new RowErrorDto(rowNumber, fieldName, fieldName + " must be an integer", "ERROR"));
            return null;
        }
    }

    /**
     * Parse boolean (yes/no/true/false).
     */
    private Boolean parseBoolean(String fieldName, String value, int rowNumber, List<RowErrorDto> rowErrors) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String upper = value.toUpperCase();
        if ("YES".equals(upper) || "TRUE".equals(upper) || "1".equals(upper)) {
            return true;
        }
        if ("NO".equals(upper) || "FALSE".equals(upper) || "0".equals(upper)) {
            return false;
        }
        rowErrors.add(new RowErrorDto(rowNumber, fieldName, fieldName + " must be yes/no", "WARNING"));
        return null;
    }

    /**
     * Parse and validate a single row.
     * Returns PackageDto if valid, adds errors to rowErrors list.
     */
    private PackageDto parseAndValidateRow(List<String> row, Map<String, Integer> columnIndexMap,
                                          int rowNumber, List<RowErrorDto> rowErrors) {
        PackageDto pkg = new PackageDto();

        // Helper: get cell value by field name
        String getPieces = getCellValue(row, columnIndexMap, "pieces");
        String getLengthCm = getCellValue(row, columnIndexMap, "lengthCm");
        String getWidthCm = getCellValue(row, columnIndexMap, "widthCm");
        String getHeightCm = getCellValue(row, columnIndexMap, "heightCm");
        String getGrossWeightKg = getCellValue(row, columnIndexMap, "grossWeightKg");
        String getWeightPerPieceKg = getCellValue(row, columnIndexMap, "weightPerPieceKg");
        String getStackable = getCellValue(row, columnIndexMap, "stackable");
        String getRotatable = getCellValue(row, columnIndexMap, "rotatable");
        String getToploadable = getCellValue(row, columnIndexMap, "toploadable");
        String getCanMix = getCellValue(row, columnIndexMap, "canMix");
        String getUldPref = getCellValue(row, columnIndexMap, "uldPreference");
        String getDestination = getCellValue(row, columnIndexMap, "destinationCode");
        String getShc = getCellValue(row, columnIndexMap, "specialHandling");
        String getDgClass = getCellValue(row, columnIndexMap, "dgClass");

        // Parse required fields
        Integer pieces = parseInteger("pieces", getPieces, true, rowNumber, rowErrors);
        Double lengthCm = parseDouble("lengthCm", getLengthCm, true, rowNumber, rowErrors);
        Double widthCm = parseDouble("widthCm", getWidthCm, true, rowNumber, rowErrors);
        Double heightCm = parseDouble("heightCm", getHeightCm, true, rowNumber, rowErrors);
        Double grossWeightKg = parseDouble("grossWeightKg", getGrossWeightKg, true, rowNumber, rowErrors);

        if (pieces == null || lengthCm == null || widthCm == null || heightCm == null || grossWeightKg == null) {
            // Required fields missing — package invalid but errors already added
            return null;
        }

        // Parse optional fields
        Double weightPerPiece = parseDouble("weightPerPieceKg", getWeightPerPieceKg, false, rowNumber, rowErrors);
        Boolean stackable = parseBoolean("stackable", getStackable, rowNumber, rowErrors);
        Boolean rotatable = parseBoolean("rotatable", getRotatable, rowNumber, rowErrors);
        Boolean toploadable = parseBoolean("toploadable", getToploadable, rowNumber, rowErrors);
        Boolean canMix = parseBoolean("canMix", getCanMix, rowNumber, rowErrors);
        String uldPref = getUldPref;
        String destination = getDestination;
        String shc = getShc;
        String dgClass = getDgClass;

        // Business rule validation: destination code
        if (destination == null || destination.isEmpty()) {
            rowErrors.add(new RowErrorDto(rowNumber, "destinationCode", "Destination code is required", "ERROR"));
        } else if (destination.length() != 3) {
            rowErrors.add(new RowErrorDto(rowNumber, "destinationCode", "Destination code must be 3 letters (IATA)", "WARNING"));
        }

        // DG validation: if specialHandling contains "DG", dgClass is required
        if (shc != null && shc.toUpperCase().contains("DG")) {
            if (dgClass == null || dgClass.isEmpty()) {
                rowErrors.add(new RowErrorDto(rowNumber, "dgClass", "DG Class is required when Special Handling = DG", "ERROR"));
            } else if (!VALID_DG_CLASSES.contains(dgClass)) {
                rowErrors.add(new RowErrorDto(rowNumber, "dgClass", "Invalid DG Class: " + dgClass, "ERROR"));
            }
        }

        // Validate special handling codes
        if (shc != null && !shc.isEmpty()) {
            String[] codes = shc.split(",");
            for (String code : codes) {
                String trimmed = code.trim().toUpperCase();
                if (!VALID_SHC_CODES.contains(trimmed)) {
                    rowErrors.add(new RowErrorDto(rowNumber, "specialHandling", "Invalid SHC code: " + trimmed, "WARNING"));
                }
            }
        }

        // Set values — Excel input is centimetres, persist as mm (canonical backend unit).
        // Single conversion point: every downstream consumer (statistics, packing algorithm,
        // CG calculator) assumes mm without re-checking.
        pkg.setPieces(pieces);
        pkg.setLengthMm(lengthCm * CM_TO_MM);
        pkg.setWidthMm(widthCm * CM_TO_MM);
        pkg.setHeightMm(heightCm * CM_TO_MM);
        pkg.setGrossWeightKg(grossWeightKg);
        pkg.setWeightPerPieceKg(weightPerPiece);
        pkg.setStackable(stackable != null ? stackable : true);
        pkg.setRotatable(rotatable != null ? rotatable : true);
        pkg.setToploadable(toploadable != null ? toploadable : true);
        pkg.setCanMix(canMix != null ? canMix : true);
        pkg.setUldPreference(uldPref);
        pkg.setDestinationCode(destination);
        pkg.setSpecialHandling(shc);
        pkg.setDgClass(dgClass);
        pkg.setSourceRowNumber(rowNumber);

        return pkg;
    }

    /**
     * Calculate statistics from validated packages.
     */
    private ManifestStatisticsDto calculateStatistics(List<PackageDto> packages) {
        ManifestStatisticsDto stats = new ManifestStatisticsDto();

        // Default size thresholds, exposed on every response so the frontend knows what was used
        // and can offer an override UI without re-deriving the defaults.
        Map<String, Double> defaultThresholdsM3 = new HashMap<>();
        defaultThresholdsM3.put("small",  DEFAULT_SIZE_SMALL_THRESHOLD_MM3  / 1_000_000_000.0);
        defaultThresholdsM3.put("medium", DEFAULT_SIZE_MEDIUM_THRESHOLD_MM3 / 1_000_000_000.0);
        defaultThresholdsM3.put("large",  DEFAULT_SIZE_LARGE_THRESHOLD_MM3  / 1_000_000_000.0);
        stats.setSizeThresholdsM3(defaultThresholdsM3);

        if (packages.isEmpty()) {
            stats.setTotalPieces(0);
            stats.setTotalWeightKg(0.0);
            stats.setTotalVolumeMm3(0.0);
            stats.setAverageDensity(0.0);
            stats.setCapacityPercentage(0.0);
            stats.setDestinationBreakdown(new HashMap<>());
            stats.setSpecialHandlingBreakdown(new HashMap<>());
            stats.setSizeSmall(0);
            stats.setSizeMedium(0);
            stats.setSizeLarge(0);
            stats.setSizeOversize(0);
            return stats;
        }

        // Volumetric (all linear units now mm; volumes mm³).
        //   totalWeight: each row's grossWeightKg is already (perPieceKg × pieces) from the
        //       Excel "GROSS WEIGHT" column, so a plain sum is correct.
        //   totalVolumeMm3: each row's L*W*H is the volume of ONE piece (in mm³), so it MUST
        //       be multiplied by pieces. (Previously this multiplier was missing — see
        //       troubleshooting/manifest-volume-calculation-pieces-multiplier.md.)
        //   density (kg/m³): divide by 10⁹ to convert mm³ → m³.
        int totalPieces = packages.stream().mapToInt(PackageDto::getPieces).sum();
        double totalWeight = packages.stream().mapToDouble(PackageDto::getGrossWeightKg).sum();
        double totalVolumeMm3 = packages.stream()
                .mapToDouble(p -> p.getLengthMm() * p.getWidthMm() * p.getHeightMm() * p.getPieces())
                .sum();
        double density = totalVolumeMm3 > 0 ? totalWeight / (totalVolumeMm3 / 1_000_000_000.0) : 0; // kg/m³

        stats.setTotalPieces(totalPieces);
        stats.setTotalWeightKg(totalWeight);
        stats.setTotalVolumeMm3(totalVolumeMm3);
        stats.setAverageDensity(Math.round(density * 100.0) / 100.0);

        // Capacity vs B777F
        double capacityPct = (totalWeight / B777F_MAX_WEIGHT_KG) * 100;
        stats.setCapacityPercentage(Math.round(capacityPct * 10.0) / 10.0);

        // Destination breakdown
        Map<String, Integer> destBreakdown = packages.stream()
                .collect(Collectors.groupingBy(PackageDto::getDestinationCode,
                        Collectors.summingInt(PackageDto::getPieces)));
        stats.setDestinationBreakdown(destBreakdown);

        // Special Handling breakdown
        Map<String, Integer> shcBreakdown = new HashMap<>();
        for (PackageDto pkg : packages) {
            if (pkg.getSpecialHandling() != null && !pkg.getSpecialHandling().isEmpty()) {
                String[] codes = pkg.getSpecialHandling().split(",");
                for (String code : codes) {
                    String trimmed = code.trim().toUpperCase();
                    shcBreakdown.put(trimmed, shcBreakdown.getOrDefault(trimmed, 0) + pkg.getPieces());
                }
            }
        }
        stats.setSpecialHandlingBreakdown(shcBreakdown);

        // Size distribution — classify each row by ONE piece's L*W*H (in mm³), then add the
        // row's `pieces` count to the matching bucket. Thresholds are ULD-oriented (see field
        // constants at top of class); the frontend can recompute these buckets with user-chosen
        // thresholds against the per-piece volumes carried on each PackageDto.
        int small = 0, medium = 0, large = 0, oversize = 0;
        for (PackageDto pkg : packages) {
            double volPerPieceMm3 = pkg.getLengthMm() * pkg.getWidthMm() * pkg.getHeightMm();
            if (volPerPieceMm3 < DEFAULT_SIZE_SMALL_THRESHOLD_MM3)        small    += pkg.getPieces();
            else if (volPerPieceMm3 < DEFAULT_SIZE_MEDIUM_THRESHOLD_MM3)  medium   += pkg.getPieces();
            else if (volPerPieceMm3 < DEFAULT_SIZE_LARGE_THRESHOLD_MM3)   large    += pkg.getPieces();
            else                                                          oversize += pkg.getPieces();
        }
        stats.setSizeSmall(small);
        stats.setSizeMedium(medium);
        stats.setSizeLarge(large);
        stats.setSizeOversize(oversize);

        return stats;
    }
}
