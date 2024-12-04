package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDaoImpl implements TaskDao {

    private Connection connection;

    // Constructor - Establish the database connection
    public TaskDaoImpl() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:tasks.db");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
            e.printStackTrace(); // For better traceability during development
        }
    }

    private void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_name TEXT NOT NULL, " +
                "category TEXT, " +
                "description TEXT, " +
                "completed BOOLEAN, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "deadline TIMESTAMP)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Table 'tasks' created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
            e.printStackTrace(); // For better traceability during development
        }

        // Ensure 'created_at' column exists in the table
        checkAndAddColumn("created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
    }

    private void checkAndAddColumn(String columnName, String columnDefinition) {
        String checkColumnSQL = "PRAGMA table_info(tasks);";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkColumnSQL)) {

            boolean columnExists = false;
            while (rs.next()) {
                if (rs.getString("name").equals(columnName)) {
                    columnExists = true;
                    break;
                }
            }

            if (!columnExists) {
                String alterTableSQL = "ALTER TABLE tasks ADD COLUMN " + columnName + " " + columnDefinition;
                stmt.executeUpdate(alterTableSQL);
                System.out.println("Column '" + columnName + "' added successfully.");
            } else {
                System.out.println("Column '" + columnName + "' already exists.");
            }

        } catch (SQLException e) {
            System.err.println("Error checking or adding column '" + columnName + "': " + e.getMessage());
            e.printStackTrace(); // For better traceability
        }
    }


    @Override
    public void insertTask(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (task_name, category, description, completed, created_at, deadline) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, task.getTaskName());
            pstmt.setString(2, task.getCategory());
            pstmt.setString(3, task.getDescription());
            pstmt.setBoolean(4, task.isCompleted());

            // Handling null deadline
            if (task.getDeadline() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(task.getDeadline()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error inserting task: " + e.getMessage());
            e.printStackTrace(); // For better traceability
            throw new SQLException("Error inserting task into the database", e);
        }
    }

    @Override
    public List<Task> getAllTasks() throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Ensure the Task constructor has the correct number of parameters
                Task task = new Task(
                        rs.getString("task_name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getBoolean("completed"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("deadline") != null ? rs.getTimestamp("deadline").toLocalDateTime() : null
                );
                tasks.add(task);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving tasks: " + e.getMessage());
            e.printStackTrace(); // For better traceability
            throw new SQLException("Error retrieving tasks from the database", e);
        }
        return tasks;
    }

    @Override
    public void updateTask(Task task) throws SQLException {
        String sql = "UPDATE tasks SET task_name = ?, category = ?, description = ?, completed = ?, deadline = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, task.getTaskName());
            pstmt.setString(2, task.getCategory());
            pstmt.setString(3, task.getDescription());
            pstmt.setBoolean(4, task.isCompleted());

            // Handling null deadline
            if (task.getDeadline() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(task.getDeadline()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }

            pstmt.setInt(6, task.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating task: " + e.getMessage());
            e.printStackTrace(); // For better traceability
            throw new SQLException("Error updating task in the database", e);
        }
    }

    @Override
    public void deleteTask(int taskId) throws SQLException {
        if (taskId <= 0) {
            throw new IllegalArgumentException("Invalid task ID: " + taskId);
        }

        // Check if the task exists before attempting to delete
        String checkExistenceSQL = "SELECT 1 FROM tasks WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkExistenceSQL)) {
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();

            // If no rows returned, task does not exist
            if (!rs.next()) {
                throw new SQLException("No task found with ID: " + taskId);
            }
        }

        // Proceed with deletion
        String deleteSQL = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setInt(1, taskId);
            pstmt.executeUpdate();
            System.out.println("Task with ID " + taskId + " deleted successfully.");
        }
    }


    // Method to close the database connection safely
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing the database connection: " + e.getMessage());
            e.printStackTrace(); // For better traceability
        }
    }
}
