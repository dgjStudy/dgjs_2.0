package com.dgjs.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dataset_column", indexes = @Index(name = "idx_dataset_column_dataset_id", columnList = "dataset_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "column_id")
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "column_key", nullable = false, length = 100)
    private String columnKey;

    @Column(name = "column_name", nullable = false, length = 150)
    private String columnName;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
