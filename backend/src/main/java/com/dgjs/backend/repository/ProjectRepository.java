package com.dgjs.backend.repository;

import com.dgjs.backend.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findAllByOrderByProjectCodeAsc();

    boolean existsByName(String name);

    boolean existsByNameAndProjectCodeNot(String name, String projectCode);

    @Query("select max(p.projectCode) from Project p")
    Optional<String> findMaxCode();
}
