package com.dgjs.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dgjs.backend.domain.DatasetColumn;
import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.DatasetRow;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.dto.SchemaPreviewDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetCount;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetMetaRepository metaRepository;
    private final DatasetColumnRepository columnRepository;
    private final DatasetRowRepository rowRepository;
    private final UploadTaskService uploadTaskService;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<DatasetDto.SummaryResponse> getAllDatasets() {
        List<DatasetMeta> metaList = metaRepository.findAllReadyOrderByCreatedAtDesc();
        if (metaList.isEmpty()) {
            return List.of();
        }

        List<Long> ids = metaList.stream().map(DatasetMeta::getId).collect(Collectors.toList());
        Map<Long, Long> rowCounts = rowRepository.countGroupByDatasetId(ids).stream()
                .collect(Collectors.toMap(DatasetCount::getDatasetId, DatasetCount::getCnt));
        Map<Long, Long> columnCounts = columnRepository.countGroupByDatasetId(ids).stream()
                .collect(Collectors.toMap(DatasetCount::getDatasetId, DatasetCount::getCnt));

        return metaList.stream().map(meta -> DatasetDto.SummaryResponse.builder()
                .id(meta.getId())
                .name(meta.getName())
                .description(meta.getDescription())
                .category(meta.getCategory())
                .ownerId(meta.getOwnerId())
                .orgId(meta.getOrgId())
                .createdAt(meta.getCreatedAt())
                .rowCount(rowCounts.getOrDefault(meta.getId(), 0L))
                .columnCount(columnCounts.getOrDefault(meta.getId(), 0L).intValue())
                .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DatasetDto.DetailResponse getDatasetDetail(Long datasetId) {
        DatasetMeta meta = metaRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));
        if (meta.getStatus() != null && !DatasetMeta.STATUS_READY.equals(meta.getStatus())) {
            throw new IllegalArgumentException("Dataset not found: " + datasetId);
        }

        List<DatasetColumn> columns = columnRepository.findByDatasetIdOrderBySortOrderAsc(datasetId);

        List<DatasetDto.ColumnResponse> columnResponses = columns.stream()
                .map(col -> DatasetDto.ColumnResponse.builder()
                        .key(col.getColumnKey())
                        .title(col.getColumnName())
                        .dataType(col.getDataType())
                        .build())
                .collect(Collectors.toList());

        return DatasetDto.DetailResponse.builder()
                .id(meta.getId())
                .name(meta.getName())
                .description(meta.getDescription())
                .category(meta.getCategory())
                .ownerId(meta.getOwnerId())
                .orgId(meta.getOrgId())
                .createdAt(meta.getCreatedAt())
                .columns(columnResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public DatasetDto.PagedRowsResponse getDatasetRowsPaged(Long datasetId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<DatasetRow> rowPage = rowRepository.findByDatasetIdOrderByRowIndexAsc(datasetId, pageable);

        List<Map<String, Object>> rowMaps = rowPage.getContent().stream().map(row -> {
            try {
                Map<String, Object> map = objectMapper.readValue(row.getDataJson(), new TypeReference<Map<String, Object>>() {});
                map.put("key", row.getId());
                return map;
            } catch (Exception e) {
                return new HashMap<String, Object>();
            }
        }).collect(Collectors.toList());

        return DatasetDto.PagedRowsResponse.builder()
                .totalElements(rowPage.getTotalElements())
                .totalPages(rowPage.getTotalPages())
                .currentPage(rowPage.getNumber())
                .pageSize(rowPage.getSize())
                .rows(rowMaps)
                .build();
    }

    public SchemaPreviewDto.Response previewAndValidate(MultipartFile file) throws Exception {
        if (isCsvFile(file.getOriginalFilename())) {
            return previewAndValidateCsv(file);
        }

        List<String> warnings = new ArrayList<>();
        boolean valid = true;

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                return SchemaPreviewDto.Response.builder()
                        .valid(false)
                        .warnings(List.of("엑셀 워크시트(Sheet)가 존재하지 않습니다."))
                        .build();
            }

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                return SchemaPreviewDto.Response.builder()
                        .valid(false)
                        .warnings(List.of("파일에 행 데이터가 존재하지 않는 빈 파일입니다."))
                        .build();
            }

            Row headerRow = rowIterator.next();
            if (headerRow == null || headerRow.getLastCellNum() <= 0) {
                return SchemaPreviewDto.Response.builder()
                        .valid(false)
                        .warnings(List.of("첫 번째 행에서 컬럼 헤더(Header) 정보를 찾을 수 없습니다."))
                        .build();
            }

            List<SchemaPreviewDto.ColumnInfo> columnInfos = new ArrayList<>();

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String columnName = cell != null ? cell.toString().trim() : "";

                if (columnName.isEmpty()) {
                    columnName = "Column_" + (i + 1);
                    warnings.add("일부 컬럼 헤더가 비어있어 '" + columnName + "'으로 임시 지정되었습니다.");
                }

                columnInfos.add(SchemaPreviewDto.ColumnInfo.builder()
                        .columnKey("col_" + i)
                        .columnName(columnName)
                        .dataType("STRING") // 기본 STRING, 하단 검증에서 타입 추정
                        .sortOrder(i)
                        .build());
            }

            // 샘플 10행 파싱 및 타입 자동 추정
            List<Map<String, Object>> previewRows = new ArrayList<>();
            int sampleCount = 0;

            while (rowIterator.hasNext() && sampleCount < 10) {
                Row row = rowIterator.next();
                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean isEmptyRow = true;

                for (int i = 0; i < columnInfos.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    Object cellValue = getCellValue(cell);
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        isEmptyRow = false;
                    }
                    rowMap.put(columnInfos.get(i).getColumnKey(), cellValue != null ? cellValue : "");
                }

                if (!isEmptyRow) {
                    rowMap.put("key", "preview_" + sampleCount);
                    previewRows.add(rowMap);
                    sampleCount++;
                }
            }

            if (previewRows.isEmpty()) {
                valid = false;
                warnings.add("헤더 이후 적재할 유효한 샘플 데이터 행이 존재하지 않습니다.");
            } else {
                // 타입 추정 (숫자/날짜 여부)
                for (SchemaPreviewDto.ColumnInfo col : columnInfos) {
                    String inferredType = inferColumnDataType(col.getColumnKey(), previewRows);
                    col.setDataType(inferredType);
                }
            }

            return SchemaPreviewDto.Response.builder()
                    .valid(valid)
                    .warnings(warnings)
                    .columns(columnInfos)
                    .previewRows(previewRows)
                    .totalPreviewRows(previewRows.size())
                    .build();
        }
    }

    @Async
    public void processAsyncUpload(String taskId, byte[] fileBytes, String originalFilename, SchemaPreviewDto.FinalUploadRequest request) {
        Long datasetId = null;
        try {
            uploadTaskService.updateProgress(taskId, 10, "데이터 메타정보 생성 중...");

            String datasetName = request.getName();
            if (datasetName == null || datasetName.trim().isEmpty()) {
                datasetName = originalFilename != null ? originalFilename : "무제 데이터셋";
            }

            DatasetMeta meta = DatasetMeta.builder()
                    .name(datasetName)
                    .description(request.getDescription())
                    .category(request.getCategory() != null ? request.getCategory() : "기타")
                    .ownerId(request.getOwnerId() != null ? request.getOwnerId() : "user_admin")
                    .orgId(request.getOrgId() != null ? request.getOrgId() : "org_hq")
                    .status(DatasetMeta.STATUS_PROCESSING)
                    .build();

            meta = metaRepository.save(meta);
            datasetId = meta.getId();

            uploadTaskService.updateProgress(taskId, 25, "확정된 스키마 저장 중...");

            List<DatasetColumn> columns = new ArrayList<>();
            for (SchemaPreviewDto.ColumnInfo colDto : request.getColumns()) {
                columns.add(DatasetColumn.builder()
                        .datasetId(datasetId)
                        .columnKey(colDto.getColumnKey())
                        .columnName(colDto.getColumnName())
                        .dataType(colDto.getDataType())
                        .sortOrder(colDto.getSortOrder())
                        .build());
            }
            columnRepository.saveAll(columns);

            if (isCsvFile(originalFilename)) {
                uploadTaskService.updateProgress(taskId, 40, "CSV 대용량 행 데이터 읽는 중...");
                try (CSVParser parser = openCsvParser(fileBytes)) {
                    Iterator<CSVRecord> records = parser.iterator();
                    if (records.hasNext()) {
                        records.next(); // 헤더 건너뛰기
                    }
                    final int CHUNK_SIZE = 500;
                    List<DatasetRow> chunkList = new ArrayList<>();
                    int rowIndex = 0;
                    int processedCount = 0;

                    while (records.hasNext()) {
                        List<String> values = records.next().toList();
                        if (isBlankRecord(values)) continue;
                        Map<String, Object> rowMap = new LinkedHashMap<>();

                        for (int i = 0; i < request.getColumns().size(); i++) {
                            SchemaPreviewDto.ColumnInfo col = request.getColumns().get(i);
                            String cellValue = i < values.size() ? values.get(i) : "";
                            rowMap.put(col.getColumnKey(), cellValue);
                        }

                        chunkList.add(DatasetRow.builder()
                                .datasetId(datasetId)
                                .rowIndex(rowIndex++)
                                .dataJson(objectMapper.writeValueAsString(rowMap))
                                .build());

                        if (chunkList.size() >= CHUNK_SIZE) {
                            insertRows(chunkList);
                            chunkList.clear();
                        }

                        processedCount++;
                        if (processedCount % 100 == 0) {
                            int currentPercent = Math.min(95, 40 + (processedCount / 50));
                            uploadTaskService.updateProgress(taskId, currentPercent, "CSV 데이터 적재 진행 중 (" + processedCount + " 행)");
                        }
                    }

                    if (!chunkList.isEmpty()) {
                        insertRows(chunkList);
                        chunkList.clear();
                    }

                    finishUpload(taskId, datasetId);
                }
            } else {
                uploadTaskService.updateProgress(taskId, 40, "엑셀 대용량 행 데이터 읽는 중...");
                try (InputStream is = new java.io.ByteArrayInputStream(fileBytes); Workbook workbook = WorkbookFactory.create(is)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    int totalRows = sheet.getLastRowNum();
                    Iterator<Row> rowIterator = sheet.iterator();

                    if (rowIterator.hasNext()) {
                        rowIterator.next(); // skip header
                    }

                    final int CHUNK_SIZE = 500;
                    List<DatasetRow> chunkList = new ArrayList<>();
                    int rowIndex = 0;
                    int processedCount = 0;

                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        Map<String, Object> rowMap = new LinkedHashMap<>();
                        boolean isEmptyRow = true;

                        for (int i = 0; i < request.getColumns().size(); i++) {
                            SchemaPreviewDto.ColumnInfo col = request.getColumns().get(i);
                            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                            Object cellValue = getCellValue(cell);
                            if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                                isEmptyRow = false;
                            }
                            rowMap.put(col.getColumnKey(), cellValue != null ? cellValue : "");
                        }

                        if (!isEmptyRow) {
                            chunkList.add(DatasetRow.builder()
                                    .datasetId(datasetId)
                                    .rowIndex(rowIndex++)
                                    .dataJson(objectMapper.writeValueAsString(rowMap))
                                    .build());

                            if (chunkList.size() >= CHUNK_SIZE) {
                                insertRows(chunkList);
                                chunkList.clear();
                            }
                        }

                        processedCount++;
                        if (totalRows > 0 && processedCount % 50 == 0) {
                            int currentPercent = 40 + (int) (((double) processedCount / totalRows) * 55);
                            uploadTaskService.updateProgress(taskId, Math.min(95, currentPercent), "데이터 적재 진행 중 (" + processedCount + " 행)");
                        }
                    }

                    if (!chunkList.isEmpty()) {
                        insertRows(chunkList);
                        chunkList.clear();
                    }

                    finishUpload(taskId, datasetId);
                }
            }
        } catch (Exception e) {
            cleanupFailedUpload(datasetId);
            uploadTaskService.failTask(taskId, e.getMessage());
        }
    }

    // IDENTITY 키 전략에서는 saveAll도 건별 insert가 되므로 JDBC 배치를 직접 사용
    private void insertRows(List<DatasetRow> rows) {
        jdbcTemplate.batchUpdate(
                "insert into dataset_row (dataset_id, row_index, data_json) values (?, ?, ?)",
                rows, rows.size(), (ps, row) -> {
                    ps.setLong(1, row.getDatasetId());
                    ps.setInt(2, row.getRowIndex());
                    ps.setString(3, row.getDataJson());
                });
    }

    private void finishUpload(String taskId, Long datasetId) {
        DatasetMeta meta = metaRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalStateException("적재 중 데이터셋이 삭제되었습니다."));
        meta.setStatus(DatasetMeta.STATUS_READY);
        metaRepository.save(meta);
        uploadTaskService.completeTask(taskId, datasetId);
    }

    // 정리마저 실패한 경우 FAILED로 남겨 목록에서 숨김
    private void cleanupFailedUpload(Long datasetId) {
        if (datasetId == null) return;
        try {
            transactionTemplate.executeWithoutResult(tx -> deleteDatasetInternal(datasetId));
        } catch (Exception cleanupError) {
            metaRepository.findById(datasetId).ifPresent(m -> {
                m.setStatus(DatasetMeta.STATUS_FAILED);
                metaRepository.save(m);
            });
        }
    }

    private void deleteDatasetInternal(Long datasetId) {
        rowRepository.deleteByDatasetId(datasetId);
        columnRepository.deleteByDatasetId(datasetId);
        metaRepository.deleteById(datasetId);
    }

    @Transactional
    public Long uploadDataset(MultipartFile file, DatasetDto.UploadRequest request) throws Exception {
        DatasetMeta meta = DatasetMeta.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : "기타")
                .ownerId(request.getOwnerId() != null ? request.getOwnerId() : "user_admin")
                .orgId(request.getOrgId() != null ? request.getOrgId() : "org_hq")
                .build();

        meta = metaRepository.save(meta);
        Long datasetId = meta.getId();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new IllegalArgumentException("엑셀 파일이 비어 있습니다.");
            }

            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            List<DatasetColumn> columns = new ArrayList<>();

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String columnName = cell != null ? cell.toString().trim() : "Column_" + (i + 1);
                String columnKey = "col_" + i;

                headers.add(columnKey);

                columns.add(DatasetColumn.builder()
                        .datasetId(datasetId)
                        .columnKey(columnKey)
                        .columnName(columnName)
                        .dataType("STRING")
                        .sortOrder(i)
                        .build());
            }

            columnRepository.saveAll(columns);

            List<DatasetRow> datasetRows = new ArrayList<>();
            int rowIndex = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean isEmptyRow = true;

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    Object cellValue = getCellValue(cell);
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        isEmptyRow = false;
                    }
                    rowMap.put(headers.get(i), cellValue != null ? cellValue : "");
                }

                if (!isEmptyRow) {
                    datasetRows.add(DatasetRow.builder()
                            .datasetId(datasetId)
                            .rowIndex(rowIndex++)
                            .dataJson(objectMapper.writeValueAsString(rowMap))
                            .build());
                }
            }

            rowRepository.saveAll(datasetRows);
        }

        return datasetId;
    }

    @Transactional
    public void deleteDataset(Long datasetId) {
        deleteDatasetInternal(datasetId);
    }

    private String inferColumnDataType(String colKey, List<Map<String, Object>> rows) {
        boolean allNumeric = true;
        boolean hasValue = false;

        for (Map<String, Object> row : rows) {
            Object val = row.get(colKey);
            if (val != null && !val.toString().trim().isEmpty()) {
                hasValue = true;
                if (!(val instanceof Number)) {
                    try {
                        Double.parseDouble(val.toString());
                    } catch (NumberFormatException e) {
                        allNumeric = false;
                        break;
                    }
                }
            }
        }

        if (hasValue && allNumeric) {
            return "NUMBER";
        }
        return "STRING";
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> numericCellValue(cell);
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null;
        };
    }

    private Object numericCellValue(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            java.time.LocalDateTime dt = cell.getLocalDateTimeCellValue();
            return dt.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? dt.toLocalDate().toString() : dt.toString();
        }
        double d = cell.getNumericCellValue();
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return (long) d;
        }
        return d;
    }

    @Transactional
    public Long createManualDataset(DatasetDto.ManualCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("데이터셋 이름은 필수입니다.");
        }
        if (request.getColumns() == null || request.getColumns().isEmpty()) {
            throw new IllegalArgumentException("최소 1개 이상의 컬럼 정의가 필요합니다.");
        }

        DatasetMeta meta = DatasetMeta.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory() != null && !request.getCategory().trim().isEmpty() ? request.getCategory() : "기타")
                .ownerId(request.getOwnerId() != null && !request.getOwnerId().trim().isEmpty() ? request.getOwnerId() : "user_admin")
                .orgId(request.getOrgId() != null && !request.getOrgId().trim().isEmpty() ? request.getOrgId() : "org_hq")
                .build();

        meta = metaRepository.save(meta);
        Long datasetId = meta.getId();

        List<DatasetColumn> columns = new ArrayList<>();
        int order = 0;
        for (DatasetDto.ManualColumnDef colDef : request.getColumns()) {
            String colName = colDef.getColumnName() != null && !colDef.getColumnName().trim().isEmpty()
                    ? colDef.getColumnName().trim()
                    : "컬럼_" + (order + 1);
            String dataType = colDef.getDataType() != null && !colDef.getDataType().trim().isEmpty()
                    ? colDef.getDataType()
                    : "STRING";

            columns.add(DatasetColumn.builder()
                    .datasetId(datasetId)
                    .columnKey("col_" + order)
                    .columnName(colName)
                    .dataType(dataType)
                    .sortOrder(order)
                    .build());
            order++;
        }

        columnRepository.saveAll(columns);
        return datasetId;
    }

    private boolean isCsvFile(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    // UTF-8로 엄격 해석이 가능하면 UTF-8, 아니면 엑셀 저장 CSV에 흔한 MS949로 판단
    private Charset detectCsvCharset(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(bytes);
        CharBuffer out = CharBuffer.allocate(8192);
        CoderResult result;
        while (!(result = decoder.decode(in, out, true)).isUnderflow()) {
            if (result.isError()) {
                return Charset.forName("MS949");
            }
            out.clear();
        }
        return StandardCharsets.UTF_8;
    }

    private CSVParser openCsvParser(byte[] bytes) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), detectCsvCharset(bytes)));
        reader.mark(1);
        if (reader.read() != '﻿') {
            reader.reset();
        }
        return CSVFormat.DEFAULT.builder().setTrim(true).build().parse(reader);
    }

    private boolean isBlankRecord(List<String> values) {
        return values.stream().allMatch(String::isEmpty);
    }

    public SchemaPreviewDto.Response previewAndValidateCsv(MultipartFile file) throws Exception {
        List<String> warnings = new ArrayList<>();
        boolean valid = true;

        try (CSVParser parser = openCsvParser(file.getBytes())) {
            Iterator<CSVRecord> records = parser.iterator();
            if (!records.hasNext()) {
                return SchemaPreviewDto.Response.builder()
                        .valid(false)
                        .warnings(List.of("CSV 파일이 비어 있습니다."))
                        .build();
            }

            List<String> headers = records.next().toList();
            if (headers.isEmpty() || isBlankRecord(headers)) {
                return SchemaPreviewDto.Response.builder()
                        .valid(false)
                        .warnings(List.of("첫 번째 행에서 컬럼 헤더 정보를 찾을 수 없습니다."))
                        .build();
            }

            List<SchemaPreviewDto.ColumnInfo> columnInfos = new ArrayList<>();

            for (int i = 0; i < headers.size(); i++) {
                String columnName = headers.get(i);
                if (columnName.isEmpty()) {
                    columnName = "Column_" + (i + 1);
                    warnings.add("일부 컬럼 헤더가 비어있어 '" + columnName + "'으로 임시 지정되었습니다.");
                }

                columnInfos.add(SchemaPreviewDto.ColumnInfo.builder()
                        .columnKey("col_" + i)
                        .columnName(columnName)
                        .dataType("STRING")
                        .sortOrder(i)
                        .build());
            }

            List<Map<String, Object>> previewRows = new ArrayList<>();
            int sampleCount = 0;

            while (records.hasNext() && sampleCount < 10) {
                List<String> values = records.next().toList();
                if (isBlankRecord(values)) continue;
                Map<String, Object> rowMap = new LinkedHashMap<>();

                for (int i = 0; i < columnInfos.size(); i++) {
                    String val = i < values.size() ? values.get(i) : "";
                    rowMap.put(columnInfos.get(i).getColumnKey(), val);
                }

                rowMap.put("key", "preview_" + sampleCount);
                previewRows.add(rowMap);
                sampleCount++;
            }

            if (previewRows.isEmpty()) {
                valid = false;
                warnings.add("헤더 이후 적재할 유효한 샘플 데이터 행이 존재하지 않습니다.");
            } else {
                for (SchemaPreviewDto.ColumnInfo col : columnInfos) {
                    col.setDataType(inferColumnDataType(col.getColumnKey(), previewRows));
                }
            }

            return SchemaPreviewDto.Response.builder()
                    .valid(valid)
                    .warnings(warnings)
                    .columns(columnInfos)
                    .previewRows(previewRows)
                    .totalPreviewRows(previewRows.size())
                    .build();
        } catch (UncheckedIOException e) {
            throw new IllegalArgumentException("CSV 형식이 올바르지 않습니다. 따옴표 짝이 맞는지 확인해 주세요.");
        }
    }

    public void validateUploadRequest(SchemaPreviewDto.FinalUploadRequest request) {
        List<SchemaPreviewDto.ColumnInfo> columns = request.getColumns();
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("컬럼 정의가 필요합니다.");
        }

        Set<String> names = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (SchemaPreviewDto.ColumnInfo col : columns) {
            String name = col.getColumnName() == null ? "" : col.getColumnName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("컬럼명이 비어 있는 컬럼이 있습니다.");
            }
            if (!names.add(name)) {
                throw new IllegalArgumentException("중복된 컬럼명('" + name + "')이 존재합니다. 컬럼명을 고유하게 변경해 주세요.");
            }
            if (col.getColumnKey() == null || col.getColumnKey().isBlank() || !keys.add(col.getColumnKey())) {
                throw new IllegalArgumentException("컬럼 키가 비어 있거나 중복되었습니다.");
            }
        }
    }

    @Transactional
    public void updateDatasetMeta(Long id, DatasetDto.MetaUpdateRequest request) {
        DatasetMeta meta = metaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            meta.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            meta.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            meta.setCategory(request.getCategory().trim());
        }
        metaRepository.save(meta);
    }

    @Transactional
    public Long addRow(Long datasetId, Map<String, Object> rowData) throws Exception {
        metaRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));

        int nextIndex = rowRepository.findFirstByDatasetIdOrderByRowIndexDesc(datasetId)
                .map(r -> r.getRowIndex() + 1)
                .orElse(0);

        Map<String, Object> cleanMap = new HashMap<>(rowData);
        cleanMap.remove("key");

        DatasetRow row = DatasetRow.builder()
                .datasetId(datasetId)
                .rowIndex(nextIndex)
                .dataJson(objectMapper.writeValueAsString(cleanMap))
                .build();

        return rowRepository.save(row).getId();
    }

    @Transactional
    public void updateRow(Long datasetId, Long rowId, Map<String, Object> rowData) throws Exception {
        DatasetRow row = rowRepository.findByIdAndDatasetId(rowId, datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Row not found: " + rowId));

        Map<String, Object> cleanMap = new HashMap<>(rowData);
        cleanMap.remove("key");

        row.setDataJson(objectMapper.writeValueAsString(cleanMap));
        rowRepository.save(row);
    }

    @Transactional
    public void deleteRow(Long datasetId, Long rowId) {
        DatasetRow row = rowRepository.findByIdAndDatasetId(rowId, datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Row not found: " + rowId));
        rowRepository.delete(row);
    }
}

