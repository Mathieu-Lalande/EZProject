package com.ezproject.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(Long organizationId, String code);

    Optional<Project> findByOrganizationIdAndCodeIgnoreCase(Long organizationId, String code);

    @EntityGraph(attributePaths = {"owner", "team"})
    List<Project> findByOrganizationIdOrderByNameAsc(Long organizationId);

    long countByOrganizationId(Long organizationId);
}
