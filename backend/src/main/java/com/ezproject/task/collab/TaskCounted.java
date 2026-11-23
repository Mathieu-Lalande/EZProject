package com.ezproject.task.collab;

/** Projection : nombre d'éléments par tâche. */
public interface TaskCounted {
    Long getTaskId();
    long getTotal();
}
