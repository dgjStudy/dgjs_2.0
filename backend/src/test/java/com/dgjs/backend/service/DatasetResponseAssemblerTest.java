package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.domain.Project;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.repository.EmployeeRepository;
import com.dgjs.backend.repository.ProjectMemberRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetResponseAssemblerTest {

    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
    private final DatasetResponseAssembler assembler =
            new DatasetResponseAssembler(employeeRepository, new DatasetAccessPolicy(memberRepository));

    private final Project project = Project.builder().projectCode("P001").name("지자체 사업").build();

    private DatasetMeta dataset(long id, String ownerId, Project p) {
        return DatasetMeta.builder().id(id).name("데이터" + id).ownerId(ownerId).project(p).build();
    }

    private Employee ownerEmployee() {
        return Employee.builder().empNo("E0002").name("홍길동").build();
    }

    @Test
    void 목록은_소유자_이름과_프로젝트를_채우고_사용자별_수정_삭제_여부를_계산한다() {
        when(employeeRepository.findAllById(Set.of("E0002"))).thenReturn(List.of(ownerEmployee()));
        when(memberRepository.findProjectCodesByEmpNo("E0003")).thenReturn(Set.of("P001"));
        List<DatasetMeta> metas = List.of(dataset(1, "E0002", project), dataset(2, "E0002", null));

        List<DatasetDto.SummaryResponse> result = assembler.summaries(
                metas, Map.of(1L, 10L), Map.of(1L, 3L), TestFixtures.principal("E0003", "USER"));

        assertThat(result.get(0).getOwnerName()).isEqualTo("홍길동");
        assertThat(result.get(0).getProjectCode()).isEqualTo("P001");
        assertThat(result.get(0).getProjectName()).isEqualTo("지자체 사업");
        assertThat(result.get(0).isCanEdit()).isTrue();
        assertThat(result.get(0).isCanDelete()).isFalse();
        assertThat(result.get(0).getRowCount()).isEqualTo(10L);
        assertThat(result.get(0).getColumnCount()).isEqualTo(3);
        assertThat(result.get(1).getProjectCode()).isNull();
        assertThat(result.get(1).isCanEdit()).isFalse();
    }

    @Test
    void 소유자와_관리자는_수정과_삭제가_모두_가능하다() {
        when(employeeRepository.findAllById(Set.of("E0002"))).thenReturn(List.of(ownerEmployee()));
        List<DatasetMeta> metas = List.of(dataset(1, "E0002", null));

        DatasetDto.SummaryResponse asOwner =
                assembler.summaries(metas, Map.of(), Map.of(), TestFixtures.principal("E0002", "USER")).get(0);
        DatasetDto.SummaryResponse asAdmin =
                assembler.summaries(metas, Map.of(), Map.of(), TestFixtures.principal("E0001", "ADMIN")).get(0);

        assertThat(asOwner.isCanEdit()).isTrue();
        assertThat(asOwner.isCanDelete()).isTrue();
        assertThat(asAdmin.isCanEdit()).isTrue();
        assertThat(asAdmin.isCanDelete()).isTrue();
    }

    @Test
    void 알_수_없는_소유자는_이름이_비고_빈_소유자는_조회하지_않는다() {
        when(employeeRepository.findAllById(Set.of("user_admin"))).thenReturn(List.of());

        DatasetDto.SummaryResponse unknown = assembler.summaries(
                List.of(dataset(1, "user_admin", null)), Map.of(), Map.of(), TestFixtures.principal("E0001", "ADMIN")).get(0);
        assertThat(unknown.getOwnerName()).isNull();

        assembler.summaries(List.of(dataset(2, null, null)), Map.of(), Map.of(), TestFixtures.principal("E0001", "ADMIN"));
        verify(employeeRepository, never()).findAllById(Set.of());
    }

    @Test
    void 관리자_목록_조회는_참여_프로젝트를_조회하지_않는다() {
        assembler.summaries(List.of(dataset(1, "E0002", project)), Map.of(), Map.of(), TestFixtures.principal("E0001", "ADMIN"));

        verify(memberRepository, never()).findProjectCodesByEmpNo(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 상세도_같은_정보를_채운다() {
        when(employeeRepository.findAllById(Set.of("E0002"))).thenReturn(List.of(ownerEmployee()));
        when(memberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo("P001", "E0003")).thenReturn(true);

        DatasetDto.DetailResponse detail = assembler.detail(
                dataset(1, "E0002", project), List.of(), TestFixtures.principal("E0003", "USER"));

        assertThat(detail.getOwnerName()).isEqualTo("홍길동");
        assertThat(detail.getProjectName()).isEqualTo("지자체 사업");
        assertThat(detail.isCanEdit()).isTrue();
        assertThat(detail.isCanDelete()).isFalse();
    }
}
