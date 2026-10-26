package com.dgjs.backend.domain;

public enum EmployeeRole {
    ADMIN,
    USER;

    public static EmployeeRole from(String value) {
        String trimmed = value == null ? "" : value.trim();
        for (EmployeeRole role : values()) {
            if (role.name().equals(trimmed)) {
                return role;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 역할입니다. (ADMIN, USER)");
    }
}
