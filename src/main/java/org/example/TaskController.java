package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;

public class TaskController {
    private static final Logger logger = Logger.getLogger(TaskController.class.getName());
    private ObservableList<Task> tasks = FXCollections.observableArrayList();

    // Method to add a task to the database
    public void addTask(Task task) {
        String insertQuery = "INSERT INTO tasks (task_name, category, description, completed, created_at, deadline) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, task.getTaskName());
            statement.setString(2, task.getCategory());
            statement.setString(3, task.getDescription());
            statement.setBoolean(4, task.isCompleted());
            statement.setTimestamp(5, Timestamp.valueOf(task.getCreatedAt()));
            statement.setTimestamp(6, task.getDeadline() != null ? Timestamp.valueOf(task.getDeadline()) : null);

            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        task.setId(generatedId);  // Set the generated ID to the task
                        logger.info("Task added successfully with ID: " + task.getId());
                    }
                }
            } else {
                logger.warning("No rows affected while adding the task.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding task: " + e.getMessage(), e);
        }
    }

    // Method to update an existing task in the database
    public void updateTask(Task task) {
        if (task == null || task.getId() <= 0) {
            logger.warning("Attempted to update task with invalid ID: " + (task != null ? task.getId() : "null"));
            return;
        }

        logger.info("Updating task with ID: " + task.getId());

        String updateQuery = "UPDATE tasks SET task_name = ?, category = ?, description = ?, completed = ?, created_at = ?, deadline = ? WHERE id = ?";
        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateQuery)) {

            statement.setString(1, task.getTaskName());
            statement.setString(2, task.getCategory());
            statement.setString(3, task.getDescription());
            statement.setBoolean(4, task.isCompleted());
            statement.setTimestamp(5, Timestamp.valueOf(task.getCreatedAt()));
            statement.setTimestamp(6, task.getDeadline() != null ? Timestamp.valueOf(task.getDeadline()) : null);
            statement.setInt(7, task.getId());

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Task updated successfully with ID: " + task.getId());
            } else {
                logger.warning("No task found with ID: " + task.getId());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating task with ID: " + task.getId(), e);
        }
    }

    // Method to delete a task from the database
    public void deleteTask(int taskId) throws SQLException {
        String query = "DELETE FROM tasks WHERE id = ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, taskId);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Task deleted successfully.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting task: " + e.getMessage(), e);
            throw e;
        }
    }

    // Method to load all tasks from the database
    public void loadTasks() throws SQLException {
        String query = "SELECT * FROM tasks ORDER BY created_at DESC";

        try (Connection connection = DatabaseHelper.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            tasks.clear();

            while (resultSet.next()) {
                int taskId = resultSet.getInt("id");
                if (taskId <= 0) {
                    logger.warning("Invalid task ID found: " + taskId);
                    continue;
                }

                // Convert and add task to the ObservableList
                long createdAtMillis = resultSet.getLong("created_at");
                LocalDateTime createdAt = convertMillisToLocalDateTime(createdAtMillis);
                long deadlineMillis = resultSet.getLong("deadline");
                LocalDateTime deadline = resultSet.wasNull() ? null : convertMillisToLocalDateTime(deadlineMillis);

                Task task = new Task(
                        taskId,
                        resultSet.getString("task_name"),
                        resultSet.getString("category"),
                        resultSet.getString("description"),
                        resultSet.getBoolean("completed"),
                        createdAt,
                        deadline
                );
                tasks.add(task);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading tasks: " + e.getMessage(), e);
            throw e;
        }
    }

    // Helper method to convert milliseconds to LocalDateTime
    private LocalDateTime convertMillisToLocalDateTime(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    // Method to retrieve the ObservableList of tasks
    public ObservableList<Task> getTaskObservableList() {
        return tasks;
    }

    // Method to retrieve a specific task by its ID
    public Task getTaskById(int taskId) throws SQLException {
        String query = "SELECT * FROM tasks WHERE id = ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, taskId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Task(
                            resultSet.getInt("id"),  // Retrieve and set the ID
                            resultSet.getString("task_name"),
                            resultSet.getString("category"),
                            resultSet.getString("description"),
                            resultSet.getBoolean("completed"),
                            resultSet.getTimestamp("created_at").toLocalDateTime(),
                            resultSet.getTimestamp("deadline") != null
                                    ? resultSet.getTimestamp("deadline").toLocalDateTime() : null
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving task by ID: " + e.getMessage(), e);
            throw e;
        }
        return null;
    }

    // Method to mark a task as completed in the database
    public void markTaskAsCompleted(int taskId) throws SQLException {
        String query = "UPDATE tasks SET completed = TRUE WHERE id = ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, taskId);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Task marked as completed.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error marking task as completed: " + e.getMessage(), e);
            throw e;
        }
    }

    // Method to mark a task as incomplete in the database
    public void markTaskAsIncomplete(int taskId) throws SQLException {
        String query = "UPDATE tasks SET completed = FALSE WHERE id = ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, taskId);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Task marked as incomplete.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error marking task as incomplete: " + e.getMessage(), e);
            throw e;
        }
    }

    // Method to search for tasks by name or description
    public ObservableList<Task> searchTasks(String query) throws SQLException {
        ObservableList<Task> searchResults = FXCollections.observableArrayList();
        String searchQuery = "SELECT * FROM tasks WHERE task_name LIKE ? OR description LIKE ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement statement = connection.prepareStatement(searchQuery)) {

            statement.setString(1, "%" + query + "%");
            statement.setString(2, "%" + query + "%");

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Task task = new Task(
                            resultSet.getInt("id"),  // Retrieve and set the ID
                            resultSet.getString("task_name"),
                            resultSet.getString("category"),
                            resultSet.getString("description"),
                            resultSet.getBoolean("completed"),
                            resultSet.getTimestamp("created_at").toLocalDateTime(),
                            resultSet.getTimestamp("deadline") != null
                                    ? resultSet.getTimestamp("deadline").toLocalDateTime() : null
                    );
                    searchResults.add(task);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching tasks: " + e.getMessage(), e);
            throw e;
        }
        return searchResults;
    }
}
