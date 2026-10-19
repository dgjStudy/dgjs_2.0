package com.dgjs.backend.service;

import com.dgjs.backend.dto.SchemaPreviewDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasetServiceTest {

    private final DatasetService service = new DatasetService(
            Mockito.mock(DatasetMetaRepository.class),
            Mockito.mock(DatasetColumnRepository.class),
            Mockito.mock(DatasetRowRepository.class),
            Mockito.mock(UploadTaskService.class),
            new RowDataValidator(),
            Mockito.mock(TransactionTemplate.class),
            Mockito.mock(JdbcTemplate.class));

    private SchemaPreviewDto.Response previewCsv(byte[] bytes) throws IOException {
        return service.previewAndValidate(new MockMultipartFile("file", "data.csv", "text/csv", bytes));
    }

    private SchemaPreviewDto.Response previewCsv(String content) throws IOException {
        return previewCsv(content.getBytes(StandardCharsets.UTF_8));
    }

    private List<String> columnNames(SchemaPreviewDto.Response response) {
        return response.getColumns().stream().map(SchemaPreviewDto.ColumnInfo::getColumnName).toList();
    }

    @Test
    void CSV_UTF8_헤더와_행을_읽고_숫자_컬럼을_추정한다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("이름,나이\n홍길동,30\n김철수,25\n");

        assertThat(response.isValid()).isTrue();
        assertThat(columnNames(response)).containsExactly("이름", "나이");
        assertThat(response.getColumns().get(0).getDataType()).isEqualTo("STRING");
        assertThat(response.getColumns().get(1).getDataType()).isEqualTo("NUMBER");
        assertThat(response.getPreviewRows()).hasSize(2);
        assertThat(response.getPreviewRows().get(0)).containsEntry("col_0", "홍길동").containsEntry("col_1", "30");
    }

    @Test
    void CSV_대문자_확장자도_CSV로_처리한다() throws IOException {
        SchemaPreviewDto.Response response = service.previewAndValidate(
                new MockMultipartFile("file", "DATA.CSV", "text/csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)));

        assertThat(columnNames(response)).containsExactly("a", "b");
    }

    @Test
    void CSV_CP949_한글을_깨지지_않게_읽는다() throws IOException {
        byte[] bytes = "이름,부서\n홍길동,영업팀\n".getBytes(Charset.forName("MS949"));

        SchemaPreviewDto.Response response = previewCsv(bytes);

        assertThat(columnNames(response)).containsExactly("이름", "부서");
        assertThat(response.getPreviewRows().get(0)).containsEntry("col_0", "홍길동").containsEntry("col_1", "영업팀");
    }

    @Test
    void CSV_BOM과_따옴표_헤더를_함께_처리한다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("﻿\"이름\",나이\n홍길동,30\n");

        assertThat(columnNames(response)).containsExactly("이름", "나이");
    }

    @Test
    void CSV_따옴표_안의_줄바꿈_쉼표_이스케이프를_처리한다() throws IOException {
        String csv = "이름,메모\n홍,\"첫줄\n둘째줄, 쉼표\"\n김,\"He said \"\"hi\"\"\"\n";

        SchemaPreviewDto.Response response = previewCsv(csv);

        assertThat(response.getPreviewRows()).hasSize(2);
        assertThat(response.getPreviewRows().get(0)).containsEntry("col_1", "첫줄\n둘째줄, 쉼표");
        assertThat(response.getPreviewRows().get(1)).containsEntry("col_1", "He said \"hi\"");
    }

    @Test
    void CSV_빈_줄은_건너뛴다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("a,b\n1,2\n\n3,4\n");

        assertThat(response.getPreviewRows()).hasSize(2);
    }

    @Test
    void CSV_값이_부족한_행은_빈_값으로_채운다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("a,b,c\n1,2\n");

        assertThat(response.getPreviewRows().get(0)).containsEntry("col_2", "");
    }

    @Test
    void CSV_빈_파일은_valid_false로_응답한다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getWarnings()).isNotEmpty();
    }

    @Test
    void CSV_헤더만_있으면_valid_false로_응답한다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("a,b\n");

        assertThat(response.isValid()).isFalse();
    }

    @Test
    void CSV_빈_헤더는_임시_이름을_붙이고_경고한다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("a,,c\n1,2,3\n");

        assertThat(response.isValid()).isTrue();
        assertThat(columnNames(response)).containsExactly("a", "Column_2", "c");
        assertThat(response.getWarnings()).hasSize(1);
    }

    @Test
    void CSV_중복_컬럼명은_차단하지_않고_경고도_없다() throws IOException {
        SchemaPreviewDto.Response response = previewCsv("이름,이름\n1,2\n");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void CSV_따옴표가_닫히지_않으면_400용_예외를_던진다() {
        assertThatThrownBy(() -> previewCsv("a,b\n1,\"닫히지 않음\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSV 형식");
    }

    private byte[] workbook(WorkbookWriter writer) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writer.write(wb, wb.createSheet("Sheet1"));
            wb.write(out);
            return out.toByteArray();
        }
    }

    private interface WorkbookWriter {
        void write(Workbook wb, Sheet sheet);
    }

    private SchemaPreviewDto.Response previewXlsx(byte[] bytes) throws IOException {
        return service.previewAndValidate(new MockMultipartFile("file", "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes));
    }

    @Test
    void 엑셀_정수는_long_소수는_double로_읽는다() throws IOException {
        byte[] bytes = workbook((wb, sheet) -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("정수");
            header.createCell(1).setCellValue("소수");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(1001);
            row.createCell(1).setCellValue(12.5);
        });

        SchemaPreviewDto.Response response = previewXlsx(bytes);

        assertThat(response.getPreviewRows().get(0)).containsEntry("col_0", 1001L).containsEntry("col_1", 12.5);
        assertThat(response.getColumns().get(0).getDataType()).isEqualTo("NUMBER");
        assertThat(response.getColumns().get(1).getDataType()).isEqualTo("NUMBER");
    }

    @Test
    void 엑셀_날짜는_ISO_형식으로_읽는다() throws IOException {
        byte[] bytes = workbook((wb, sheet) -> {
            CreationHelper helper = wb.getCreationHelper();
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
            CellStyle dateTimeStyle = wb.createCellStyle();
            dateTimeStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("날짜");
            header.createCell(1).setCellValue("일시");
            Row row = sheet.createRow(1);
            Cell date = row.createCell(0);
            date.setCellValue(LocalDate.of(2026, 10, 13));
            date.setCellStyle(dateStyle);
            Cell dateTime = row.createCell(1);
            dateTime.setCellValue(LocalDateTime.of(2026, 10, 13, 9, 30));
            dateTime.setCellStyle(dateTimeStyle);
        });

        SchemaPreviewDto.Response response = previewXlsx(bytes);

        assertThat(response.getPreviewRows().get(0))
                .containsEntry("col_0", "2026-10-13")
                .containsEntry("col_1", "2026-10-13T09:30");
    }

    @Test
    void 엑셀_수식은_수식_문자열이_아니라_캐시된_결과값으로_읽는다() throws IOException {
        byte[] bytes = workbook((wb, sheet) -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("값");
            header.createCell(1).setCellValue("수식");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(1001);
            Cell formula = row.createCell(1);
            formula.setCellFormula("A2*2");
            wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(formula);
        });

        SchemaPreviewDto.Response response = previewXlsx(bytes);

        assertThat(response.getPreviewRows().get(0)).containsEntry("col_1", 2002L);
    }

    @Test
    void 엑셀_불리언과_문자열을_읽는다() throws IOException {
        byte[] bytes = workbook((wb, sheet) -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("문자");
            header.createCell(1).setCellValue("참거짓");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("가나다");
            row.createCell(1).setCellValue(true);
        });

        SchemaPreviewDto.Response response = previewXlsx(bytes);

        assertThat(response.getPreviewRows().get(0)).containsEntry("col_0", "가나다").containsEntry("col_1", true);
    }

    @Test
    void 엑셀_중복_컬럼명은_차단하지_않는다() throws IOException {
        byte[] bytes = workbook((wb, sheet) -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름");
            header.createCell(1).setCellValue("이름");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("a");
            row.createCell(1).setCellValue("b");
        });

        SchemaPreviewDto.Response response = previewXlsx(bytes);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void 엑셀_행이_없으면_valid_false로_응답한다() throws IOException {
        SchemaPreviewDto.Response response = previewXlsx(workbook((wb, sheet) -> { }));

        assertThat(response.isValid()).isFalse();
    }

    @Test
    void 엑셀이_아닌_파일은_400용_예외를_던진다() {
        assertThatThrownBy(() -> previewXlsx("그냥 텍스트".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("엑셀 파일을 읽을 수 없습니다");
    }

    private SchemaPreviewDto.ColumnInfo col(String key, String name) {
        return SchemaPreviewDto.ColumnInfo.builder().columnKey(key).columnName(name).dataType("STRING").sortOrder(0).build();
    }

    private SchemaPreviewDto.FinalUploadRequest request(SchemaPreviewDto.ColumnInfo... columns) {
        SchemaPreviewDto.FinalUploadRequest request = new SchemaPreviewDto.FinalUploadRequest();
        request.setColumns(columns == null ? null : List.of(columns));
        return request;
    }

    @Test
    void 업로드_검증_정상_요청은_통과한다() {
        service.validateUploadRequest(request(col("col_0", "이름"), col("col_1", "나이")));
    }

    @Test
    void 업로드_검증_컬럼_목록이_없으면_거부한다() {
        SchemaPreviewDto.FinalUploadRequest nullColumns = new SchemaPreviewDto.FinalUploadRequest();

        assertThatThrownBy(() -> service.validateUploadRequest(nullColumns)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateUploadRequest(request())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 업로드_검증_빈_컬럼명은_거부한다() {
        assertThatThrownBy(() -> service.validateUploadRequest(request(col("col_0", "  "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비어");
        assertThatThrownBy(() -> service.validateUploadRequest(request(col("col_0", null))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 업로드_검증_공백만_다른_중복_컬럼명도_거부한다() {
        assertThatThrownBy(() -> service.validateUploadRequest(request(col("col_0", "이름"), col("col_1", " 이름 "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복");
    }

    @Test
    void 업로드_검증_컬럼_키가_비었거나_중복이면_거부한다() {
        assertThatThrownBy(() -> service.validateUploadRequest(request(col("", "이름"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateUploadRequest(request(col("col_0", "이름"), col("col_0", "나이"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
