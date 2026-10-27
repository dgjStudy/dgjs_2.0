package com.dgjs.backend.service;

import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public Authentication authenticate(String email, String password) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isEmpty() || password == null || password.isEmpty()) {
            throw new LoginFailedException();
        }
        if (loginAttemptLimiter.isLocked(normalized)) {
            throw new LoginLockedException();
        }
        try {
            Authentication result = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalized, password));
            loginAttemptLimiter.recordSuccess(normalized);
            return result;
        } catch (AuthenticationException e) {
            loginAttemptLimiter.recordFailure(normalized);
            throw new LoginFailedException();
        }
    }

    @Transactional
    public void changePassword(String empNo, String currentPassword, String newPassword) {
        Employee employee = employeeRepository.findById(empNo)
                .orElseThrow(() -> new ResourceNotFoundException("사원을 찾을 수 없습니다. (사번: " + empNo + ")"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, employee.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        PasswordPolicy.validate(newPassword, currentPassword, employee.getEmail());
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employee.setMustChangePassword(false);
        employeeRepository.save(employee);
    }
}
