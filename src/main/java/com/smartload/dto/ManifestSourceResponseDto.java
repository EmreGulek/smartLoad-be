package com.smartload.dto;

import java.util.Map;

/** GET /manifests/{id}/source — immutable grid + frozen mapping. */
public class ManifestSourceResponseDto {
    private String manifestId;
    private String fileName;
    private SourceGridDto sourceGrid;
    private Map<String, Integer> columnMapping;

    public String getManifestId() { return manifestId; }
    public void setManifestId(String manifestId) { this.manifestId = manifestId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public SourceGridDto getSourceGrid() { return sourceGrid; }
    public void setSourceGrid(SourceGridDto sourceGrid) { this.sourceGrid = sourceGrid; }

    public Map<String, Integer> getColumnMapping() { return columnMapping; }
    public void setColumnMapping(Map<String, Integer> columnMapping) { this.columnMapping = columnMapping; }
}
