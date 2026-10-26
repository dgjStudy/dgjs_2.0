package com.dgjs.backend.repository;

import com.dgjs.backend.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    List<Department> findAllByOrderByDeptCodeAsc();

    boolean existsByName(String name);

    boolean existsByNameAndDeptCodeNot(String name, String deptCode);

    Optional<Department> findByName(String name);

    @Query("select max(d.deptCode) from Department d")
    Optional<String> findMaxCode();
}
