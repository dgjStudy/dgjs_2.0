package com.dgjs.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 프로젝트와 사원의 N:M 관계를 풀어 주는 연결 테이블임
@Entity
@Table(name = "project_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_project_member", columnNames = {"project_code", "emp_no"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_code", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emp_no", nullable = false)
    private Employee employee;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
