package com.dgjs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ProjectDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberAddRequest {
        private String empNo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String projectCode;
        private String name;
        private long memberCount;
        private LocalDateTime createdAt;
    }
}
