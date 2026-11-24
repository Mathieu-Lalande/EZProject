package com.ezproject.report;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StatusReportRepository extends JpaRepository<StatusReport, Long> {

    @EntityGraph(attributePaths = "author")
    List<StatusReport> findByProjectIdOrderByReportDateDescIdDesc(Long projectId, Pageable pageable);

    /** Dernier rapport de chaque projet (vue portefeuille). */
    @Query("select r from StatusReport r where r.project.id in :projectIds and r.id = "
            + "(select max(r2.id) from StatusReport r2 where r2.project.id = r.project.id)")
    List<StatusReport> latestFor(@Param("projectIds") List<Long> projectIds);
}
