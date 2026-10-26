package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.dto.EmployeeDto;
import com.dgjs.backend.repository.DepartmentRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 저장소를 목으로 대체해 DB 없이 사원 서비스 규칙을 확인함
class EmployeeServiceTest {

    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmployeeService service = new EmployeeService(
            employeeRepository, departmentRepository, passwordEncoder,
            new TempPasswordGenerator(), TestFixtures.directExecutor());

    private final Department dept = TestFixtures.department("D001", "시스템관리");

    @BeforeEach
    void stub() {
        when(departmentRepository.findById("D001")).thenReturn(Optional.of(dept));
        when(employeeRepository.findMaxCode()).thenReturn(Optional.of("E0009"));
        when(employeeRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        when(employeeRepository.save(any())).thenAnswer(returnsFirstArg());
    }

    private EmployeeDto.CreateRequest createRequest() {
        return new EmployeeDto.CreateRequest("홍길동", " Hong@DGJS.com ", "D001", "대리", "USER");
    }

    @Test
    void 계정을_발급하면_사번을_부여하고_이메일은_소문자로_비밀번호는_해시로만_저장한다() {
        EmployeeDto.CreatedResponse response = service.create(createRequest());

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).saveAndFlush(saved.capture());
        Employee employee = saved.getValue();
        assertThat(employee.getEmpNo()).isEqualTo("E0010");
        assertThat(employee.getEmail()).isEqualTo("hong@dgjs.com");
        assertThat(employee.getStatus()).isEqualTo("ACTIVE");
        assertThat(employee.isMustChangePassword()).isTrue();
        assertThat(employee.getPasswordHash()).isNotEqualTo(response.getTempPassword());
        assertThat(passwordEncoder.matches(response.getTempPassword(), employee.getPasswordHash())).isTrue();
        assertThat(response.getEmployee().getEmpNo()).isEqualTo("E0010");
        assertThat(response.getEmployee().getDeptName()).isEqualTo("시스템관리");
    }

    @Test
    void 이미_사용_중인_이메일은_거부한다() {
        when(employeeRepository.existsByEmail("hong@dgjs.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
        verify(employeeRepository, never()).saveAndFlush(any());
    }

    @Test
    void 허용되지_않은_직급은_거부한다() {
        EmployeeDto.CreateRequest request = createRequest();
        request.setJobTitle("병장");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("직급");
    }

    @Test
    void 이메일_형식이_틀리면_거부한다() {
        EmployeeDto.CreateRequest request = createRequest();
        request.setEmail("not-an-email");

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 존재하지_않는_부서는_거부한다() {
        EmployeeDto.CreateRequest request = createRequest();
        request.setDeptCode("D999");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("부서");
    }

    private EmployeeDto.UpdateRequest updateRequest(String role, String status) {
        return new EmployeeDto.UpdateRequest("관리자", "admin@dgjs.local", "D001", "임원", role, status);
    }

    @Test
    void 마지막_활성_관리자의_역할은_바꿀_수_없다() {
        Employee admin = TestFixtures.employee("E0001", "ADMIN", "ACTIVE", dept);
        when(employeeRepository.findById("E0001")).thenReturn(Optional.of(admin));
        when(employeeRepository.countByRoleAndStatus("ADMIN", "ACTIVE")).thenReturn(1L);

        assertThatThrownBy(() -> service.update("E0001", updateRequest("USER", "ACTIVE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("마지막");
        assertThatThrownBy(() -> service.update("E0001", updateRequest("ADMIN", "INACTIVE")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 관리자가_둘_이상이면_한_명을_USER로_바꿀_수_있다() {
        Employee admin = TestFixtures.employee("E0001", "ADMIN", "ACTIVE", dept);
        when(employeeRepository.findById("E0001")).thenReturn(Optional.of(admin));
        when(employeeRepository.countByRoleAndStatus("ADMIN", "ACTIVE")).thenReturn(2L);

        EmployeeDto.Response response = service.update("E0001", updateRequest("USER", "ACTIVE"));

        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void 다른_사원의_이메일과_겹치면_수정을_거부한다() {
        Employee user = TestFixtures.employee("E0002", "USER", "ACTIVE", dept);
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(user));
        when(employeeRepository.existsByEmailAndEmpNoNot("admin@dgjs.local", "E0002")).thenReturn(true);

        assertThatThrownBy(() -> service.update("E0002", updateRequest("USER", "ACTIVE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
    }

    @Test
    void 비밀번호를_초기화하면_새_임시_비밀번호의_해시로_바뀌고_변경_필요_상태가_된다() {
        Employee user = TestFixtures.employee("E0002", "USER", "ACTIVE", dept);
        user.setMustChangePassword(false);
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(user));

        EmployeeDto.PasswordResetResponse response = service.resetPassword("E0002");

        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches(response.getTempPassword(), user.getPasswordHash())).isTrue();
    }

    @Test
    void 없는_사원은_찾을_수_없다() {
        when(employeeRepository.findById("E9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("E9999")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.resetPassword("E9999")).isInstanceOf(ResourceNotFoundException.class);
    }
}
