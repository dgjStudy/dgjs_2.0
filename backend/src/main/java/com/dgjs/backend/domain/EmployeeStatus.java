package com.dgjs.backend.domain;

public enum EmployeeStatus {
    ACTIVE,
    INACTIVE;

    public static EmployeeStatus from(String value) {
        String trimmed = value == null ? "" : value.trim();
        for (EmployeeStatus status : values()) {
            if (status.name().equals(trimmed)) {
                return status;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 상태입니다. (ACTIVE, INACTIVE)");
    }
}
