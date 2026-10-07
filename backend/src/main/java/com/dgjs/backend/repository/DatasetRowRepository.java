package com.dgjs.backend.repository;

import com.dgjs.backend.domain.DatasetRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRowRepository extends JpaRepository<DatasetRow, Long> {
    List<DatasetRow> findByDatasetIdOrderByRowIndexAsc(Long datasetId);
    long countByDatasetId(Long datasetId);
    void deleteByDatasetId(Long datasetId);
}
