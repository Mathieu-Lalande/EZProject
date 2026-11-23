package com.ezproject.todo;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<TodoItem, Long> {

    @EntityGraph(attributePaths = {"owner", "project"})
    List<TodoItem> findByOwnerIdAndProjectIsNullOrderByDoneAscCreatedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = {"owner", "project"})
    List<TodoItem> findByProjectIdOrderByDoneAscCreatedAtDesc(Long projectId);
}
