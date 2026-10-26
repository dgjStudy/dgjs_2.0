package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.domain.EmployeeRole;
import com.dgjs.backend.domain.EmployeeStatus;
import com.dgjs.backend.domain.JobTitle;
import com.dgjs.backend.repository.DepartmentRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 계정을 만들 관리자가 없는 순환을 끊기 위해 사원이 하나도 없을 때만 초기 부서와 관리자를 만듦
@Service
@RequiredArgsConstructor
public class InitialSeedService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TempPasswordGenerator tempPasswordGenerator;

    // generatedPassword는 비밀번호를 설정으로 받지 않아 임의 생성한 경우에만 값이 있음
    public record SeedResult(String empNo, String email, String generatedPassword) {
    }

    @Transactional
    public Optional<SeedResult> seedIfEmpty(String deptName, String adminName, String adminEmail, String adminPassword) {
        if (employeeRepository.count() > 0) {
            return Optional.empty();
        }

        String email = InputValidator.normalizeEmail(adminEmail);
        String name = InputValidator.requireText(adminName, "관리자 이름", 50);
        String departmentName = InputValidator.requireText(deptName, "초기 부서명", 100);

        boolean generated = adminPassword == null || adminPassword.isBlank();
        String password = generated ? tempPasswordGenerator.generate() : adminPassword;

        Department dept = departmentRepository.findByName(departmentName).orElseGet(() ->
                departmentRepository.saveAndFlush(Department.builder()
                        .deptCode(CodeGenerator.next("D", 3, departmentRepository.findMaxCode().orElse(null)))
                        .name(departmentName)
                        .build()));

        Employee admin = employeeRepository.saveAndFlush(Employee.builder()
                .empNo(CodeGenerator.next("E", 4, employeeRepository.findMaxCode().orElse(null)))
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .department(dept)
                .jobTitle(JobTitle.EXECUTIVE.getLabel())
                .role(EmployeeRole.ADMIN.name())
                .status(EmployeeStatus.ACTIVE.name())
                .mustChangePassword(generated)
                .build());

        return Optional.of(new SeedResult(admin.getEmpNo(), admin.getEmail(), generated ? password : null));
    }
}
