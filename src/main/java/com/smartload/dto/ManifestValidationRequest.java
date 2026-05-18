package com.smartload.dto;

import java.util.List;
import java.util.Map;

/**
 * Raw grid data from frontend (Excel import).
 * Headers: column names (auto-detected from row 0)
 * Rows: actual data rows
 * ColumnMapping: user-selected mapping (field name → column index)
 */
public class ManifestValidationRequest {
    private List<String> headers;
    private List<List<String>> rows;
    private Map<String, Integer> columnMapping; // e.g., {"pieces": 0, "grossWeightKg": 2, ...}

    public ManifestValidationRequest() {}

    public ManifestValidationRequest(List<String> headers, List<List<String>> rows) {
        this.headers = headers;
        this.rows = rows;
    }

    public ManifestValidationRequest(List<String> headers, List<List<String>> rows, Map<String, Integer> columnMapping) {
        this.headers = headers;
        this.rows = rows;
        this.columnMapping = columnMapping;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }

    public Map<String, Integer> getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(Map<String, Integer> columnMapping) {
        this.columnMapping = columnMapping;
    }
}
