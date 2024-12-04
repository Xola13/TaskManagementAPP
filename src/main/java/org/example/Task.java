package org.example;

import java.time.LocalDateTime;

public class Task {
    private int id;  // Task ID
    private String taskName;
    private String category;
    private String description;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;

    // Constructor for creating a new task (without specifying the ID)
    public Task(String taskName, String category, String description, boolean completed, LocalDateTime createdAt, LocalDateTime deadline) {
        this.taskName = taskName;
        this.category = category;
        this.description = description;
        this.completed = completed;
        this.createdAt = createdAt;
        this.deadline = deadline;
        this.id = -1;  // Set the ID to a default invalid value for new tasks
    }

    // Constructor for updating an existing task (with the specified ID)
    public Task(int id, String taskName, String category, String description, boolean completed, LocalDateTime createdAt, LocalDateTime deadline) {
        if (id <= 0) {
            throw new IllegalArgumentException("Task ID must be a positive integer.");
        }
        this.id = id;
        this.taskName = taskName;
        this.category = category;
        this.description = description;
        this.completed = completed;
        this.createdAt = createdAt;
        this.deadline = deadline;
    }

    // Getter and setter methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Task ID must be a positive integer.");
        }
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
}
