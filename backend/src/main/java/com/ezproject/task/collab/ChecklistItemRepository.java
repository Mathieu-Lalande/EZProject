package com.ezproject.task.collab;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByTaskIdOrderByPositionAsc(Long taskId);

    @Query("select i.task.id as taskId, count(i) as total from ChecklistItem i where i.task.project.id = :projectId group by i.task.id")
    List<TaskCounted> countByTask(@Param("projectId") Long projectId);

    @Query("select i.task.id as taskId, count(i) as total from ChecklistItem i where i.task.project.id = :projectId and i.done = true group by i.task.id")
    List<TaskCounted> countDoneByTask(@Param("projectId") Long projectId);
}
