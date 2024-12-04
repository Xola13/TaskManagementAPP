package org.example;

import java.sql.SQLException;
import java.util.List;

public interface TaskDao {
    void insertTask(Task task) throws SQLException;
    List<Task> getAllTasks() throws SQLException;
    void updateTask(Task task) throws SQLException;
    void deleteTask(int taskId) throws SQLException;
}
