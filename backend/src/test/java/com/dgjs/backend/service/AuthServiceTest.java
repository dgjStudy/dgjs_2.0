package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter();
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthService service = new AuthService(authenticationManager, limiter, employeeRepository, passwordEncoder);

    private final Authentication success = UsernamePasswordAuthenticationToken.authenticated("principal", null, java.util.List.of());

    @Test
    void 이메일은_소문자로_바꿔_인증하고_성공하면_결과를_돌려준다() {
        when(authenticationManager.authenticate(any())).thenReturn(success);

        Authentication result = service.authenticate("  Admin@DGJS.local ", "pw-12345");

        assertThat(result).isSameAs(success);
        verify(authenticationManager).authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("admin@dgjs.local", "pw-12345"));
    }

    @Test
    void 인증이_실패하면_사유와_상관없이_같은_예외로_바꾼다() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("x"))
                .thenThrow(new DisabledException("y"));

        assertThatThrownBy(() -> service.authenticate("a@dgjs.local", "pw")).isInstanceOf(LoginFailedException.class);
        assertThatThrownBy(() -> service.authenticate("b@dgjs.local", "pw")).isInstanceOf(LoginFailedException.class);
    }

    @Test
    void 비어_있는_입력은_인증을_시도하지_않고_실패로_처리한다() {
        assertThatThrownBy(() -> service.authenticate("", "pw")).isInstanceOf(LoginFailedException.class);
        assertThatThrownBy(() -> service.authenticate("a@dgjs.local", "")).isInstanceOf(LoginFailedException.class);
        assertThatThrownBy(() -> service.authenticate(null, null)).isInstanceOf(LoginFailedException.class);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void 연속으로_실패하면_잠기고_잠긴_뒤에는_인증을_시도하지_않는다() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            assertThatThrownBy(() -> service.authenticate("a@dgjs.local", "pw")).isInstanceOf(LoginFailedException.class);
        }

        assertThatThrownBy(() -> service.authenticate("a@dgjs.local", "pw")).isInstanceOf(LoginLockedException.class);
        verify(authenticationManager, org.mockito.Mockito.times(LoginAttemptLimiter.MAX_FAILURES)).authenticate(any());
    }

    private Employee employee(String rawPassword) {
        Employee employee = Employee.builder()
                .empNo("E0001")
                .email("a@dgjs.local")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .department(Department.builder().deptCode("D001").name("부서").build())
                .mustChangePassword(true)
                .build();
        when(employeeRepository.findById("E0001")).thenReturn(Optional.of(employee));
        return employee;
    }

    @Test
    void 비밀번호를_바꾸면_해시가_바뀌고_변경_필요_상태가_해제된다() {
        Employee employee = employee("old-pass-1");

        service.changePassword("E0001", "old-pass-1", "new-pass-2");

        assertThat(passwordEncoder.matches("new-pass-2", employee.getPasswordHash())).isTrue();
        assertThat(employee.isMustChangePassword()).isFalse();
        verify(employeeRepository).save(employee);
    }

    @Test
    void 현재_비밀번호가_틀리면_바꾸지_않는다() {
        Employee employee = employee("old-pass-1");
        String before = employee.getPasswordHash();

        assertThatThrownBy(() -> service.changePassword("E0001", "wrong-pass-1", "new-pass-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호");

        assertThat(employee.getPasswordHash()).isEqualTo(before);
        assertThat(employee.isMustChangePassword()).isTrue();
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void 정책에_맞지_않는_새_비밀번호는_거부한다() {
        employee("old-pass-1");

        assertThatThrownBy(() -> service.changePassword("E0001", "old-pass-1", "short1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void 없는_사원은_찾을_수_없다() {
        when(employeeRepository.findById("E9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword("E9999", "a", "b"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
