package org.example;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DatabaseHelper {
    private static final String URL = "jdbc:sqlite:tasks.db";
    private static final Logger logger = Logger.getLogger(DatabaseHelper.class.getName());

    // SQL query to check if the table exists
    private static final String TABLE_EXISTS_SQL = "SELECT name FROM sqlite_master WHERE type='table' AND name='tasks';";

    // SQL query to drop and create the table
    private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS tasks;";
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "task_name TEXT NOT NULL," +
                    "category TEXT," +
                    "description TEXT," +
                    "completed BOOLEAN," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "deadline TIMESTAMP" +
                    ");";

    // Method to get the database connection
    public static Connection getConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL);
            if (connection != null) {
                logger.info("Database connection successful.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to connect to the database: " + e.getMessage(), e);
            throw e;
        }
        return connection;
    }

    // Convert LocalDateTime to Timestamp
    public static Timestamp convertToTimestamp(LocalDateTime localDateTime) {
        return Timestamp.valueOf(localDateTime);
    }

    // Convert Timestamp to LocalDateTime
    public static LocalDateTime convertToLocalDateTime(Timestamp timestamp) {
        return timestamp.toLocalDateTime();
    }

    // Method to check if the tasks table exists
    public static boolean checkIfTableExists(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(TABLE_EXISTS_SQL);
            return rs.next(); // Returns true if the table exists
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking table existence: " + e.getMessage(), e);
            return false;
        }
    }

    // Method to drop the tasks table (used for specific scenarios like development)
    public static void dropTable() throws SQLException {
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(DROP_TABLE_SQL);
            logger.info("Dropped tasks table.");
        }
    }

    // Method to create the tasks table if it doesn't already exist
    public static void createTable() throws SQLException {
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(CREATE_TABLE_SQL);
            logger.info("Created tasks table.");
        }
    }

    // Method to delete invalid tasks (tasks with NULL or 0 ID)
    public static void deleteInvalidTasks() {
        String deleteQuery = "DELETE FROM tasks WHERE id IS NULL OR id = 0";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Successfully deleted " + rowsAffected + " invalid task(s).");
            } else {
                logger.info("No invalid tasks found to delete.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting invalid tasks: " + e.getMessage(), e);
        }
    }

    // Method to close connection
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Connection closed successfully.");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing connection: " + e.getMessage(), e);
            }
        }
    }

    // Method to check and add missing columns if necessary
    public static void checkAndAddMissingColumns(Connection connection) {
        checkAndAddColumn(connection, "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        checkAndAddColumn(connection, "deadline", "TIMESTAMP");
    }

    // Generic method to check and add a column if it doesn't exist
    public static void checkAndAddColumn(Connection connection, String columnName, String columnDefinition) {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(tasks);");
            boolean columnExists = false;
            while (rs.next()) {
                if (rs.getString("name").equals(columnName)) {
                    columnExists = true;
                    break;
                }
            }

            if (!columnExists) {
                String addColumnSQL = "ALTER TABLE tasks ADD COLUMN " + columnName + " " + columnDefinition;
                stmt.executeUpdate(addColumnSQL);
                logger.info("Column '" + columnName + "' added successfully.");
                if (columnName.equals("created_at")) {
                    String updateSQL = "UPDATE tasks SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL";
                    stmt.executeUpdate(updateSQL);
                    logger.info("Existing rows updated with default 'created_at' value.");
                }
            } else {
                logger.info("Column '" + columnName + "' already exists.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking or adding column '" + columnName + "': " + e.getMessage(), e);
        }
    }
}
