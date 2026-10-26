package com.dgjs.backend.controller;

import com.dgjs.backend.dto.EmployeeDto;
import com.dgjs.backend.dto.ProjectDto;
import com.dgjs.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 목록 조회 (참여자 수 포함, 전체)")
    @GetMapping
    public ResponseEntity<List<ProjectDto.Response>> getProjects() {
        return ResponseEntity.ok(projectService.getAll());
    }

    @Operation(summary = "프로젝트 생성 (코드는 P001 형식으로 자동 부여)")
    @PostMapping
    public ResponseEntity<ProjectDto.Response> createProject(@RequestBody ProjectDto.Request request) {
        return ResponseEntity.ok(projectService.create(request));
    }

    @Operation(summary = "프로젝트명 수정")
    @PutMapping("/{projectCode}")
    public ResponseEntity<ProjectDto.Response> updateProject(
            @PathVariable("projectCode") String projectCode,
            @RequestBody ProjectDto.Request request) {
        return ResponseEntity.ok(projectService.update(projectCode, request));
    }

    @Operation(summary = "프로젝트 삭제 (참여자 연결도 함께 삭제)")
    @DeleteMapping("/{projectCode}")
    public ResponseEntity<Void> deleteProject(@PathVariable("projectCode") String projectCode) {
        projectService.delete(projectCode);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 참여자 목록 조회")
    @GetMapping("/{projectCode}/members")
    public ResponseEntity<List<EmployeeDto.Response>> getMembers(@PathVariable("projectCode") String projectCode) {
        return ResponseEntity.ok(projectService.getMembers(projectCode));
    }

    @Operation(summary = "프로젝트 참여자 추가 (활성 사원만 가능)")
    @PostMapping("/{projectCode}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable("projectCode") String projectCode,
            @RequestBody ProjectDto.MemberAddRequest request) {
        projectService.addMember(projectCode, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 참여자 제거")
    @DeleteMapping("/{projectCode}/members/{empNo}")
    public ResponseEntity<Void> removeMember(
            @PathVariable("projectCode") String projectCode,
            @PathVariable("empNo") String empNo) {
        projectService.removeMember(projectCode, empNo);
        return ResponseEntity.ok().build();
    }
}
