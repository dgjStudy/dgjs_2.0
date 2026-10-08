package com.dgjs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class SchemaPreviewDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfo {
        private String columnKey;
        private String columnName;
        private String dataType; // STRING, NUMBER, DATE, BOOLEAN
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private boolean valid;
        private List<String> warnings;
        private List<ColumnInfo> columns;
        private List<Map<String, Object>> previewRows;
        private int totalPreviewRows;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalUploadRequest {
        private String name;
        private String description;
        private String category;
        private String ownerId;
        private String orgId;
        private List<ColumnInfo> columns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProgressResponse {
        private String taskId;
        private String status; // PROCESSING, COMPLETED, FAILED
        private int progress; // 0 ~ 100
        private String message;
        private Long datasetId;
    }
}
