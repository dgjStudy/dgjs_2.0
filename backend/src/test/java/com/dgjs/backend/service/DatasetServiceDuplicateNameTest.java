package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.Project;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.dto.SchemaPreviewDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import com.dgjs.backend.repository.ProjectMemberRepository;
import com.dgjs.backend.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 같은 프로젝트 안의 같은 이름(프로젝트 미지정끼리는 이름만) 데이터셋을 생성·이름 변경 경로에서 거부하는지 확인함
class DatasetServiceDuplicateNameTest {

    private final DatasetMetaRepository metaRepository = mock(DatasetMetaRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
    private final DatasetService service = new DatasetService(
            metaRepository, mock(DatasetColumnRepository.class), mock(DatasetRowRepository.class),
            new UploadTaskService(), new RowDataValidator(), new UploadFileStorage(),
            mock(TransactionTemplate.class), mock(JdbcTemplate.class),
            projectRepository, new DatasetAccessPolicy(memberRepository), TestFixtures.assembler());

    private final EmployeePrincipal admin = TestFixtures.principal("E0001", "ADMIN");
    private final Project p1 = Project.builder().projectCode("P001").name("사업1").build();

    private SchemaPreviewDto.FinalUploadRequest uploadRequest(String name, String projectCode) {
        return new SchemaPreviewDto.FinalUploadRequest(name, null, null, projectCode, List.of());
    }

    private DatasetDto.ManualCreateRequest manualRequest(String name, String projectCode) {
        return new DatasetDto.ManualCreateRequest(name, null, null, projectCode,
                List.of(new DatasetDto.ManualColumnDef("컬럼", "STRING")));
    }

    @Test
    void 같은_프로젝트에_같은_이름으로_업로드하면_거부된다() {
        when(metaRepository.existsByNameAndProject_ProjectCodeAndIdNot("월간 보고", "P001", -1L)).thenReturn(true);

        assertThatThrownBy(() -> service.checkUploadNameAvailable(uploadRequest("월간 보고", "P001"), "a.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 프로젝트에 같은 이름('월간 보고')");
    }

    @Test
    void 다른_프로젝트에서는_같은_이름으로_업로드할_수_있다() {
        when(metaRepository.existsByNameAndProject_ProjectCodeAndIdNot("월간 보고", "P001", -1L)).thenReturn(true);

        assertThatCode(() -> service.checkUploadNameAvailable(uploadRequest("월간 보고", "P002"), "a.csv"))
                .doesNotThrowAnyException();
    }

    @Test
    void 프로젝트_미지정끼리는_이름만_비교한다() {
        when(metaRepository.existsByNameAndProjectIsNullAndIdNot("월간 보고", -1L)).thenReturn(true);

        assertThatThrownBy(() -> service.checkUploadNameAvailable(uploadRequest("월간 보고", null), "a.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("프로젝트 미지정 데이터셋 중 같은 이름('월간 보고')");
        assertThatThrownBy(() -> service.checkUploadNameAvailable(uploadRequest("월간 보고", "  "), "a.csv"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이름이_비어_있으면_파일명으로_중복을_검사하고_앞뒤_공백은_무시한다() {
        when(metaRepository.existsByNameAndProjectIsNullAndIdNot("data.csv", -1L)).thenReturn(true);
        when(metaRepository.existsByNameAndProjectIsNullAndIdNot("공백 이름", -1L)).thenReturn(true);

        assertThatThrownBy(() -> service.checkUploadNameAvailable(uploadRequest(" ", null), "data.csv"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.checkUploadNameAvailable(uploadRequest("  공백 이름  ", null), "x.csv"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 수동_스키마_생성도_중복_이름이면_저장하지_않고_거부된다() {
        when(projectRepository.findById("P001")).thenReturn(Optional.of(p1));
        when(memberRepository.existsByProject_ProjectCodeAndEmployee_EmpNo("P001", "E0001")).thenReturn(true);
        when(metaRepository.existsByNameAndProject_ProjectCodeAndIdNot("월간 보고", "P001", -1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createManualDataset(manualRequest(" 월간 보고 ", "P001"), admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("월간 보고");
        verify(metaRepository, never()).save(any());
    }

    @Test
    void 이름을_바꿀_때_같은_프로젝트의_다른_데이터셋과_겹치면_거부된다() {
        DatasetMeta meta = DatasetMeta.builder().id(1L).name("원래 이름").ownerId("E0001").project(p1).build();
        when(metaRepository.findById(1L)).thenReturn(Optional.of(meta));
        when(metaRepository.existsByNameAndProject_ProjectCodeAndIdNot("새 이름", "P001", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateDatasetMeta(1L,
                new DatasetDto.MetaUpdateRequest("새 이름", null, null, null), admin))
                .isInstanceOf(IllegalArgumentException.class);
        verify(metaRepository, never()).save(any());
    }

    @Test
    void 프로젝트를_바꿔_다른_프로젝트의_같은_이름과_겹치면_거부된다() {
        DatasetMeta meta = DatasetMeta.builder().id(1L).name("월간 보고").ownerId("E0001").project(p1).build();
        when(metaRepository.findById(1L)).thenReturn(Optional.of(meta));
        when(metaRepository.existsByNameAndProjectIsNullAndIdNot("월간 보고", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateDatasetMeta(1L,
                new DatasetDto.MetaUpdateRequest(null, null, null, ""), admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("프로젝트 미지정");
    }

    @Test
    void 이름과_프로젝트가_그대로면_이미_중복된_데이터의_설명_수정은_막지_않는다() {
        DatasetMeta meta = DatasetMeta.builder().id(1L).name("월간 보고").ownerId("E0001").project(p1).build();
        when(metaRepository.findById(1L)).thenReturn(Optional.of(meta));
        when(metaRepository.existsByNameAndProject_ProjectCodeAndIdNot("월간 보고", "P001", 1L)).thenReturn(true);

        service.updateDatasetMeta(1L, new DatasetDto.MetaUpdateRequest("월간 보고", "새 설명", null, null), admin);

        assertThat(meta.getDescription()).isEqualTo("새 설명");
        verify(metaRepository).save(meta);
    }
}
