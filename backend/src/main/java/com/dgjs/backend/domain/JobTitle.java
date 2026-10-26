package com.dgjs.backend.domain;

import java.util.Arrays;
import java.util.stream.Collectors;

// DB에는 label(한글)을 저장하며, 허용 값은 DB 제약이 아니라 이 enum으로 서버에서 검증함
public enum JobTitle {
    STAFF("사원"),
    ASSOCIATE("대리"),
    MANAGER("과장"),
    SENIOR_MANAGER("차장"),
    DIRECTOR("부장"),
    EXECUTIVE("임원");

    private final String label;

    JobTitle(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static JobTitle fromLabel(String label) {
        String trimmed = label == null ? "" : label.trim();
        return Arrays.stream(values())
                .filter(t -> t.label.equals(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "유효하지 않은 직급입니다. (" + Arrays.stream(values()).map(JobTitle::getLabel).collect(Collectors.joining(", ")) + ")"));
    }
}
