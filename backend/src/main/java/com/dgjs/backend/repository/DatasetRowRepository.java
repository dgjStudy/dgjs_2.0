package com.dgjs.backend.repository;

import com.dgjs.backend.domain.DatasetRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetRowRepository extends JpaRepository<DatasetRow, Long> {
    List<DatasetRow> findByDatasetIdOrderByRowIndexAsc(Long datasetId);
    Page<DatasetRow> findByDatasetIdOrderByRowIndexAsc(Long datasetId, Pageable pageable);
    long countByDatasetId(Long datasetId);
    void deleteByDatasetId(Long datasetId);
    Optional<DatasetRow> findByIdAndDatasetId(Long id, Long datasetId);
    Optional<DatasetRow> findFirstByDatasetIdOrderByRowIndexDesc(Long datasetId);
}
