package com.ezproject.risk;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    @EntityGraph(attributePaths = "owner")
    List<Risk> findByProjectId(Long projectId);

    List<Risk> findByProjectIdIn(List<Long> projectIds);
}
