package com.dgjs.backend.service;

import java.util.regex.Pattern;

final class InputValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EMAIL_MAX_LENGTH = 100;

    private InputValidator() {
    }

    static String requireText(String value, String label, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + "은(는) 필수입니다.");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(label + "은(는) " + maxLength + "자 이내여야 합니다.");
        }
        return trimmed;
    }

    static String normalizeEmail(String value) {
        String email = requireText(value, "이메일", EMAIL_MAX_LENGTH).toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
        return email;
    }
}
