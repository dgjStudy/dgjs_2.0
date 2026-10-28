package com.dgjs.backend.repository;

import com.dgjs.backend.domain.DatasetMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetMetaRepository extends JpaRepository<DatasetMeta, Long> {
    List<DatasetMeta> findAllByOrderByCreatedAtDesc();

    @Query("select m from DatasetMeta m left join fetch m.project where m.status is null or m.status = 'READY' order by m.createdAt desc")
    List<DatasetMeta> findAllReadyOrderByCreatedAtDesc();

    // 프로젝트를 삭제하기 전에 연결된 데이터셋을 미지정으로 바꿈
    @Modifying
    @Query(value = "update dataset_meta set project_code = null where project_code = :projectCode", nativeQuery = true)
    int clearProject(@Param("projectCode") String projectCode);

    // 소유자가 비었거나 사원에 없는 데이터셋(기존 mock 값 등)을 지정한 사원에게 넘김
    @Modifying
    @Query(value = "update dataset_meta set owner_id = :empNo where owner_id is null or owner_id not in (select emp_no from employee)", nativeQuery = true)
    int reassignUnknownOwners(@Param("empNo") String empNo);
}
