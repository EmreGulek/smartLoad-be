package com.smartload.dto;

/**
 * Per-row validation error.
 */
public class RowErrorDto {
    private Integer rowNumber;
    private String columnName;
    private String message;
    private String severity; // ERROR, WARNING, INFO

    public RowErrorDto() {}

    public RowErrorDto(Integer rowNumber, String columnName, String message, String severity) {
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.message = message;
        this.severity = severity;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
