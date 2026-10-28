package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.Project;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import com.dgjs.backend.repository.ProjectMemberRepository;
import com.dgjs.backend.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 데이터셋 서비스가 소유자·프로젝트 규칙을 모든 수정 경로에 적용하는지 저장소를 목으로 대체해 확인함
class DatasetServiceAccessTest {

    private final DatasetMetaRepository metaRepository = mock(DatasetMetaRepository.class);
    private final DatasetColumnRepository columnRepository = mock(DatasetColumnRepository.class);
    private final DatasetRowRepository rowRepository = mock(DatasetRowRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
    private final DatasetAccessPolicy policy = new DatasetAccessPolicy(memberRepository);
    private final DatasetService service = new DatasetService(
            metaRepository, columnRepository, rowRepository,
            new UploadTaskService(), new RowDataValidator(), new UploadFileStorage(),
            mock(TransactionTemplate.class), mock(JdbcTemplate.class),
            projectRepository, policy, TestFixtures.assembler());

    private final EmployeePrincipal admin = TestFixtures.principal("E0001", "ADMIN");
    private final EmployeePrincipal owner = TestFixtures.principal("E0002", "USER");
    private final EmployeePrincipal member = TestFixtures.principal("E0003", "USER");
    private final EmployeePrincipal outsider = TestFixtures.principal("E0004", "USER");

    private final Project p1 = Project.builder().projectCode("P001").name("사업1").build();
    private final Project p2 = Project.builder().projectCode("P002").name("사업2").build();

    private DatasetMeta dataset(Project project) {
        DatasetMeta meta = DatasetMeta.builder().id(1L).name("데이터").ownerId("E0002").project(project).build();
        when(metaRepository.findById(1L)).thenReturn(Optional.of(meta));
        return meta;
    }

    private void join(String projectCode, String empNo) {
        when(memberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo(projectCode, empNo)).thenReturn(true);
        when(memberRepository.findProjectCodesByEmpNo(empNo)).thenReturn(java.util.Set.of(projectCode));
    }

    private DatasetDto.MetaUpdateRequest nameChange() {
        return new DatasetDto.MetaUpdateRequest("새 이름", null, null, null);
    }

    @Test
    void 권한이_없는_사용자의_메타_수정은_거부된다() {
        dataset(p1);

        assertThatThrownBy(() -> service.updateDatasetMeta(1L, nameChange(), outsider))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(metaRepository, never()).save(any());
    }

    @Test
    void 프로젝트_참여자는_메타를_수정할_수_있다() {
        DatasetMeta meta = dataset(p1);
        join("P001", "E0003");

        service.updateDatasetMeta(1L, nameChange(), member);

        assertThat(meta.getName()).isEqualTo("새 이름");
        verify(metaRepository).save(meta);
    }

    @Test
    void 권한이_없는_사용자의_행_추가_수정_삭제는_거부된다() {
        dataset(p1);
        Map<String, Object> row = Map.of("col_0", "값");

        assertThatThrownBy(() -> service.addRow(1L, row, outsider)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.updateRow(1L, 5L, row, outsider)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.deleteRow(1L, 5L, outsider)).isInstanceOf(ForbiddenOperationException.class);
        verify(rowRepository, never()).save(any());
        verify(rowRepository, never()).delete(any());
    }

    @Test
    void 프로젝트_참여자도_행_삭제는_할_수_있다() {
        dataset(p1);
        join("P001", "E0003");
        com.dgjs.backend.domain.DatasetRow row = com.dgjs.backend.domain.DatasetRow.builder().id(5L).datasetId(1L).rowIndex(0).dataJson("{}").build();
        when(rowRepository.findByIdAndDatasetId(5L, 1L)).thenReturn(Optional.of(row));

        service.deleteRow(1L, 5L, member);

        verify(rowRepository).delete(row);
    }

    @Test
    void 데이터셋_삭제는_프로젝트_참여자도_거부되고_소유자와_관리자만_된다() {
        dataset(p1);
        join("P001", "E0003");

        assertThatThrownBy(() -> service.deleteDataset(1L, member)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.deleteDataset(1L, outsider)).isInstanceOf(ForbiddenOperationException.class);
        verify(metaRepository, never()).deleteById(any());

        service.deleteDataset(1L, owner);
        service.deleteDataset(1L, admin);
        verify(metaRepository, org.mockito.Mockito.times(2)).deleteById(1L);
    }

    @Test
    void 없는_데이터셋은_권한보다_먼저_찾을_수_없다고_응답한다() {
        when(metaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDataset(9L, outsider)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteRow(9L, 1L, outsider)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 프로젝트_연결_변경은_참여자_수정_권한만으로는_거부된다() {
        dataset(p1);
        join("P001", "E0003");
        join("P002", "E0003");

        assertThatThrownBy(() -> service.updateDatasetMeta(1L,
                new DatasetDto.MetaUpdateRequest(null, null, null, ""), member))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("프로젝트 연결");
    }

    @Test
    void 소유자는_참여한_프로젝트로_연결을_바꾸거나_해제할_수_있다() {
        DatasetMeta meta = dataset(p1);
        join("P002", "E0002");
        when(projectRepository.findById("P002")).thenReturn(Optional.of(p2));

        service.updateDatasetMeta(1L, new DatasetDto.MetaUpdateRequest(null, null, null, "P002"), owner);
        assertThat(meta.getProject()).isSameAs(p2);

        service.updateDatasetMeta(1L, new DatasetDto.MetaUpdateRequest(null, null, null, ""), owner);
        assertThat(meta.getProject()).isNull();
    }

    @Test
    void 참여하지_않은_프로젝트로의_연결은_소유자도_거부된다() {
        DatasetMeta meta = dataset(p1);
        when(projectRepository.findById("P002")).thenReturn(Optional.of(p2));

        assertThatThrownBy(() -> service.updateDatasetMeta(1L,
                new DatasetDto.MetaUpdateRequest(null, null, null, "P002"), owner))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(meta.getProject()).isSameAs(p1);
    }

    @Test
    void 관리자는_어느_프로젝트로든_연결할_수_있다() {
        DatasetMeta meta = dataset(null);
        when(projectRepository.findById("P002")).thenReturn(Optional.of(p2));

        service.updateDatasetMeta(1L, new DatasetDto.MetaUpdateRequest(null, null, null, "P002"), admin);

        assertThat(meta.getProject()).isSameAs(p2);
    }

    @Test
    void 연결_값을_보내지_않으면_프로젝트는_그대로다() {
        DatasetMeta meta = dataset(p1);
        join("P001", "E0003");

        service.updateDatasetMeta(1L, nameChange(), member);

        assertThat(meta.getProject()).isSameAs(p1);
    }

    @Test
    void 존재하지_않는_프로젝트로_연결하면_400용_예외를_던진다() {
        dataset(null);
        when(projectRepository.findById("P999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDatasetMeta(1L,
                new DatasetDto.MetaUpdateRequest(null, null, null, "P999"), admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("프로젝트");
    }

    private DatasetDto.ManualCreateRequest manualRequest(String projectCode) {
        return new DatasetDto.ManualCreateRequest("새 데이터셋", null, "기타", projectCode,
                List.of(new DatasetDto.ManualColumnDef("이름", "STRING")));
    }

    @Test
    void 수동_생성의_소유자는_요청이_아니라_로그인_사용자다() {
        when(metaRepository.save(any())).thenAnswer(invocation -> {
            DatasetMeta meta = invocation.getArgument(0);
            meta.setId(7L);
            return meta;
        });

        service.createManualDataset(manualRequest(null), owner);

        ArgumentCaptor<DatasetMeta> saved = ArgumentCaptor.forClass(DatasetMeta.class);
        verify(metaRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo("E0002");
        assertThat(saved.getValue().getProject()).isNull();
    }

    @Test
    void 수동_생성에서_참여한_프로젝트는_연결하고_참여하지_않은_프로젝트는_거부한다() {
        when(metaRepository.save(any())).thenAnswer(invocation -> {
            DatasetMeta meta = invocation.getArgument(0);
            meta.setId(7L);
            return meta;
        });
        when(projectRepository.findById("P001")).thenReturn(Optional.of(p1));
        join("P001", "E0003");

        service.createManualDataset(manualRequest("P001"), member);
        ArgumentCaptor<DatasetMeta> saved = ArgumentCaptor.forClass(DatasetMeta.class);
        verify(metaRepository).save(saved.capture());
        assertThat(saved.getValue().getProject()).isSameAs(p1);

        assertThatThrownBy(() -> service.createManualDataset(manualRequest("P001"), outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void 업로드_전_프로젝트_확인은_비어_있으면_통과하고_참여하지_않으면_거부한다() {
        when(projectRepository.findById("P001")).thenReturn(Optional.of(p1));
        join("P001", "E0003");

        assertThatCode(() -> service.checkProjectAttachable(outsider, null)).doesNotThrowAnyException();
        assertThatCode(() -> service.checkProjectAttachable(outsider, "  ")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkProjectAttachable(member, "P001")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.checkProjectAttachable(outsider, "P001"))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
