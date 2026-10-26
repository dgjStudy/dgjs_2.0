package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.domain.EmployeeRole;
import com.dgjs.backend.domain.EmployeeStatus;
import com.dgjs.backend.domain.JobTitle;
import com.dgjs.backend.dto.EmployeeDto;
import com.dgjs.backend.repository.DepartmentRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final String CODE_PREFIX = "E";
    private static final int CODE_WIDTH = 4;
    private static final int NAME_MAX_LENGTH = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TempPasswordGenerator tempPasswordGenerator;
    private final CodeRetryExecutor codeRetryExecutor;

    @Transactional(readOnly = true)
    public EmployeeDto.PagedResponse search(int page, int size, String keyword, String deptCode) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE), Sort.by("empNo"));
        Page<Employee> result = employeeRepository.search(
                keyword == null ? "" : keyword.trim(),
                deptCode == null ? "" : deptCode.trim(),
                pageable);
        return EmployeeDto.PagedResponse.builder()
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .employees(result.getContent().stream().map(EmployeeDto.Response::from).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeDto.Response get(String empNo) {
        return EmployeeDto.Response.from(find(empNo));
    }

    public EmployeeDto.CreatedResponse create(EmployeeDto.CreateRequest request) {
        String name = InputValidator.requireText(request.getName(), "이름", NAME_MAX_LENGTH);
        String email = InputValidator.normalizeEmail(request.getEmail());
        String jobTitle = JobTitle.fromLabel(request.getJobTitle()).getLabel();
        String role = EmployeeRole.from(request.getRole()).name();
        String deptCode = InputValidator.requireText(request.getDeptCode(), "부서", 10);
        if (employeeRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String tempPassword = tempPasswordGenerator.generate();
        String passwordHash = passwordEncoder.encode(tempPassword);

        EmployeeDto.Response created = codeRetryExecutor.execute(() -> {
            Department department = findDepartment(deptCode);
            Employee saved = employeeRepository.saveAndFlush(Employee.builder()
                    .empNo(CodeGenerator.next(CODE_PREFIX, CODE_WIDTH, employeeRepository.findMaxCode().orElse(null)))
                    .name(name)
                    .email(email)
                    .passwordHash(passwordHash)
                    .department(department)
                    .jobTitle(jobTitle)
                    .role(role)
                    .status(EmployeeStatus.ACTIVE.name())
                    .mustChangePassword(true)
                    .build());
            return EmployeeDto.Response.from(saved);
        });
        return EmployeeDto.CreatedResponse.builder().employee(created).tempPassword(tempPassword).build();
    }

    @Transactional
    public EmployeeDto.Response update(String empNo, EmployeeDto.UpdateRequest request) {
        Employee employee = find(empNo);
        String name = InputValidator.requireText(request.getName(), "이름", NAME_MAX_LENGTH);
        String email = InputValidator.normalizeEmail(request.getEmail());
        String jobTitle = JobTitle.fromLabel(request.getJobTitle()).getLabel();
        String role = EmployeeRole.from(request.getRole()).name();
        String status = EmployeeStatus.from(request.getStatus()).name();
        Department department = findDepartment(InputValidator.requireText(request.getDeptCode(), "부서", 10));

        if (employeeRepository.existsByEmailAndEmpNoNot(email, empNo)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        boolean wasActiveAdmin = isActiveAdmin(employee.getRole(), employee.getStatus());
        if (wasActiveAdmin && !isActiveAdmin(role, status)
                && employeeRepository.countByRoleAndStatus(EmployeeRole.ADMIN.name(), EmployeeStatus.ACTIVE.name()) <= 1) {
            throw new IllegalArgumentException("마지막 활성 관리자의 역할이나 상태는 바꿀 수 없습니다.");
        }

        employee.setName(name);
        employee.setEmail(email);
        employee.setDepartment(department);
        employee.setJobTitle(jobTitle);
        employee.setRole(role);
        employee.setStatus(status);
        return EmployeeDto.Response.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDto.PasswordResetResponse resetPassword(String empNo) {
        Employee employee = find(empNo);
        String tempPassword = tempPasswordGenerator.generate();
        employee.setPasswordHash(passwordEncoder.encode(tempPassword));
        employee.setMustChangePassword(true);
        employeeRepository.save(employee);
        return new EmployeeDto.PasswordResetResponse(tempPassword);
    }

    private boolean isActiveAdmin(String role, String status) {
        return EmployeeRole.ADMIN.name().equals(role) && EmployeeStatus.ACTIVE.name().equals(status);
    }

    private Employee find(String empNo) {
        return employeeRepository.findById(empNo)
                .orElseThrow(() -> new ResourceNotFoundException("사원을 찾을 수 없습니다. (사번: " + empNo + ")"));
    }

    private Department findDepartment(String deptCode) {
        return departmentRepository.findById(deptCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다. (코드: " + deptCode + ")"));
    }
}
