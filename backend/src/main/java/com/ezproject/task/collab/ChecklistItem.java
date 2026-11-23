package com.ezproject.task.collab;

import com.ezproject.task.Task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Sous-tâche (élément de checklist) d'une tâche. */
@Entity
@Table(name = "task_checklist_item")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private int position;

    protected ChecklistItem() {
        // JPA
    }

    public ChecklistItem(Task task, String title, int position) {
        this.task = task;
        this.title = title;
        this.position = position;
    }

    public void update(String newTitle, boolean newDone) {
        this.title = newTitle;
        this.done = newDone;
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public String getTitle() { return title; }
    public boolean isDone() { return done; }
    public int getPosition() { return position; }
}
