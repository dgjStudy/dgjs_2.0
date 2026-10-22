package com.dgjs.backend.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpreadsheetReaderTest {

    @TempDir
    Path tempDir;

    private Path write(Workbook workbook, String name) throws IOException {
        Path path = tempDir.resolve(name);
        try (OutputStream out = Files.newOutputStream(path)) {
            workbook.write(out);
        }
        workbook.close();
        return path;
    }

    // 행 번호를 키로 하는 읽기 결과
    private Map<Integer, List<Object>> readAll(Path path) throws IOException {
        Map<Integer, List<Object>> rows = new TreeMap<>();
        SpreadsheetReader.read(path, (rowNumber, cells) -> {
            rows.put(rowNumber, new ArrayList<>(cells));
            return true;
        });
        return rows;
    }

    private CellStyle dateStyle(Workbook wb, String format) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat(format));
        return style;
    }

    @Test
    void 값_종류별로_기존_변환_규칙대로_읽는다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet();
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("문자 ");
        row.createCell(1).setCellValue(1001);
        row.createCell(2).setCellValue(12.5);
        row.createCell(3).setCellValue(-7);
        row.createCell(4).setCellValue(1e15);
        row.createCell(5).setCellValue(true);
        Cell date = row.createCell(6);
        date.setCellValue(LocalDate.of(2026, 10, 13));
        date.setCellStyle(dateStyle(wb, "yyyy-mm-dd"));
        Cell dateTime = row.createCell(7);
        dateTime.setCellValue(LocalDateTime.of(2026, 10, 13, 9, 30));
        dateTime.setCellStyle(dateStyle(wb, "yyyy-mm-dd hh:mm"));
        row.createCell(8).setCellType(CellType.BLANK);
        Cell formula = row.createCell(9);
        formula.setCellFormula("B1*2");
        Cell textFormula = row.createCell(10);
        textFormula.setCellFormula("A1&\"!\"");
        Cell boolFormula = row.createCell(11);
        boolFormula.setCellFormula("B1>5");
        Cell errorFormula = row.createCell(12);
        errorFormula.setCellFormula("1/0");
        wb.getCreationHelper().createFormulaEvaluator().evaluateAll();

        List<Object> cells = readAll(write(wb, "types.xlsx")).get(1);

        assertThat(cells).containsExactly(
                "문자 ", 1001L, 12.5, -7L, 1.0E15, true, "2026-10-13", "2026-10-13T09:30",
                null, 2002L, "문자 !", true, null);
    }

    @Test
    void 비어_있는_행과_열은_위치를_유지한다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet();
        sheet.createRow(0).createCell(0).setCellValue("A");
        sheet.createRow(2).createCell(3).setCellValue("D3");
        sheet.createRow(5);

        Map<Integer, List<Object>> rows = readAll(write(wb, "sparse.xlsx"));

        assertThat(rows.keySet()).containsExactly(1, 3, 6);
        assertThat(rows.get(1)).containsExactly("A");
        assertThat(rows.get(3)).containsExactly(null, null, null, "D3");
        assertThat(rows.get(6)).isEmpty();
    }

    @Test
    void 값이_없어도_서식이_있는_칸은_열_개수에_포함한다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Row header = wb.createSheet().createRow(0);
        header.createCell(0).setCellValue("이름");
        header.createCell(1).setCellValue("나이");
        header.createCell(2).setCellStyle(dateStyle(wb, "yyyy-mm-dd"));

        assertThat(readAll(write(wb, "blankstyle.xlsx")).get(1)).containsExactly("이름", "나이", null);
    }

    @Test
    void 인라인_문자열도_읽는다() throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(10);
        Row row = wb.createSheet().createRow(0);
        row.createCell(0).setCellValue("인라인 문자열");
        row.createCell(1).setCellValue(42);

        assertThat(readAll(write(wb, "inline.xlsx")).get(1)).containsExactly("인라인 문자열", 42L);
    }

    @Test
    void 리치_텍스트는_일반_문자열로_읽는다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Row row = wb.createSheet().createRow(0);
        RichTextString rich = wb.getCreationHelper().createRichTextString("굵게 그리고 보통");
        Font bold = wb.createFont();
        bold.setBold(true);
        rich.applyFont(0, 2, bold);
        row.createCell(0).setCellValue(rich);

        assertThat(readAll(write(wb, "rich.xlsx")).get(1)).containsExactly("굵게 그리고 보통");
    }

    @Test
    void 날짜_체계가_1904이면_그에_맞게_변환한다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Cell cell = wb.createSheet().createRow(0).createCell(0);
        cell.setCellValue(366);
        cell.setCellStyle(dateStyle(wb, "yyyy-mm-dd"));
        Path normal = write(wb, "d1900.xlsx");
        Path path1904 = tempDir.resolve("d1904.xlsx");
        rewriteWorkbookXml(normal, path1904, "date1904=\"false\"", "date1904=\"1\"");

        Object value1900 = readAll(normal).get(1).get(0);
        Object value1904 = readAll(path1904).get(1).get(0);

        assertThat(value1900).isEqualTo("1900-12-31");
        assertThat(value1904).isEqualTo("1905-01-01");
    }

    private void rewriteWorkbookXml(Path source, Path target, String from, String to) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(source));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                byte[] data = in.readAllBytes();
                if (entry.getName().equals("xl/workbook.xml")) {
                    String xml = new String(data, StandardCharsets.UTF_8);
                    assertThat(xml).contains(from);
                    data = xml.replace(from, to).getBytes(StandardCharsets.UTF_8);
                }
                out.putNextEntry(new ZipEntry(entry.getName()));
                out.write(data);
                out.closeEntry();
            }
        }
    }

    @Test
    void 방문자가_false를_반환하면_거기서_읽기를_멈춘다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet();
        for (int i = 0; i < 100; i++) {
            sheet.createRow(i).createCell(0).setCellValue(i);
        }
        Path path = write(wb, "stop.xlsx");
        List<Integer> visited = new ArrayList<>();

        boolean hasSheet = SpreadsheetReader.read(path, (rowNumber, cells) -> {
            visited.add(rowNumber);
            return visited.size() < 3;
        });

        assertThat(hasSheet).isTrue();
        assertThat(visited).containsExactly(1, 2, 3);
    }

    @Test
    void 시트에_선언된_마지막_행_번호를_알려준다() throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet();
        for (int i = 0; i < 25; i++) {
            sheet.createRow(i).createCell(1).setCellValue(i);
        }
        int[] total = {0};

        SpreadsheetReader.read(write(wb, "dim.xlsx"), new SpreadsheetReader.RowVisitor() {
            @Override
            public boolean visit(int rowNumber, List<Object> cells) {
                return true;
            }

            @Override
            public void totalRows(int count) {
                total[0] = count;
            }
        });

        assertThat(total[0]).isEqualTo(25);
    }

    @Test
    void 구형_xls도_같은_규칙으로_읽는다() throws IOException {
        Workbook wb = new HSSFWorkbook();
        Row row = wb.createSheet().createRow(0);
        row.createCell(0).setCellValue("가나다");
        row.createCell(1).setCellValue(7);
        row.createCell(2).setCellValue(2.5);
        Cell date = row.createCell(3);
        date.setCellValue(LocalDate.of(2026, 10, 13));
        date.setCellStyle(dateStyle(wb, "yyyy-mm-dd"));

        assertThat(readAll(write(wb, "legacy.xls")).get(1)).containsExactly("가나다", 7L, 2.5, "2026-10-13");
    }

    @Test
    void 시트가_하나도_없으면_false를_반환한다() throws IOException {
        Path path = write(new XSSFWorkbook(), "nosheet.xlsx");

        assertThat(SpreadsheetReader.read(path, (rowNumber, cells) -> true)).isFalse();
    }

    @Test
    void 엑셀이_아닌_파일은_예외를_던진다() throws IOException {
        Path text = tempDir.resolve("a.xlsx");
        Files.writeString(text, "그냥 텍스트");
        Path brokenZip = tempDir.resolve("b.xlsx");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(brokenZip))) {
            out.putNextEntry(new ZipEntry("hello.txt"));
            out.write("hi".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        assertThatThrownBy(() -> SpreadsheetReader.read(text, (r, c) -> true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("엑셀 파일을 읽을 수 없습니다");
        assertThatThrownBy(() -> SpreadsheetReader.read(brokenZip, (r, c) -> true))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("엑셀 파일을 읽을 수 없습니다");
    }

    @Test
    void 행이_많아도_모두_읽는다() throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        Sheet sheet = wb.createSheet();
        for (int i = 0; i < 50_000; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(i);
            row.createCell(1).setCellValue("행 " + i);
        }
        Path path = write(wb, "many.xlsx");
        int[] count = {0};
        Object[] last = {null};

        SpreadsheetReader.read(path, (rowNumber, cells) -> {
            count[0]++;
            last[0] = cells.get(1);
            return true;
        });

        assertThat(count[0]).isEqualTo(50_000);
        assertThat(last[0]).isEqualTo("행 49999");
    }
}
