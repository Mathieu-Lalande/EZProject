package com.ezproject.access;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findByProjectId(Long projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
}
