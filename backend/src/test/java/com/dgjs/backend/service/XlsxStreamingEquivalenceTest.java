package com.dgjs.backend.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

// 스트리밍(SAX) 읽기가 POI DOM 방식(기존 구현과 동일한 변환 규칙)과 셀 단위로 같은 결과를 내는지 확인함
class XlsxStreamingEquivalenceTest {

    @TempDir
    Path tempDir;

    private static final String[] DATE_FORMATS = {
            "yyyy-mm-dd", "yyyy/mm/dd hh:mm", "m/d/yy", "d-mmm-yy", "hh:mm:ss", "yyyy\"년\" mm\"월\"", "0.00", "#,##0", "@"
    };

    @Test
    void 무작위로_만든_통합문서를_DOM과_같게_읽는다() throws IOException {
        for (long seed = 1; seed <= 12; seed++) {
            Path path = writeRandomWorkbook(seed);

            List<List<Object>> expected = readWithDom(path);
            List<List<Object>> actual = readWithStreaming(path);

            assertThat(actual).as("seed=" + seed).isEqualTo(expected);
            assertThat(actual.size()).as("seed=" + seed + " 행 수").isGreaterThan(50);
        }
    }

    @Test
    void 캐시된_결과값이_없는_수식은_빈_값으로_읽는다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Row row = wb.createSheet().createRow(0);
        row.createCell(0).setCellValue(5);
        row.createCell(1).setCellFormula("A1*2");
        row.createCell(2).setCellFormula("A1&\"x\"");
        Path path = tempDir.resolve("nocache.xlsx");
        try (OutputStream out = Files.newOutputStream(path)) {
            wb.write(out);
        }
        wb.close();

        // 기존 DOM 방식은 계산 결과가 저장되지 않은 수식을 0으로 읽었으나, 없는 값을 만들지 않도록 빈 값으로 읽음
        assertThat(readWithDom(path)).isEqualTo(List.of(java.util.Arrays.asList(5L, 0L, 0L)));
        assertThat(readWithStreaming(path)).isEqualTo(List.of(java.util.Arrays.asList(5L, null, null)));
    }

    private Path writeRandomWorkbook(long seed) throws IOException {
        Random random = new Random(seed);
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet();
        CellStyle[] styles = new CellStyle[DATE_FORMATS.length];
        for (int i = 0; i < styles.length; i++) {
            styles[i] = wb.createCellStyle();
            styles[i].setDataFormat(wb.getCreationHelper().createDataFormat().getFormat(DATE_FORMATS[i]));
        }

        int rowCount = 120 + random.nextInt(80);
        int columnCount = 4 + random.nextInt(8);
        for (int r = 0; r < rowCount; r++) {
            if (random.nextInt(15) == 0) continue; // 비어 있는 행
            Row row = sheet.createRow(r);
            for (int c = 0; c < columnCount; c++) {
                if (random.nextInt(6) == 0) continue; // 비어 있는 칸
                Cell cell = row.createCell(c);
                switch (random.nextInt(11)) {
                    case 0 -> cell.setCellValue(random.nextInt(2_000_000) - 1_000_000);
                    case 1 -> cell.setCellValue(random.nextDouble() * 1000);
                    case 2 -> cell.setCellValue(Math.round(random.nextDouble() * 1e6) / 100.0);
                    case 3 -> cell.setCellValue("텍스트 " + random.nextInt(500) + (random.nextBoolean() ? " " : ""));
                    case 4 -> cell.setCellValue(random.nextBoolean());
                    case 5 -> {
                        cell.setCellValue(LocalDate.of(2000 + random.nextInt(40), 1 + random.nextInt(12), 1 + random.nextInt(28)));
                        cell.setCellStyle(styles[random.nextInt(styles.length)]);
                    }
                    case 6 -> {
                        cell.setCellValue(LocalDateTime.of(2000 + random.nextInt(40), 1 + random.nextInt(12), 1 + random.nextInt(28),
                                random.nextInt(24), random.nextInt(60)));
                        cell.setCellStyle(styles[random.nextInt(styles.length)]);
                    }
                    case 7 -> cell.setCellFormula("SUM(A" + (r + 1) + ":B" + (r + 1) + ")");
                    case 8 -> cell.setCellFormula("\"합계=\"&A" + (r + 1));
                    case 9 -> cell.setCellFormula(random.nextBoolean() ? "1/0" : "A" + (r + 1) + ">100");
                    default -> cell.setCellValue(random.nextInt(100)); // 서식이 있는 숫자
                }
                if (random.nextInt(8) == 0 && cell.getCellType() == CellType.NUMERIC) {
                    cell.setCellStyle(styles[random.nextInt(styles.length)]);
                }
            }
        }
        wb.getCreationHelper().createFormulaEvaluator().evaluateAll();

        Path path = tempDir.resolve("random-" + seed + ".xlsx");
        try (OutputStream out = Files.newOutputStream(path)) {
            wb.write(out);
        }
        wb.close();
        return path;
    }

    private List<List<Object>> readWithStreaming(Path path) throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        SpreadsheetReader.read(path, (rowNumber, cells) -> {
            rows.add(new ArrayList<>(cells));
            return true;
        });
        return rows;
    }

    // 기존 구현(DatasetService.getCellValue)과 같은 규칙으로 DOM에서 읽음
    private List<List<Object>> readWithDom(Path path) throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(path.toFile(), null, true)) {
            for (Row row : wb.getSheetAt(0)) {
                int cellCount = Math.max(0, row.getLastCellNum());
                List<Object> cells = new ArrayList<>();
                for (int i = 0; i < cellCount; i++) {
                    cells.add(domValue(row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private Object domValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDateTime dt = cell.getLocalDateTimeCellValue();
                    yield dt.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? dt.toLocalDate().toString() : dt.toString();
                }
                double d = cell.getNumericCellValue();
                yield d == Math.rint(d) && Math.abs(d) < 1e15 ? (Object) (long) d : (Object) d;
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null;
        };
    }
}
