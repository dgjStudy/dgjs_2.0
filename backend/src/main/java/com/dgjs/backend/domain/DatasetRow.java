package com.dgjs.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dataset_row")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "row_id")
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @Column(name = "data_json", columnDefinition = "LONGTEXT", nullable = false)
    private String dataJson;
}
