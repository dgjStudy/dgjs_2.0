package com.dgjs.backend.service;

import org.apache.poi.util.XMLHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 시트 XML을 앞에서부터 읽으며 행 단위로 전달하므로 행 수와 무관하게 메모리 사용량이 일정함 (공유 문자열 표는 메모리에 적재됨)
final class XlsxRowReader {

    private static final Pattern DATE_1904 = Pattern.compile("date1904=\"(1|true)\"");

    private XlsxRowReader() {
    }

    static boolean read(Path file, SpreadsheetReader.RowVisitor visitor) throws IOException {
        try (OPCPackage pkg = OPCPackage.open(file.toFile(), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            if (!sheets.hasNext()) {
                return false;
            }
            SheetHandler handler = new SheetHandler(reader.getSharedStringsTable(), reader.getStylesTable(),
                    isDate1904(reader), visitor);
            try (InputStream sheet = sheets.next()) {
                XMLReader parser = XMLHelper.newXMLReader();
                parser.setContentHandler(handler);
                parser.parse(new InputSource(sheet));
            } catch (StopReading ignored) {
                // 방문자가 중단을 요청한 경우임
            }
            return true;
        } catch (NotOfficeXmlFileException | OpenXML4JException | SAXException | ParserConfigurationException e) {
            throw new IOException(SpreadsheetReader.UNREADABLE_MESSAGE, e);
        }
    }

    private static boolean isDate1904(XSSFReader reader) throws IOException, InvalidFormatException {
        try (InputStream in = reader.getWorkbookData()) {
            String xml = new String(in.readNBytes(64 * 1024), StandardCharsets.UTF_8);
            return DATE_1904.matcher(xml).find();
        }
    }

    private static final class StopReading extends RuntimeException {

        StopReading() {
            super(null, null, false, false);
        }
    }

    private static final class SheetHandler extends DefaultHandler {

        private final SharedStrings sharedStrings;
        private final StylesTable styles;
        private final boolean date1904;
        private final SpreadsheetReader.RowVisitor visitor;
        private final Map<Integer, Boolean> dateStyleCache = new HashMap<>();
        private final StringBuilder text = new StringBuilder();

        private List<Object> cells;
        private int rowNumber;
        private int column;
        private int nextColumn;
        private String cellType;
        private int styleIndex;
        private boolean hasValue;
        private String rawValue;
        private boolean inValue;
        private boolean inInline;
        private boolean inInlineText;

        SheetHandler(SharedStrings sharedStrings, StylesTable styles, boolean date1904, SpreadsheetReader.RowVisitor visitor) {
            this.sharedStrings = sharedStrings;
            this.styles = styles;
            this.date1904 = date1904;
            this.visitor = visitor;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            switch (localName) {
                case "dimension" -> reportTotalRows(attrs.getValue("ref"));
                case "row" -> {
                    String r = attrs.getValue("r");
                    rowNumber = r != null ? Integer.parseInt(r) : rowNumber + 1;
                    cells = new ArrayList<>();
                    nextColumn = 0;
                }
                case "c" -> {
                    String ref = attrs.getValue("r");
                    column = ref != null ? columnIndex(ref) : nextColumn;
                    cellType = attrs.getValue("t");
                    String s = attrs.getValue("s");
                    styleIndex = s != null ? Integer.parseInt(s) : 0;
                    hasValue = false;
                    rawValue = null;
                    text.setLength(0);
                }
                case "v" -> {
                    inValue = true;
                    text.setLength(0);
                }
                case "is" -> {
                    inInline = true;
                    hasValue = true;
                    text.setLength(0);
                }
                case "t" -> inInlineText = inInline;
                default -> {
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inValue || inInlineText) {
                text.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (localName) {
                case "v" -> {
                    inValue = false;
                    hasValue = true;
                    rawValue = text.toString();
                }
                case "t" -> inInlineText = false;
                case "is" -> {
                    inInline = false;
                    rawValue = text.toString();
                }
                case "c" -> {
                    putCell(column, hasValue ? convert(cellType, rawValue, styleIndex) : null);
                    nextColumn = column + 1;
                }
                case "row" -> {
                    try {
                        if (!visitor.visit(rowNumber, cells)) {
                            throw new StopReading();
                        }
                    } catch (IOException e) {
                        throw new SAXException(e);
                    }
                }
                default -> {
                }
            }
        }

        private void putCell(int index, Object value) {
            while (cells.size() <= index) {
                cells.add(null);
            }
            cells.set(index, value);
        }

        private Object convert(String type, String raw, int style) {
            String t = type == null ? "n" : type;
            switch (t) {
                case "s":
                    return sharedStrings.getItemAt(Integer.parseInt(raw.trim())).getString();
                case "str", "inlineStr":
                    return raw;
                case "b":
                    return "1".equals(raw.trim());
                case "e":
                    return null;
                default:
                    if (raw.isBlank()) {
                        return null;
                    }
                    double d = Double.parseDouble(raw.trim());
                    if (isDateStyle(style, d)) {
                        LocalDateTime dt = DateUtil.getLocalDateTime(d, date1904);
                        return dt.toLocalTime().equals(LocalTime.MIDNIGHT) ? dt.toLocalDate().toString() : dt.toString();
                    }
                    return SpreadsheetReader.plainNumber(d);
            }
        }

        private boolean isDateStyle(int style, double value) {
            if (!DateUtil.isValidExcelDate(value)) {
                return false;
            }
            return dateStyleCache.computeIfAbsent(style, key -> {
                XSSFCellStyle cellStyle = styles.getStyleAt(key);
                return cellStyle != null && DateUtil.isADateFormat(cellStyle.getDataFormat(), cellStyle.getDataFormatString());
            });
        }

        // "A1:J300001" 형식에서 마지막 행 번호를 꺼냄
        private void reportTotalRows(String ref) {
            if (ref == null) return;
            Matcher m = Pattern.compile("(\\d+)$").matcher(ref);
            if (m.find()) {
                visitor.totalRows(Integer.parseInt(m.group(1)));
            }
        }

        // "AB12" -> 27 (0부터 시작)
        private static int columnIndex(String ref) {
            int index = 0;
            for (int i = 0; i < ref.length(); i++) {
                char c = ref.charAt(i);
                if (c < 'A' || c > 'Z') break;
                index = index * 26 + (c - 'A' + 1);
            }
            return index - 1;
        }
    }
}
