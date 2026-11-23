package com.ezproject.project;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByCodeIgnoreCase(String code);

    @EntityGraph(attributePaths = "owner")
    List<Project> findAllByOrderByNameAsc();
}
