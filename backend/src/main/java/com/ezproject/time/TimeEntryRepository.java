package com.ezproject.time;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    @EntityGraph(attributePaths = {"user", "task"})
    Page<TimeEntry> findByProjectIdOrderByWorkDateDescIdDesc(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "task"})
    List<TimeEntry> findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(Long userId, LocalDate from, LocalDate to);

    List<TimeEntry> findByUserIdAndProjectIdAndWorkDateAndTaskIsNull(Long userId, Long projectId, LocalDate date);

    @Query("select coalesce(sum(t.days), 0) from TimeEntry t where t.user.id = :userId and t.workDate = :date "
            + "and (:excludeId is null or t.id <> :excludeId)")
    BigDecimal sumForUserOnDate(@Param("userId") Long userId, @Param("date") LocalDate date,
                                @Param("excludeId") Long excludeId);

    @Query("select t.project.id as projectId, sum(t.days) as days from TimeEntry t "
            + "where t.project.id in :projectIds group by t.project.id")
    List<ProjectDays> sumByProject(@Param("projectIds") List<Long> projectIds);

    @Query("select t.user.id as userId, t.user.displayName as name, sum(t.days) as days from TimeEntry t "
            + "where t.project.id = :projectId group by t.user.id, t.user.displayName order by sum(t.days) desc")
    List<UserDays> sumByUser(@Param("projectId") Long projectId);

    @Query("select t.workDate as date, t.days as days from TimeEntry t where t.project.id = :projectId")
    List<DatedDays> datedDays(@Param("projectId") Long projectId);

    @EntityGraph(attributePaths = {"project"})
    List<TimeEntry> findByUserIdOrderByWorkDateDesc(Long userId);

    interface ProjectDays {
        Long getProjectId();
        BigDecimal getDays();
    }

    interface UserDays {
        Long getUserId();
        String getName();
        BigDecimal getDays();
    }

    interface DatedDays {
        LocalDate getDate();
        BigDecimal getDays();
    }
}
