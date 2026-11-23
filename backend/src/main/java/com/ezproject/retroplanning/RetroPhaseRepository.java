package com.ezproject.retroplanning;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RetroPhaseRepository extends JpaRepository<RetroPhase, Long> {

    List<RetroPhase> findByProjectIdOrderBySortOrderAsc(Long projectId);

    List<RetroPhase> findByProjectIdInOrderBySortOrderAsc(List<Long> projectIds);
}
