package com.dgjs.backend.controller;

import com.dgjs.backend.dto.EmployeeDto;
import com.dgjs.backend.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "사원 목록 조회 (이름·이메일 검색, 부서 필터, 페이징)")
    @GetMapping
    public ResponseEntity<EmployeeDto.PagedResponse> getEmployees(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "deptCode", defaultValue = "") String deptCode) {
        return ResponseEntity.ok(employeeService.search(page, size, keyword, deptCode));
    }

    @Operation(summary = "사원 상세 조회")
    @GetMapping("/{empNo}")
    public ResponseEntity<EmployeeDto.Response> getEmployee(@PathVariable("empNo") String empNo) {
        return ResponseEntity.ok(employeeService.get(empNo));
    }

    @Operation(summary = "사원 계정 발급 (사번 자동 부여, 임시 비밀번호는 응답에만 한 번 포함)")
    @PostMapping
    public ResponseEntity<EmployeeDto.CreatedResponse> createEmployee(@RequestBody EmployeeDto.CreateRequest request) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @Operation(summary = "사원 정보 수정 (이름, 이메일, 부서, 직급, 역할, 상태)")
    @PutMapping("/{empNo}")
    public ResponseEntity<EmployeeDto.Response> updateEmployee(
            @PathVariable("empNo") String empNo,
            @RequestBody EmployeeDto.UpdateRequest request) {
        return ResponseEntity.ok(employeeService.update(empNo, request));
    }

    @Operation(summary = "비밀번호 초기화 (새 임시 비밀번호는 응답에만 한 번 포함)")
    @PostMapping("/{empNo}/reset-password")
    public ResponseEntity<EmployeeDto.PasswordResetResponse> resetPassword(@PathVariable("empNo") String empNo) {
        return ResponseEntity.ok(employeeService.resetPassword(empNo));
    }
}
