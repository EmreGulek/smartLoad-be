package com.smartload.dto;

import java.util.Map;

public class ManifestSaveRequest {
    private ManifestValidationResponse validationResult;
    private String fileName;
    private SourceGridDto sourceGrid;
    private Map<String, Integer> columnMapping;

    public ManifestValidationResponse getValidationResult() { return validationResult; }
    public void setValidationResult(ManifestValidationResponse validationResult) {
        this.validationResult = validationResult;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public SourceGridDto getSourceGrid() { return sourceGrid; }
    public void setSourceGrid(SourceGridDto sourceGrid) { this.sourceGrid = sourceGrid; }

    public Map<String, Integer> getColumnMapping() { return columnMapping; }
    public void setColumnMapping(Map<String, Integer> columnMapping) { this.columnMapping = columnMapping; }
}
