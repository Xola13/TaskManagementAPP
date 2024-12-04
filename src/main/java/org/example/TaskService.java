package org.example;

import java.sql.SQLException;
import java.util.List;

public class TaskService {
    private TaskDao taskDao;

    public TaskService() {
        this.taskDao = new TaskDaoImpl();
    }

    public List<Task> getAllTasks() throws SQLException {
        return taskDao.getAllTasks();
    }

    public void addTask(Task task) throws SQLException {
        taskDao.insertTask(task);
    }

    public void updateTask(Task task) throws SQLException {
        taskDao.updateTask(task);
    }

    public void deleteTask(int taskId) throws SQLException {
        taskDao.deleteTask(taskId);
    }
}
