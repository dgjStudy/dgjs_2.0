package com.dgjs.backend.repository;

import com.dgjs.backend.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("select m from ProjectMember m join fetch m.employee e join fetch e.department "
            + "where m.project.projectCode = :projectCode order by e.empNo")
    List<ProjectMember> findByProjectCode(@Param("projectCode") String projectCode);

    boolean existsByProject_ProjectCodeAndEmployee_EmpNo(String projectCode, String empNo);

    Optional<ProjectMember> findByProject_ProjectCodeAndEmployee_EmpNo(String projectCode, String empNo);

    @Modifying
    @Query("delete from ProjectMember m where m.project.projectCode = :projectCode")
    void deleteByProjectCode(@Param("projectCode") String projectCode);

    @Query("select m.project.projectCode as code, count(m) as cnt from ProjectMember m group by m.project.projectCode")
    List<CodeCount> countGroupByProjectCode();
}
