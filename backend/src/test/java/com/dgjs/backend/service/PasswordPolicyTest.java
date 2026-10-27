package com.dgjs.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void 조건을_만족하는_비밀번호는_통과한다() {
        assertThatCode(() -> PasswordPolicy.validate("abcd1234", "old-pass-1", "a@dgjs.local")).doesNotThrowAnyException();
    }

    @Test
    void 너무_짧거나_긴_비밀번호는_거부한다() {
        assertThatThrownBy(() -> PasswordPolicy.validate("abc1234", "old", "a@dgjs.local"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8자");
        assertThatThrownBy(() -> PasswordPolicy.validate("a1".repeat(33), "old", "a@dgjs.local"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate(null, "old", "a@dgjs.local"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 영문과_숫자를_모두_포함해야_한다() {
        assertThatThrownBy(() -> PasswordPolicy.validate("abcdefgh", "old", "a@dgjs.local"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("영문과 숫자");
        assertThatThrownBy(() -> PasswordPolicy.validate("12345678", "old", "a@dgjs.local"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 현재_비밀번호나_이메일과_같으면_거부한다() {
        assertThatThrownBy(() -> PasswordPolicy.validate("abcd1234", "abcd1234", "a@dgjs.local"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호");
        assertThatThrownBy(() -> PasswordPolicy.validate("a1@dgjs.local", "old", "A1@dgjs.local"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
    }
}
