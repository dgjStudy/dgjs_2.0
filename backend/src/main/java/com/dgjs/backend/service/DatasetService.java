package com.dgjs.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dgjs.backend.domain.DatasetColumn;
import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.DatasetRow;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.dto.SchemaPreviewDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetMetaRepository metaRepository;
    private final DatasetColumnRepository columnRepository;
    private final DatasetRowRepository rowRepository;
    private final UploadTaskService uploadTaskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<DatasetDto.SummaryResponse> getAllDatasets() {
        List<DatasetMeta> metaList = metaRepository.findAllByOrderByCreatedAtDesc();
        return metaList.stream().map(meta -> {
            long rowCount = rowRepository.countByDatasetId(meta.getId());
            int columnCount = columnRepository.findByDatasetIdOrderBySortOrderAsc(meta.getId()).size();
            return DatasetDto.SummaryResponse.builder()
                    .id(meta.getId())
                    .name(meta.getName())
                    .description(meta.getDescription())
                    .category(meta.getCategory())
                    .ownerId(meta.getOwnerId())
                    .orgId(meta.getOrgId())
                    .createdAt(meta.getCreatedAt())
                    .rowCount(rowCount)
                    .columnCount(columnCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DatasetDto.DetailResponse getDatasetDetail(Long datasetId) {
        DatasetMeta meta = metaRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));

        List<DatasetColumn> columns = columnRepository.findByDatasetIdOrderBySortOrderAsc(datasetId);
        List<DatasetRow> rows = rowRepository.findByDatasetIdOrderByRowIndexAsc(datasetId);

        List<DatasetDto.ColumnResponse> columnResponses = columns.stream()
                .map(col -> DatasetDto.ColumnResponse.builder()
                        .key(col.getColumnKey())
                        .title(col.getColumnName())
                        .dataType(col.getDataType())
                        .build())
                .collect(Collectors.toList());

        List<Map<String, Object>> rowMaps = rows.stream().map(row -> {
            try {
                Map<String, Object> map = objectMapper.readValue(row.getDataJson(), new TypeReference<Map<String, Object>>() {});
                map.put("key", row.getId());
                return map;
            } catch (Exception e) {
                return new HashMap<String, Object>();
            }
        }).collect(Collectors.toList());

        return DatasetDto.DetailResponse.builder()
                .id(meta.getId())
                .name(meta.getName())
                .description(meta.getDescription())
                .category(meta.getCategory())
                .ownerId(meta.getOwnerId())
                .orgId(meta.getOrgId())
                .createdAt(meta.getCreatedAt())
                .columns(columnResponses)
                .rows(rowMaps)
                .build();
    }

    public SchemaPreviewDto.Response previewAndValidate(MultipartFile file) throws Exception {
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
            Set<String> columnNameSet = new HashSet<>();

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String columnName = cell != null ? cell.toString().trim() : "";

                if (columnName.isEmpty()) {
                    columnName = "Column_" + (i + 1);
                    warnings.add("일부 컬럼 헤더가 비어있어 '" + columnName + "'으로 임시 지정되었습니다.");
                }

                if (columnNameSet.contains(columnName)) {
                    warnings.add("중복된 컬럼명('" + columnName + "')이 존재합니다. 정형 DB 적재를 위해 컬럼명을 고유하게 변경해 주세요.");
                    valid = false;
                }
                columnNameSet.add(columnName);

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
        uploadTaskService.initTask(taskId);
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
                    .build();

            meta = metaRepository.save(meta);
            Long datasetId = meta.getId();

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

            uploadTaskService.updateProgress(taskId, 40, "엑셀 대용량 행 데이터 읽는 중...");

            try (InputStream is = new java.io.ByteArrayInputStream(fileBytes); Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                Iterator<Row> rowIterator = sheet.iterator();

                if (rowIterator.hasNext()) {
                    rowIterator.next(); // skip header
                }

                List<DatasetRow> datasetRows = new ArrayList<>();
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
                        datasetRows.add(DatasetRow.builder()
                                .datasetId(datasetId)
                                .rowIndex(rowIndex++)
                                .dataJson(objectMapper.writeValueAsString(rowMap))
                                .build());
                    }

                    processedCount++;
                    if (totalRows > 0 && processedCount % 50 == 0) {
                        int currentPercent = 40 + (int) (((double) processedCount / totalRows) * 55);
                        uploadTaskService.updateProgress(taskId, Math.min(95, currentPercent), "데이터 적재 진행 중 (" + processedCount + " 행)");
                    }
                }

                rowRepository.saveAll(datasetRows);
                uploadTaskService.completeTask(taskId, datasetId);
            }
        } catch (Exception e) {
            uploadTaskService.failTask(taskId, e.getMessage());
        }
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
        rowRepository.deleteByDatasetId(datasetId);
        columnRepository.deleteByDatasetId(datasetId);
        metaRepository.deleteById(datasetId);
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
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue().toString() : cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}

