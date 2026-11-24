package com.ezproject.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"assignee", "project", "labels"})
    List<Task> findByProjectIdOrderByPositionAsc(Long projectId);

    List<Task> findByProjectIdAndStatusOrderByPositionAsc(Long projectId, TaskStatus status);

    @EntityGraph(attributePaths = {"assignee", "project"})
    List<Task> findByAssigneeIdAndStatusNotOrderByDueDateAsc(Long assigneeId, TaskStatus status);

    @EntityGraph(attributePaths = {"assignee", "project"})
    List<Task> findByProjectIdInOrderByProjectIdAscStatusAscPositionAsc(List<Long> projectIds);

    int countByProjectIdAndStatus(Long projectId, TaskStatus status);

    /** Nombre de tâches par projet et par statut (vue portefeuille). */
    /** Nombre de tâches par projet et par statut (vue portefeuille), limité aux projets donnés. */
    @Query("select t.project.id as projectId, t.status as status, count(t) as total "
            + "from Task t where t.project.id in :projectIds group by t.project.id, t.status")
    List<StatusCount> countByProjectAndStatus(@Param("projectIds") List<Long> projectIds);

    @Query("select t.project.id as projectId, count(t) as total from Task t "
            + "where t.project.id in :projectIds and t.status <> com.ezproject.task.TaskStatus.DONE "
            + "and t.dueDate < :today group by t.project.id")
    List<ProjectCount> countOverdueByProject(@Param("projectIds") List<Long> projectIds,
                                             @Param("today") LocalDate today);

    @Query("select count(t) from Task t where t.project.id in :projectIds and t.completedAt >= :since")
    long countCompletedSince(@Param("projectIds") List<Long> projectIds, @Param("since") Instant since);

    /** Charge ouverte par personne (tâches non terminées assignées), sur les projets donnés. */
    @Query("select t.assignee.id as userId, count(t) as openTasks, coalesce(sum(t.storyPoints), 0) as openPoints "
            + "from Task t where t.project.id in :projectIds and t.assignee is not null "
            + "and t.status <> com.ezproject.task.TaskStatus.DONE group by t.assignee.id")
    List<Workload> workloadByAssignee(@Param("projectIds") List<Long> projectIds);

    interface StatusCount {
        Long getProjectId();
        TaskStatus getStatus();
        long getTotal();
    }

    interface ProjectCount {
        Long getProjectId();
        long getTotal();
    }

    interface Workload {
        Long getUserId();
        long getOpenTasks();
        long getOpenPoints();
    }
}
