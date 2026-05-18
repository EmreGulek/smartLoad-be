package com.smartload.dto;

import java.util.List;

/**
 * Validation result: validated packages + errors + statistics.
 */
public class ManifestValidationResponse {
    private Boolean validated;
    private List<PackageDto> packages;
    private List<RowErrorDto> issues;
    private ManifestStatisticsDto statistics;

    public ManifestValidationResponse() {
    }

    public ManifestValidationResponse(Boolean validated, List<PackageDto> packages,
                                      List<RowErrorDto> issues, ManifestStatisticsDto statistics) {
        this.validated = validated;
        this.packages = packages;
        this.issues = issues;
        this.statistics = statistics;
    }

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public List<PackageDto> getPackages() {
        return packages;
    }

    public void setPackages(List<PackageDto> packages) {
        this.packages = packages;
    }

    public List<RowErrorDto> getIssues() {
        return issues;
    }

    public void setIssues(List<RowErrorDto> issues) {
        this.issues = issues;
    }

    public ManifestStatisticsDto getStatistics() {
        return statistics;
    }

    public void setStatistics(ManifestStatisticsDto statistics) {
        this.statistics = statistics;
    }
}
