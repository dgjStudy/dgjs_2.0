package com.dgjs.backend.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// 엑셀 파일의 첫 번째 시트를 행 단위로 읽음. xlsx는 스트리밍(SAX), xls는 기존 방식(DOM)임.
public final class SpreadsheetReader {

    public static final String UNREADABLE_MESSAGE = "엑셀 파일을 읽을 수 없습니다. 파일 형식과 암호 설정을 확인해 주세요.";

    private SpreadsheetReader() {
    }

    @FunctionalInterface
    public interface RowVisitor {

        // cells는 열 위치 기준 값 목록(빈 칸은 null)이고, false를 반환하면 읽기를 중단함
        boolean visit(int rowNumber, List<Object> cells) throws IOException;

        // 시트에 선언된 마지막 행 번호를 알 수 있을 때 호출됨 (진행률 계산용)
        default void totalRows(int total) {
        }
    }

    // 시트가 하나도 없으면 false를 반환함
    public static boolean read(Path file, RowVisitor visitor) throws IOException {
        byte[] magic = new byte[4];
        int length;
        try (InputStream in = Files.newInputStream(file)) {
            length = in.readNBytes(magic, 0, 4);
        }
        if (length == 4 && magic[0] == 'P' && magic[1] == 'K') {
            return XlsxRowReader.read(file, visitor);
        }
        if (length == 4 && (magic[0] & 0xFF) == 0xD0 && (magic[1] & 0xFF) == 0xCF
                && (magic[2] & 0xFF) == 0x11 && (magic[3] & 0xFF) == 0xE0) {
            return readLegacy(file, visitor);
        }
        throw new IllegalArgumentException(UNREADABLE_MESSAGE);
    }

    // xls(65,536행 제한)와 암호화된 파일은 메모리 부담이 작아 기존 DOM 방식을 유지함
    private static boolean readLegacy(Path file, RowVisitor visitor) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.toFile(), null, true)) {
            if (workbook.getNumberOfSheets() == 0) {
                return false;
            }
            Sheet sheet = workbook.getSheetAt(0);
            visitor.totalRows(sheet.getLastRowNum());
            for (Row row : sheet) {
                int cellCount = Math.max(0, row.getLastCellNum());
                List<Object> cells = new ArrayList<>(cellCount);
                for (int i = 0; i < cellCount; i++) {
                    cells.add(getCellValue(row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                }
                if (!visitor.visit(row.getRowNum() + 1, cells)) {
                    break;
                }
            }
            return true;
        }
    }

    private static Object getCellValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> numericCellValue(cell);
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null;
        };
    }

    private static Object numericCellValue(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            java.time.LocalDateTime dt = cell.getLocalDateTimeCellValue();
            return dt.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? dt.toLocalDate().toString() : dt.toString();
        }
        return plainNumber(cell.getNumericCellValue());
    }

    // 소수부가 없고 절댓값이 1e15 미만이면 long으로 변환함
    static Object plainNumber(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return (long) d;
        }
        return d;
    }
}
