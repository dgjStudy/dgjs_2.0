package com.dgjs.backend.service;

public final class PasswordPolicy {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 64;

    private PasswordPolicy() {
    }

    public static void validate(String newPassword, String currentPassword, String email) {
        if (newPassword == null || newPassword.length() < MIN_LENGTH || newPassword.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("새 비밀번호는 " + MIN_LENGTH + "자 이상 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("새 비밀번호는 영문과 숫자를 모두 포함해야 합니다.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
        if (email != null && newPassword.equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("비밀번호는 이메일과 같을 수 없습니다.");
        }
    }
}
