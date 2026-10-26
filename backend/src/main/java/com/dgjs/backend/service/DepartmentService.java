package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.dto.DepartmentDto;
import com.dgjs.backend.repository.CodeCount;
import com.dgjs.backend.repository.DepartmentRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final String CODE_PREFIX = "D";
    private static final int CODE_WIDTH = 3;
    private static final int NAME_MAX_LENGTH = 100;

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CodeRetryExecutor codeRetryExecutor;

    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getAll() {
        Map<String, Long> counts = countByDept();
        return departmentRepository.findAllByOrderByDeptCodeAsc().stream()
                .map(d -> toResponse(d, counts.getOrDefault(d.getDeptCode(), 0L)))
                .collect(Collectors.toList());
    }

    public DepartmentDto.Response create(DepartmentDto.Request request) {
        String name = InputValidator.requireText(request.getName(), "부서명", NAME_MAX_LENGTH);
        if (departmentRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 부서명입니다.");
        }
        return codeRetryExecutor.execute(() -> {
            String code = CodeGenerator.next(CODE_PREFIX, CODE_WIDTH, departmentRepository.findMaxCode().orElse(null));
            Department saved = departmentRepository.saveAndFlush(Department.builder().deptCode(code).name(name).build());
            return toResponse(saved, 0);
        });
    }

    @Transactional
    public DepartmentDto.Response update(String deptCode, DepartmentDto.Request request) {
        Department department = find(deptCode);
        String name = InputValidator.requireText(request.getName(), "부서명", NAME_MAX_LENGTH);
        if (departmentRepository.existsByNameAndDeptCodeNot(name, deptCode)) {
            throw new IllegalArgumentException("이미 존재하는 부서명입니다.");
        }
        department.setName(name);
        Department saved = departmentRepository.save(department);
        return toResponse(saved, countByDept().getOrDefault(deptCode, 0L));
    }

    @Transactional
    public void delete(String deptCode) {
        Department department = find(deptCode);
        if (employeeRepository.existsByDepartment_DeptCode(deptCode)) {
            throw new IllegalArgumentException("소속 사원이 있는 부서는 삭제할 수 없습니다.");
        }
        departmentRepository.delete(department);
    }

    private Map<String, Long> countByDept() {
        return employeeRepository.countGroupByDeptCode().stream()
                .collect(Collectors.toMap(CodeCount::getCode, CodeCount::getCnt));
    }

    private Department find(String deptCode) {
        return departmentRepository.findById(deptCode)
                .orElseThrow(() -> new ResourceNotFoundException("부서를 찾을 수 없습니다. (코드: " + deptCode + ")"));
    }

    private DepartmentDto.Response toResponse(Department department, long employeeCount) {
        return DepartmentDto.Response.builder()
                .deptCode(department.getDeptCode())
                .name(department.getName())
                .employeeCount(employeeCount)
                .createdAt(department.getCreatedAt())
                .build();
    }
}
