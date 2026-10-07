package com.dgjs.backend.repository;

import com.dgjs.backend.domain.DatasetMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetMetaRepository extends JpaRepository<DatasetMeta, Long> {
    List<DatasetMeta> findAllByOrderByCreatedAtDesc();
}
