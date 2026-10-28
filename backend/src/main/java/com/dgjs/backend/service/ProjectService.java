package com.dgjs.backend.service;

import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.domain.EmployeeStatus;
import com.dgjs.backend.domain.Project;
import com.dgjs.backend.domain.ProjectMember;
import com.dgjs.backend.dto.EmployeeDto;
import com.dgjs.backend.dto.ProjectDto;
import com.dgjs.backend.repository.CodeCount;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import com.dgjs.backend.repository.ProjectMemberRepository;
import com.dgjs.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String CODE_PREFIX = "P";
    private static final int CODE_WIDTH = 3;
    private static final int NAME_MAX_LENGTH = 100;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;
    private final DatasetMetaRepository datasetMetaRepository;
    private final DatasetAccessPolicy accessPolicy;
    private final CodeRetryExecutor codeRetryExecutor;

    @Transactional(readOnly = true)
    public List<ProjectDto.Response> getAll() {
        Map<String, Long> counts = countByProject();
        return projectRepository.findAllByOrderByProjectCodeAsc().stream()
                .map(p -> toResponse(p, counts.getOrDefault(p.getProjectCode(), 0L)))
                .collect(Collectors.toList());
    }

    // 데이터셋을 연결할 수 있는 프로젝트 목록임. ADMIN은 전체, 그 밖에는 참여 중인 프로젝트만
    @Transactional(readOnly = true)
    public List<ProjectDto.Response> getMine(EmployeePrincipal user) {
        if (accessPolicy.isAdmin(user)) {
            return getAll();
        }
        Set<String> codes = accessPolicy.myProjectCodes(user);
        return getAll().stream().filter(p -> codes.contains(p.getProjectCode())).collect(Collectors.toList());
    }

    public ProjectDto.Response create(ProjectDto.Request request) {
        String name = InputValidator.requireText(request.getName(), "프로젝트명", NAME_MAX_LENGTH);
        if (projectRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 프로젝트명입니다.");
        }
        return codeRetryExecutor.execute(() -> {
            String code = CodeGenerator.next(CODE_PREFIX, CODE_WIDTH, projectRepository.findMaxCode().orElse(null));
            Project saved = projectRepository.saveAndFlush(Project.builder().projectCode(code).name(name).build());
            return toResponse(saved, 0);
        });
    }

    @Transactional
    public ProjectDto.Response update(String projectCode, ProjectDto.Request request) {
        Project project = find(projectCode);
        String name = InputValidator.requireText(request.getName(), "프로젝트명", NAME_MAX_LENGTH);
        if (projectRepository.existsByNameAndProjectCodeNot(name, projectCode)) {
            throw new IllegalArgumentException("이미 존재하는 프로젝트명입니다.");
        }
        project.setName(name);
        Project saved = projectRepository.save(project);
        return toResponse(saved, countByProject().getOrDefault(projectCode, 0L));
    }

    @Transactional
    public void delete(String projectCode) {
        Project project = find(projectCode);
        datasetMetaRepository.clearProject(projectCode);
        memberRepository.deleteByProjectCode(projectCode);
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto.Response> getMembers(String projectCode) {
        find(projectCode);
        return memberRepository.findByProjectCode(projectCode).stream()
                .map(m -> EmployeeDto.Response.from(m.getEmployee()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addMember(String projectCode, ProjectDto.MemberAddRequest request) {
        Project project = find(projectCode);
        String empNo = InputValidator.requireText(request.getEmpNo(), "사번", 10);
        Employee employee = employeeRepository.findById(empNo)
                .orElseThrow(() -> new ResourceNotFoundException("사원을 찾을 수 없습니다. (사번: " + empNo + ")"));
        if (!EmployeeStatus.ACTIVE.name().equals(employee.getStatus())) {
            throw new IllegalArgumentException("비활성 사원은 프로젝트에 추가할 수 없습니다.");
        }
        if (memberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo(projectCode, empNo)) {
            throw new IllegalArgumentException("이미 프로젝트에 참여 중인 사원입니다.");
        }
        memberRepository.save(ProjectMember.builder().project(project).employee(employee).build());
    }

    @Transactional
    public void removeMember(String projectCode, String empNo) {
        ProjectMember member = memberRepository.findByProject_ProjectCodeAndEmployee_EmpNo(projectCode, empNo)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 참여자를 찾을 수 없습니다. (사번: " + empNo + ")"));
        memberRepository.delete(member);
    }

    private Map<String, Long> countByProject() {
        return memberRepository.countGroupByProjectCode().stream()
                .collect(Collectors.toMap(CodeCount::getCode, CodeCount::getCnt));
    }

    private Project find(String projectCode) {
        return projectRepository.findById(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트를 찾을 수 없습니다. (코드: " + projectCode + ")"));
    }

    private ProjectDto.Response toResponse(Project project, long memberCount) {
        return ProjectDto.Response.builder()
                .projectCode(project.getProjectCode())
                .name(project.getName())
                .memberCount(memberCount)
                .createdAt(project.getCreatedAt())
                .build();
    }
}
