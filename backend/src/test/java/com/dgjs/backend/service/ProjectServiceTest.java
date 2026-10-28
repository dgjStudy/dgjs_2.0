package com.dgjs.backend.service;

import com.dgjs.backend.domain.Department;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.domain.Project;
import com.dgjs.backend.domain.ProjectMember;
import com.dgjs.backend.dto.ProjectDto;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.EmployeeRepository;
import com.dgjs.backend.repository.ProjectMemberRepository;
import com.dgjs.backend.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final DatasetMetaRepository datasetMetaRepository = mock(DatasetMetaRepository.class);
    private final ProjectService service =
            new ProjectService(projectRepository, memberRepository, employeeRepository, datasetMetaRepository,
                    new DatasetAccessPolicy(memberRepository), TestFixtures.directExecutor());

    private final Project project = Project.builder().projectCode("P001").name("사업").build();
    private final Department dept = Department.builder().deptCode("D001").name("개발팀").build();

    @Test
    void 프로젝트_코드는_P001부터_일련번호로_부여한다() {
        when(projectRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        when(projectRepository.findMaxCode()).thenReturn(Optional.of("P004"));

        assertThat(service.create(new ProjectDto.Request("신규 사업")).getProjectCode()).isEqualTo("P005");
    }

    @Test
    void 프로젝트명이_중복이면_거부한다() {
        when(projectRepository.existsByName("사업")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ProjectDto.Request("사업"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 활성_사원은_참여자로_추가할_수_있다() {
        Employee employee = TestFixtures.employee("E0002", "USER", "ACTIVE", dept);
        when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(employee));

        service.addMember("P001", new ProjectDto.MemberAddRequest("E0002"));

        verify(memberRepository).save(any(ProjectMember.class));
    }

    @Test
    void 이미_참여_중인_사원은_다시_추가할_수_없다() {
        Employee employee = TestFixtures.employee("E0002", "USER", "ACTIVE", dept);
        when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
        when(employeeRepository.findById("E0002")).thenReturn(Optional.of(employee));
        when(memberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo("P001", "E0002")).thenReturn(true);

        assertThatThrownBy(() -> service.addMember("P001", new ProjectDto.MemberAddRequest("E0002")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미");
        verify(memberRepository, never()).save(any());
    }

    @Test
    void 비활성_사원은_참여자로_추가할_수_없다() {
        Employee employee = TestFixtures.employee("E0003", "USER", "INACTIVE", dept);
        when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
        when(employeeRepository.findById("E0003")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.addMember("P001", new ProjectDto.MemberAddRequest("E0003")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비활성");
    }

    @Test
    void 없는_사원이나_프로젝트는_찾을_수_없다() {
        when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
        when(employeeRepository.findById("E9999")).thenReturn(Optional.empty());
        when(projectRepository.findById("P404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMember("P001", new ProjectDto.MemberAddRequest("E9999")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getMembers("P404")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 프로젝트를_삭제하면_데이터셋_연결과_참여자_연결을_먼저_지운다() {
        when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

        service.delete("P001");

        var order = inOrder(datasetMetaRepository, memberRepository, projectRepository);
        order.verify(datasetMetaRepository).clearProject("P001");
        order.verify(memberRepository).deleteByProjectCode("P001");
        order.verify(projectRepository).delete(project);
    }

    @Test
    void 연결할_프로젝트_목록은_관리자는_전체_일반_사용자는_참여한_것만_내려준다() {
        when(projectRepository.findAllByOrderByProjectCodeAsc()).thenReturn(java.util.List.of(
                project, Project.builder().projectCode("P002").name("다른 사업").build()));
        when(memberRepository.findProjectCodesByEmpNo("E0002")).thenReturn(java.util.Set.of("P002"));

        assertThat(service.getMine(TestFixtures.principal("E0001", "ADMIN")))
                .extracting(ProjectDto.Response::getProjectCode).containsExactly("P001", "P002");
        assertThat(service.getMine(TestFixtures.principal("E0002", "USER")))
                .extracting(ProjectDto.Response::getProjectCode).containsExactly("P002");
    }

    @Test
    void 참여하지_않은_사원을_제거하려_하면_찾을_수_없다() {
        when(memberRepository.findByProject_ProjectCodeAndEmployee_EmpNo("P001", "E0002")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember("P001", "E0002")).isInstanceOf(ResourceNotFoundException.class);
    }
}
