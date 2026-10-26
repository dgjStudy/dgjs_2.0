package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.repository.DepartmentRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialSeedServiceTest {

    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final InitialSeedService service = new InitialSeedService(
            employeeRepository, departmentRepository, passwordEncoder, new TempPasswordGenerator());

    @BeforeEach
    void stub() {
        when(employeeRepository.findMaxCode()).thenReturn(Optional.empty());
        when(employeeRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        when(departmentRepository.findByName("시스템관리")).thenReturn(Optional.empty());
        when(departmentRepository.findMaxCode()).thenReturn(Optional.empty());
        when(departmentRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
    }

    @Test
    void 사원이_없으면_초기_부서와_관리자를_만든다() {
        when(employeeRepository.count()).thenReturn(0L);

        Optional<InitialSeedService.SeedResult> result =
                service.seedIfEmpty("시스템관리", "관리자", "Admin@DGJS.local", "fixed-pass-1");

        ArgumentCaptor<Department> dept = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).saveAndFlush(dept.capture());
        assertThat(dept.getValue().getDeptCode()).isEqualTo("D001");

        ArgumentCaptor<Employee> admin = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).saveAndFlush(admin.capture());
        Employee saved = admin.getValue();
        assertThat(saved.getEmpNo()).isEqualTo("E0001");
        assertThat(saved.getEmail()).isEqualTo("admin@dgjs.local");
        assertThat(saved.getRole()).isEqualTo("ADMIN");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(passwordEncoder.matches("fixed-pass-1", saved.getPasswordHash())).isTrue();
        assertThat(saved.isMustChangePassword()).isFalse();
        assertThat(result).isPresent();
        assertThat(result.get().generatedPassword()).isNull();
    }

    @Test
    void 비밀번호를_설정하지_않으면_임의로_만들어_알려주고_변경을_요구한다() {
        when(employeeRepository.count()).thenReturn(0L);

        InitialSeedService.SeedResult result =
                service.seedIfEmpty("시스템관리", "관리자", "admin@dgjs.local", "").orElseThrow();

        ArgumentCaptor<Employee> admin = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).saveAndFlush(admin.capture());
        assertThat(result.generatedPassword()).hasSize(TempPasswordGenerator.LENGTH);
        assertThat(passwordEncoder.matches(result.generatedPassword(), admin.getValue().getPasswordHash())).isTrue();
        assertThat(admin.getValue().isMustChangePassword()).isTrue();
    }

    @Test
    void 이미_사원이_있으면_아무것도_만들지_않는다() {
        when(employeeRepository.count()).thenReturn(3L);

        assertThat(service.seedIfEmpty("시스템관리", "관리자", "admin@dgjs.local", "")).isEmpty();

        verify(employeeRepository, never()).saveAndFlush(any());
        verify(departmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void 같은_이름의_부서가_있으면_새로_만들지_않고_재사용한다() {
        when(employeeRepository.count()).thenReturn(0L);
        when(departmentRepository.findByName("시스템관리"))
                .thenReturn(Optional.of(TestFixtures.department("D005", "시스템관리")));

        service.seedIfEmpty("시스템관리", "관리자", "admin@dgjs.local", "pw-123456");

        verify(departmentRepository, never()).saveAndFlush(any());
    }
}
