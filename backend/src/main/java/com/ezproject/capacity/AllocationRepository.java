package com.ezproject.capacity;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    @EntityGraph(attributePaths = "user")
    List<Allocation> findByProjectIdOrderByStartDateAsc(Long projectId);

    @EntityGraph(attributePaths = "project")
    @Query("select a from Allocation a where a.user.id in :userIds and a.endDate >= :from and a.startDate <= :to")
    List<Allocation> overlapping(@Param("userIds") List<Long> userIds, @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);
}
