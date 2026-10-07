package com.dgjs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DatasetDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private Long id;
        private String name;
        private String description;
        private String category;
        private String ownerId;
        private String orgId;
        private LocalDateTime createdAt;
        private long rowCount;
        private int columnCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private Long id;
        private String name;
        private String description;
        private String category;
        private String ownerId;
        private String orgId;
        private LocalDateTime createdAt;
        private List<ColumnResponse> columns;
        private List<Map<String, Object>> rows;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnResponse {
        private String key;
        private String title;
        private String dataType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRequest {
        private String name;
        private String description;
        private String category;
        private String ownerId;
        private String orgId;
    }
}
