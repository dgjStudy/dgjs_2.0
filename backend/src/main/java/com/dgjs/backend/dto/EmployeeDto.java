package com.dgjs.backend.dto;

import com.dgjs.backend.domain.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class EmployeeDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String name;
        private String email;
        private String deptCode;
        private String jobTitle;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String email;
        private String deptCode;
        private String jobTitle;
        private String role;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String empNo;
        private String name;
        private String email;
        private String deptCode;
        private String deptName;
        private String jobTitle;
        private String role;
        private String status;
        private boolean mustChangePassword;
        private LocalDateTime createdAt;

        // 부서가 지연 로딩이므로 트랜잭션 안에서 호출해야 함
        public static Response from(Employee e) {
            return Response.builder()
                    .empNo(e.getEmpNo())
                    .name(e.getName())
                    .email(e.getEmail())
                    .deptCode(e.getDepartment().getDeptCode())
                    .deptName(e.getDepartment().getName())
                    .jobTitle(e.getJobTitle())
                    .role(e.getRole())
                    .status(e.getStatus())
                    .mustChangePassword(e.isMustChangePassword())
                    .createdAt(e.getCreatedAt())
                    .build();
        }
    }

    // 임시 비밀번호는 이 응답에서만 평문으로 내려가며 다시 조회할 수 없음
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedResponse {
        private Response employee;
        private String tempPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetResponse {
        private String tempPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse {
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private List<Response> employees;
    }
}
