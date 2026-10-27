package com.dgjs.backend.service;

import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeUserDetailsServiceTest {

    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final EmployeeUserDetailsService service = new EmployeeUserDetailsService(employeeRepository);

    private Employee employee(String role, String status) {
        return Employee.builder()
                .empNo("E0002").email("user@dgjs.local").passwordHash("hash")
                .role(role).status(status).mustChangePassword(false)
                .build();
    }

    @Test
    void 이메일을_소문자로_바꿔_찾고_역할을_권한으로_바꾼다() {
        when(employeeRepository.findByEmail("user@dgjs.local")).thenReturn(Optional.of(employee("ADMIN", "ACTIVE")));

        UserDetails details = service.loadUserByUsername(" User@DGJS.local ");

        assertThat(details.getUsername()).isEqualTo("user@dgjs.local");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void 비활성_사원은_사용할_수_없는_계정으로_다룬다() {
        when(employeeRepository.findByEmail("user@dgjs.local")).thenReturn(Optional.of(employee("USER", "INACTIVE")));

        assertThat(service.loadUserByUsername("user@dgjs.local").isEnabled()).isFalse();
    }

    @Test
    void 없는_이메일은_예외를_던진다() {
        when(employeeRepository.findByEmail("none@dgjs.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("none@dgjs.local"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
