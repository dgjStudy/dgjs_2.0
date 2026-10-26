package com.dgjs.backend.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeEnumsTest {

    @Test
    void 직급은_정해진_한글_값만_허용한다() {
        assertThat(JobTitle.fromLabel("대리")).isEqualTo(JobTitle.ASSOCIATE);
        assertThat(JobTitle.fromLabel(" 임원 ")).isEqualTo(JobTitle.EXECUTIVE);
        assertThatThrownBy(() -> JobTitle.fromLabel("병장")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JobTitle.fromLabel(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 역할은_ADMIN과_USER만_허용한다() {
        assertThat(EmployeeRole.from("ADMIN")).isEqualTo(EmployeeRole.ADMIN);
        assertThatThrownBy(() -> EmployeeRole.from("admin")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmployeeRole.from("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상태는_ACTIVE와_INACTIVE만_허용한다() {
        assertThat(EmployeeStatus.from("INACTIVE")).isEqualTo(EmployeeStatus.INACTIVE);
        assertThatThrownBy(() -> EmployeeStatus.from("DELETED")).isInstanceOf(IllegalArgumentException.class);
    }
}
