package com.dgjs.backend.repository;

import com.dgjs.backend.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndEmpNoNot(String email, String empNo);

    boolean existsByDepartment_DeptCode(String deptCode);

    long countByRoleAndStatus(String role, String status);

    @Query("select max(e.empNo) from Employee e")
    Optional<String> findMaxCode();

    @Query(value = "select e from Employee e join fetch e.department d "
            + "where (:deptCode = '' or d.deptCode = :deptCode) "
            + "and (lower(e.name) like lower(concat('%', :keyword, '%')) or lower(e.email) like lower(concat('%', :keyword, '%')))",
            countQuery = "select count(e) from Employee e "
            + "where (:deptCode = '' or e.department.deptCode = :deptCode) "
            + "and (lower(e.name) like lower(concat('%', :keyword, '%')) or lower(e.email) like lower(concat('%', :keyword, '%')))")
    Page<Employee> search(@Param("keyword") String keyword, @Param("deptCode") String deptCode, Pageable pageable);

    @Query("select e.department.deptCode as code, count(e) as cnt from Employee e group by e.department.deptCode")
    List<CodeCount> countGroupByDeptCode();
}
