package com.dgjs.backend.controller;

import com.dgjs.backend.dto.DepartmentDto;
import com.dgjs.backend.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "부서 목록 조회 (소속 사원 수 포함, 전체)")
    @GetMapping
    public ResponseEntity<List<DepartmentDto.Response>> getDepartments() {
        return ResponseEntity.ok(departmentService.getAll());
    }

    @Operation(summary = "부서 생성 (코드는 D001 형식으로 자동 부여)")
    @PostMapping
    public ResponseEntity<DepartmentDto.Response> createDepartment(@RequestBody DepartmentDto.Request request) {
        return ResponseEntity.ok(departmentService.create(request));
    }

    @Operation(summary = "부서명 수정")
    @PutMapping("/{deptCode}")
    public ResponseEntity<DepartmentDto.Response> updateDepartment(
            @PathVariable("deptCode") String deptCode,
            @RequestBody DepartmentDto.Request request) {
        return ResponseEntity.ok(departmentService.update(deptCode, request));
    }

    @Operation(summary = "부서 삭제 (소속 사원이 있으면 거부)")
    @DeleteMapping("/{deptCode}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable("deptCode") String deptCode) {
        departmentService.delete(deptCode);
        return ResponseEntity.ok().build();
    }
}
