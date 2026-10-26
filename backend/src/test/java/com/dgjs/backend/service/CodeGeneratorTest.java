package com.dgjs.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGeneratorTest {

    @Test
    void 최댓값이_없으면_1번부터_시작한다() {
        assertThat(CodeGenerator.next("E", 4, null)).isEqualTo("E0001");
        assertThat(CodeGenerator.next("D", 3, null)).isEqualTo("D001");
    }

    @Test
    void 최댓값에_1을_더하고_자릿수를_맞춘다() {
        assertThat(CodeGenerator.next("E", 4, "E0009")).isEqualTo("E0010");
        assertThat(CodeGenerator.next("P", 3, "P099")).isEqualTo("P100");
    }

    @Test
    void 자릿수를_모두_쓰면_예외를_던진다() {
        assertThatThrownBy(() -> CodeGenerator.next("D", 3, "D999"))
                .isInstanceOf(IllegalStateException.class);
    }
}
