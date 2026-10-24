package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetColumn;
import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.DatasetRow;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 최근 적재일 계산(행 추가 시 갱신, 값이 없으면 등록일로 대체)을 저장소를 목으로 대체해 DB 없이 확인함
class DatasetServiceLastLoadedTest {

    private final DatasetMetaRepository metaRepository = mock(DatasetMetaRepository.class);
    private final DatasetColumnRepository columnRepository = mock(DatasetColumnRepository.class);
    private final DatasetRowRepository rowRepository = mock(DatasetRowRepository.class);
    private final DatasetService service = new DatasetService(
            metaRepository,
            columnRepository,
            rowRepository,
            new UploadTaskService(),
            new RowDataValidator(),
            new UploadFileStorage(),
            mock(TransactionTemplate.class),
            mock(JdbcTemplate.class));

    private DatasetMeta meta(Long id, LocalDateTime createdAt, LocalDateTime lastLoadedAt) {
        return DatasetMeta.builder()
                .id(id)
                .name("테스트")
                .status(DatasetMeta.STATUS_READY)
                .createdAt(createdAt)
                .lastLoadedAt(lastLoadedAt)
                .build();
    }

    @Test
    void 행을_추가하면_최근_적재일이_갱신된다() throws Exception {
        DatasetMeta meta = meta(1L, LocalDateTime.of(2026, 1, 1, 0, 0), null);
        when(metaRepository.findById(1L)).thenReturn(Optional.of(meta));
        when(columnRepository.findByDatasetIdOrderBySortOrderAsc(1L)).thenReturn(List.of(
                DatasetColumn.builder().datasetId(1L).columnKey("col_0").columnName("이름").dataType("STRING").sortOrder(0).build()));
        when(rowRepository.save(any())).thenAnswer(invocation -> {
            DatasetRow row = invocation.getArgument(0);
            row.setId(10L);
            return row;
        });

        service.addRow(1L, Map.of("col_0", "값"));

        assertThat(meta.getLastLoadedAt()).isNotNull().isAfter(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void 목록은_최근_적재일이_없으면_등록일로_대체한다() {
        LocalDateTime created = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime loaded = LocalDateTime.of(2026, 5, 1, 9, 0);
        when(metaRepository.findAllReadyOrderByCreatedAtDesc()).thenReturn(List.of(
                meta(1L, created, null),
                meta(2L, created, loaded)));

        List<DatasetDto.SummaryResponse> result = service.getAllDatasets();

        assertThat(result).extracting(DatasetDto.SummaryResponse::getLastLoadedAt).containsExactly(created, loaded);
    }
}
