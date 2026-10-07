package com.dgjs.backend.repository;

import com.dgjs.backend.domain.DatasetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, Long> {
    List<DatasetColumn> findByDatasetIdOrderBySortOrderAsc(Long datasetId);
    void deleteByDatasetId(Long datasetId);
}
