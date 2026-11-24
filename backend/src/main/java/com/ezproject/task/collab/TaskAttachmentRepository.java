package com.ezproject.task.collab;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    @EntityGraph(attributePaths = "uploader")
    List<TaskAttachment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    long countByTaskId(Long taskId);

    @Query("select a.task.id as taskId, count(a) as total from TaskAttachment a where a.task.project.id = :projectId group by a.task.id")
    List<TaskCounted> countByTask(@Param("projectId") Long projectId);
}
