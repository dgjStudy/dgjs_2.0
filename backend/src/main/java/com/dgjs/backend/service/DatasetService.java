package com.dgjs.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.EncryptedDocumentException;
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
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetMetaRepository metaRepository;
    private final DatasetColumnRepository columnRepository;
    private final DatasetRowRepository rowRepository;
    private final UploadTaskService uploadTaskService;
    private final RowDataValidator rowDataValidator;
    private final UploadFileStorage uploadFileStorage;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int PREVIEW_ROW_LIMIT = 10;
    private static final String CSV_FORMAT_ERROR = "CSV 형식이 올바르지 않습니다. 따옴표 짝이 맞는지 확인해 주세요.";

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
                .orElseThrow(() -> datasetNotFound(datasetId));
        if (meta.getStatus() != null && !DatasetMeta.STATUS_READY.equals(meta.getStatus())) {
            throw datasetNotFound(datasetId);
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

    public SchemaPreviewDto.Response previewAndValidate(MultipartFile file) throws IOException {
        if (isCsvFile(file.getOriginalFilename())) {
            return previewAndValidateCsv(file);
        }

        Path tempFile = uploadFileStorage.save(file);
        try {
            return previewSpreadsheet(tempFile);
        } finally {
            uploadFileStorage.delete(tempFile);
        }
    }

    private SchemaPreviewDto.Response previewSpreadsheet(Path path) {
        List<String> warnings = new ArrayList<>();
        List<SchemaPreviewDto.ColumnInfo> columnInfos = new ArrayList<>();
        List<Map<String, Object>> previewRows = new ArrayList<>();
        boolean[] headerRead = {false};
        String[] headerError = {null};

        boolean hasSheet;
        try {
            hasSheet = SpreadsheetReader.read(path, (rowNumber, cells) -> {
                if (!headerRead[0]) {
                    headerRead[0] = true;
                    if (cells.isEmpty()) {
                        headerError[0] = "첫 번째 행에서 컬럼 헤더(Header) 정보를 찾을 수 없습니다.";
                        return false;
                    }
                    for (int i = 0; i < cells.size(); i++) {
                        String columnName = headerName(cells.get(i));
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
                    return true;
                }

                // 샘플 10행 파싱 (빈 행은 건너뜀)
                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean isEmptyRow = true;
                for (int i = 0; i < columnInfos.size(); i++) {
                    Object cellValue = i < cells.size() ? cells.get(i) : null;
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        isEmptyRow = false;
                    }
                    rowMap.put(columnInfos.get(i).getColumnKey(), cellValue != null ? cellValue : "");
                }
                if (!isEmptyRow) {
                    rowMap.put("key", "preview_" + previewRows.size());
                    previewRows.add(rowMap);
                }
                return previewRows.size() < PREVIEW_ROW_LIMIT;
            });
        } catch (IOException | EncryptedDocumentException e) {
            throw new IllegalArgumentException(SpreadsheetReader.UNREADABLE_MESSAGE, e);
        }

        if (!hasSheet) {
            return invalidPreview("엑셀 워크시트(Sheet)가 존재하지 않습니다.");
        }
        if (!headerRead[0]) {
            return invalidPreview("파일에 행 데이터가 존재하지 않는 빈 파일입니다.");
        }
        if (headerError[0] != null) {
            return invalidPreview(headerError[0]);
        }

        boolean valid = true;
        if (previewRows.isEmpty()) {
            valid = false;
            warnings.add("헤더 이후 적재할 유효한 샘플 데이터 행이 존재하지 않습니다.");
        } else {
            // 타입 추정 (숫자/날짜 여부)
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
    }

    private SchemaPreviewDto.Response invalidPreview(String warning) {
        return SchemaPreviewDto.Response.builder()
                .valid(false)
                .warnings(List.of(warning))
                .build();
    }

    private String headerName(Object value) {
        if (value == null) return "";
        if (value instanceof Boolean b) return b ? "TRUE" : "FALSE";
        return value.toString().trim();
    }

    @Async
    public void processAsyncUpload(String taskId, Path uploadedFile, String originalFilename, SchemaPreviewDto.FinalUploadRequest request) {
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
                try (CSVParser parser = openCsvParser(() -> Files.newInputStream(uploadedFile))) {
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
                } catch (UncheckedIOException e) {
                    throw new IllegalArgumentException(CSV_FORMAT_ERROR, e);
                }
            } else {
                uploadTaskService.updateProgress(taskId, 40, "엑셀 대용량 행 데이터 읽는 중...");
                loadSpreadsheetRows(taskId, datasetId, uploadedFile, request);
                finishUpload(taskId, datasetId);
            }
        } catch (Exception e) {
            log.error("비동기 적재 실패 (taskId={})", taskId, e);
            cleanupFailedUpload(datasetId);
            uploadTaskService.failTask(taskId, e instanceof IllegalArgumentException ? e.getMessage() : "서버 내부 오류가 발생했습니다.");
        } finally {
            uploadFileStorage.delete(uploadedFile);
        }
    }

    private void loadSpreadsheetRows(String taskId, Long datasetId, Path file, SchemaPreviewDto.FinalUploadRequest request) throws IOException {
        final int CHUNK_SIZE = 500;
        List<SchemaPreviewDto.ColumnInfo> columns = request.getColumns();
        List<DatasetRow> chunkList = new ArrayList<>();
        int[] counters = {0, 0, 0}; // 저장한 행 번호, 처리한 행 수, 시트에 선언된 마지막 행 번호
        boolean[] headerSkipped = {false};

        SpreadsheetReader.read(file, new SpreadsheetReader.RowVisitor() {
            @Override
            public void totalRows(int total) {
                counters[2] = total;
            }

            @Override
            public boolean visit(int rowNumber, List<Object> cells) throws IOException {
                if (!headerSkipped[0]) {
                    headerSkipped[0] = true;
                    return true;
                }

                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean isEmptyRow = true;
                for (int i = 0; i < columns.size(); i++) {
                    Object cellValue = i < cells.size() ? cells.get(i) : null;
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        isEmptyRow = false;
                    }
                    rowMap.put(columns.get(i).getColumnKey(), cellValue != null ? cellValue : "");
                }

                if (!isEmptyRow) {
                    chunkList.add(DatasetRow.builder()
                            .datasetId(datasetId)
                            .rowIndex(counters[0]++)
                            .dataJson(objectMapper.writeValueAsString(rowMap))
                            .build());
                    if (chunkList.size() >= CHUNK_SIZE) {
                        insertRows(chunkList);
                        chunkList.clear();
                    }
                }

                counters[1]++;
                if (counters[1] % 50 == 0) {
                    uploadTaskService.updateProgress(taskId, progressPercent(counters[1], counters[2]),
                            "데이터 적재 진행 중 (" + counters[1] + " 행)");
                }
                return true;
            }
        });

        if (!chunkList.isEmpty()) {
            insertRows(chunkList);
            chunkList.clear();
        }
    }

    // 40~95% 구간. 총 행 수를 알 수 없으면 처리 행 수가 늘수록 95에 천천히 가까워지게 함
    static int progressPercent(int processed, int totalRows) {
        if (totalRows > 0) {
            return Math.min(95, 40 + (int) (((double) processed / totalRows) * 55));
        }
        return 40 + (int) (55.0 * processed / (processed + 50_000.0));
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

    @FunctionalInterface
    private interface StreamSource {
        InputStream open() throws IOException;
    }

    // UTF-8로 엄격 해석이 가능하면 UTF-8, 아니면 엑셀 저장 CSV에 흔한 MS949로 판단 (파일 전체를 스트림으로 한 번 훑음)
    private Charset detectCsvCharset(StreamSource source) throws IOException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (Reader reader = new InputStreamReader(source.open(), decoder)) {
            char[] buffer = new char[8192];
            while (reader.read(buffer) != -1) {
                // 디코딩 오류 여부만 확인함
            }
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return Charset.forName("MS949");
        }
    }

    private CSVParser openCsvParser(StreamSource source) throws IOException {
        Charset charset = detectCsvCharset(source);
        BufferedReader reader = new BufferedReader(new InputStreamReader(source.open(), charset));
        reader.mark(1);
        if (reader.read() != '﻿') {
            reader.reset();
        }
        return CSVFormat.DEFAULT.builder().setTrim(true).build().parse(reader);
    }

    private boolean isBlankRecord(List<String> values) {
        return values.stream().allMatch(String::isEmpty);
    }

    public SchemaPreviewDto.Response previewAndValidateCsv(MultipartFile file) throws IOException {
        List<String> warnings = new ArrayList<>();
        boolean valid = true;

        try (CSVParser parser = openCsvParser(file::getInputStream)) {
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
            throw new IllegalArgumentException(CSV_FORMAT_ERROR, e);
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
                .orElseThrow(() -> datasetNotFound(id));
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
    public Long addRow(Long datasetId, Map<String, Object> rowData) throws JsonProcessingException {
        DatasetMeta meta = metaRepository.findById(datasetId)
                .orElseThrow(() -> datasetNotFound(datasetId));
        // 적재 중이거나 실패한 데이터셋에 행을 넣으면 배치 적재의 행 번호와 충돌할 수 있음
        if (meta.getStatus() != null && !DatasetMeta.STATUS_READY.equals(meta.getStatus())) {
            throw datasetNotFound(datasetId);
        }

        List<DatasetColumn> columns = columnRepository.findByDatasetIdOrderBySortOrderAsc(datasetId);
        Map<String, Object> validated = rowDataValidator.validate(columns, rowData, null);

        int nextIndex = rowRepository.findFirstByDatasetIdOrderByRowIndexDesc(datasetId)
                .map(r -> r.getRowIndex() + 1)
                .orElse(0);

        DatasetRow row = DatasetRow.builder()
                .datasetId(datasetId)
                .rowIndex(nextIndex)
                .dataJson(objectMapper.writeValueAsString(validated))
                .build();

        return rowRepository.save(row).getId();
    }

    @Transactional
    public void updateRow(Long datasetId, Long rowId, Map<String, Object> rowData) throws JsonProcessingException {
        DatasetRow row = rowRepository.findByIdAndDatasetId(rowId, datasetId)
                .orElseThrow(() -> rowNotFound(rowId));

        List<DatasetColumn> columns = columnRepository.findByDatasetIdOrderBySortOrderAsc(datasetId);
        Map<String, Object> existing = objectMapper.readValue(row.getDataJson(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> validated = rowDataValidator.validate(columns, rowData, existing);

        row.setDataJson(objectMapper.writeValueAsString(validated));
        rowRepository.save(row);
    }

    @Transactional
    public void deleteRow(Long datasetId, Long rowId) {
        DatasetRow row = rowRepository.findByIdAndDatasetId(rowId, datasetId)
                .orElseThrow(() -> rowNotFound(rowId));
        rowRepository.delete(row);
    }

    private ResourceNotFoundException datasetNotFound(Long datasetId) {
        return new ResourceNotFoundException("데이터셋을 찾을 수 없습니다. (ID: " + datasetId + ")");
    }

    private ResourceNotFoundException rowNotFound(Long rowId) {
        return new ResourceNotFoundException("데이터 행을 찾을 수 없습니다. (ID: " + rowId + ")");
    }
}

