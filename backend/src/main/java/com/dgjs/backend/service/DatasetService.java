package com.dgjs.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dgjs.backend.domain.DatasetColumn;
import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.DatasetRow;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.repository.DatasetColumnRepository;
import com.dgjs.backend.repository.DatasetMetaRepository;
import com.dgjs.backend.repository.DatasetRowRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
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

            // 1. 헤더 추출
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

            // 2. Row 데이터 파싱 및 저장
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
