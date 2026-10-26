package com.dgjs.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TempPasswordGeneratorTest {

    private final TempPasswordGenerator generator = new TempPasswordGenerator();

    @Test
    void 정해진_길이이고_대문자_소문자_숫자를_모두_포함한다() {
        for (int i = 0; i < 50; i++) {
            String password = generator.generate();
            assertThat(password).hasSize(TempPasswordGenerator.LENGTH);
            assertThat(password).matches(".*[A-Z].*").matches(".*[a-z].*").matches(".*[0-9].*");
        }
    }

    @Test
    void 헷갈리는_문자는_쓰지_않는다() {
        for (int i = 0; i < 50; i++) {
            assertThat(generator.generate()).doesNotContainPattern("[0OIl1]");
        }
    }

    @Test
    void 호출할_때마다_다른_값을_만든다() {
        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }
}
