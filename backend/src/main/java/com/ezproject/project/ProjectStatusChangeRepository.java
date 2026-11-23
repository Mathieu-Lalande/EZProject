package com.ezproject.project;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStatusChangeRepository extends JpaRepository<ProjectStatusChange, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<ProjectStatusChange> findByProjectIdOrderByChangedAtDesc(Long projectId);
}
