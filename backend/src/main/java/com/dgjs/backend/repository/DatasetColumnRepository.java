package com.dgjs.backend.repository;

import com.dgjs.backend.domain.DatasetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, Long> {
    List<DatasetColumn> findByDatasetIdOrderBySortOrderAsc(Long datasetId);
    @Modifying
    @Query("delete from DatasetColumn c where c.datasetId = :datasetId")
    void deleteByDatasetId(@Param("datasetId") Long datasetId);

    @Query("select c.datasetId as datasetId, count(c) as cnt from DatasetColumn c where c.datasetId in :ids group by c.datasetId")
    List<DatasetCount> countGroupByDatasetId(@Param("ids") Collection<Long> ids);
}
