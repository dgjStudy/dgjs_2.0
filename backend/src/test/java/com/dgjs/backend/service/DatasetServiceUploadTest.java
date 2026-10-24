package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.dto.SchemaPreviewDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 비동기 적재 흐름(임시 파일, 청크 저장, 실패 처리)을 저장소를 목으로 대체해 DB 없이 확인함
class DatasetServiceUploadTest {

    @TempDir
    Path tempDir;

    private final DatasetMetaRepository metaRepository = mock(DatasetMetaRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final UploadTaskService taskService = new UploadTaskService();
    private final DatasetService service = new DatasetService(
            metaRepository,
            mock(DatasetColumnRepository.class),
            mock(DatasetRowRepository.class),
            taskService,
            new RowDataValidator(),
            new UploadFileStorage(),
            transactionTemplate,
            jdbcTemplate);

    private final AtomicReference<DatasetMeta> savedMeta = new AtomicReference<>();

    private void stubMeta() {
        when(metaRepository.save(any())).thenAnswer(invocation -> {
            DatasetMeta meta = invocation.getArgument(0);
            if (meta.getId() == null) {
                meta.setId(7L);
            }
            savedMeta.set(meta);
            return meta;
        });
        when(metaRepository.findById(7L)).thenAnswer(invocation -> Optional.ofNullable(savedMeta.get()));
    }

    private SchemaPreviewDto.FinalUploadRequest request() {
        SchemaPreviewDto.FinalUploadRequest request = new SchemaPreviewDto.FinalUploadRequest();
        request.setName("테스트");
        request.setColumns(List.of(column("col_0", "번호", 0), column("col_1", "이름", 1)));
        return request;
    }

    private SchemaPreviewDto.ColumnInfo column(String key, String name, int order) {
        return SchemaPreviewDto.ColumnInfo.builder().columnKey(key).columnName(name).dataType("STRING").sortOrder(order).build();
    }

    private Path xlsxWithRows(int rows) throws IOException {
        Path path = Files.createTempFile(tempDir, "upload-", ".tmp");
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100); OutputStream out = Files.newOutputStream(path)) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("번호");
            header.createCell(1).setCellValue("이름");
            for (int i = 1; i <= rows; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(i);
                row.createCell(1).setCellValue("이름" + i);
            }
            wb.write(out);
        }
        return path;
    }

    private Path textFile(String content) throws IOException {
        Path path = Files.createTempFile(tempDir, "upload-", ".tmp");
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    @Test
    void 엑셀_적재는_500행_단위로_저장하고_임시_파일을_지운다() throws IOException {
        stubMeta();
        Path file = xlsxWithRows(1200);
        taskService.initTask("t1");

        service.processAsyncUpload("t1", file, "data.xlsx", request());

        ArgumentCaptor<Integer> batchSizes = ArgumentCaptor.forClass(Integer.class);
        verify(jdbcTemplate, times(3)).batchUpdate(anyString(), any(java.util.Collection.class), batchSizes.capture(), any());
        assertThat(batchSizes.getAllValues()).containsExactly(500, 500, 200);
        assertThat(taskService.getProgress("t1").getStatus()).isEqualTo("COMPLETED");
        assertThat(taskService.getProgress("t1").getDatasetId()).isEqualTo(7L);
        assertThat(savedMeta.get().getStatus()).isEqualTo(DatasetMeta.STATUS_READY);
        assertThat(savedMeta.get().getLastLoadedAt()).isNotNull();
        assertThat(file).doesNotExist();
    }

    @Test
    void CSV_적재도_500행_단위로_저장하고_임시_파일을_지운다() throws IOException {
        stubMeta();
        StringBuilder csv = new StringBuilder("번호,이름\n");
        for (int i = 1; i <= 1100; i++) csv.append(i).append(",이름").append(i).append('\n');
        Path file = textFile(csv.toString());
        taskService.initTask("t1");

        service.processAsyncUpload("t1", file, "data.csv", request());

        ArgumentCaptor<Integer> batchSizes = ArgumentCaptor.forClass(Integer.class);
        verify(jdbcTemplate, times(3)).batchUpdate(anyString(), any(java.util.Collection.class), batchSizes.capture(), any());
        assertThat(batchSizes.getAllValues()).containsExactly(500, 500, 100);
        assertThat(taskService.getProgress("t1").getStatus()).isEqualTo("COMPLETED");
        assertThat(file).doesNotExist();
    }

    @Test
    void CSV_중간에_따옴표가_닫히지_않으면_원인을_알려주고_정리한다() throws IOException {
        stubMeta();
        StringBuilder csv = new StringBuilder("번호,이름\n");
        for (int i = 1; i <= 19; i++) csv.append(i).append(",이름").append(i).append('\n');
        csv.append("20,\"닫히지 않은 따옴표\n");
        for (int i = 21; i <= 40; i++) csv.append(i).append(",이름").append(i).append('\n');
        Path file = textFile(csv.toString());
        taskService.initTask("t1");

        service.processAsyncUpload("t1", file, "data.csv", request());

        SchemaPreviewDto.TaskProgressResponse progress = taskService.getProgress("t1");
        assertThat(progress.getStatus()).isEqualTo("FAILED");
        assertThat(progress.getMessage()).contains("CSV 형식이 올바르지 않습니다");
        verify(transactionTemplate).executeWithoutResult(any());
        verify(metaRepository, never()).findById(7L);
        assertThat(file).doesNotExist();
    }

    @Test
    void 엑셀이_아닌_파일은_이유를_알려주고_정리한다() throws IOException {
        stubMeta();
        Path file = textFile("그냥 텍스트");
        taskService.initTask("t1");

        service.processAsyncUpload("t1", file, "data.xlsx", request());

        SchemaPreviewDto.TaskProgressResponse progress = taskService.getProgress("t1");
        assertThat(progress.getStatus()).isEqualTo("FAILED");
        assertThat(progress.getMessage()).contains("엑셀 파일을 읽을 수 없습니다");
        verify(transactionTemplate).executeWithoutResult(any());
        assertThat(file).doesNotExist();
    }

    @Test
    void 적재_중_예상하지_못한_오류는_내부_정보를_숨긴다() throws IOException {
        stubMeta();
        when(jdbcTemplate.batchUpdate(anyString(), any(java.util.Collection.class), eq(500), any()))
                .thenThrow(new IllegalStateException("insert into dataset_row ... password"));
        Path file = xlsxWithRows(600);
        taskService.initTask("t1");

        service.processAsyncUpload("t1", file, "data.xlsx", request());

        SchemaPreviewDto.TaskProgressResponse progress = taskService.getProgress("t1");
        assertThat(progress.getStatus()).isEqualTo("FAILED");
        assertThat(progress.getMessage()).doesNotContain("insert", "password");
        assertThat(file).doesNotExist();
    }

    @Test
    void 진행률은_총_행_수를_알면_비율로_모르면_천천히_증가한다() {
        assertThat(DatasetService.progressPercent(0, 1000)).isEqualTo(40);
        assertThat(DatasetService.progressPercent(500, 1000)).isEqualTo(67);
        assertThat(DatasetService.progressPercent(1000, 1000)).isEqualTo(95);
        assertThat(DatasetService.progressPercent(5000, 1000)).isEqualTo(95);

        assertThat(DatasetService.progressPercent(0, 0)).isEqualTo(40);
        int at100k = DatasetService.progressPercent(100_000, 0);
        int at1m = DatasetService.progressPercent(1_000_000, 0);
        assertThat(at100k).isBetween(70, 80);
        assertThat(at1m).isGreaterThan(at100k).isLessThan(95);
    }
}
