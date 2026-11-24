package com.ezproject.task.collab;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = "author")
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    @Query("select c.task.id as taskId, count(c) as total from TaskComment c where c.task.project.id = :projectId group by c.task.id")
    List<TaskCounted> countByTask(@Param("projectId") Long projectId);

    List<TaskComment> findByAuthorId(Long authorId);
}
