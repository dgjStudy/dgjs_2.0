package com.dgjs.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    // 소유자 사번임. 기존 값(user_admin) 때문에 외래키는 두지 않고 서비스가 사원 존재를 검사함
    @Column(name = "owner_id", length = 50)
    private String ownerId;

    // 연결된 프로젝트임. NULL이면 미지정이며 프로젝트 참여자 권한이 적용되지 않음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code")
    private Project project;

    // 컬럼 추가 전 기존 데이터는 NULL이며 READY를 의미함
    @Column(length = 20)
    @Builder.Default
    private String status = STATUS_READY;

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 적재 완료·행 추가 시각임. 컬럼 추가 전 데이터와 빈 수동 데이터셋은 NULL이며 응답에서 등록일로 대체함
    @Column(name = "last_loaded_at")
    private LocalDateTime lastLoadedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
