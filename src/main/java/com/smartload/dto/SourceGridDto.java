package com.smartload.dto;

import java.util.List;

/**
 * Immutable Excel import grid stored on the manifest.
 * headers = Excel row 1; rows = data rows only (Excel row 2 .. N).
 */
public class SourceGridDto {
    private String sheetName;
    private List<String> headers;
    private List<List<String>> rows;

    public SourceGridDto() {}

    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }

    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }

    public List<List<String>> getRows() { return rows; }
    public void setRows(List<List<String>> rows) { this.rows = rows; }
}
