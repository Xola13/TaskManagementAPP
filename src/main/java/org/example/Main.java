package org.example;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;

public class Main extends Application {

    private TaskController taskController;
    private TableView<Task> taskTable;
    private ObservableList<Task> tasks;
    private Connection connection;

    public static void main(String[] args) {
        // Ensure invalid tasks are deleted on startup
        deleteInvalidTasks();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Step 1: Establish the database connection
            connection = DatabaseHelper.getConnection();
            DatabaseHelper.checkAndAddMissingColumns(connection);

            // Initialize the task controller and observable list
            taskController = new TaskController();
            tasks = taskController.getTaskObservableList();

            // Initialize TableView
            taskTable = new TableView<>();
            taskTable.setItems(tasks);
            taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Define and add columns to the TableView
            addTableColumns();

            // Load tasks from the database
            taskController.loadTasks();

            // Buttons for actions
            Button addButton = new Button("Add Task");
            addButton.setOnAction(event -> showAddTaskDialog());

            Button editButton = new Button("Edit Task");
            editButton.setOnAction(event -> showEditTaskDialog());

            Button deleteButton = new Button("Delete Task");
            deleteButton.setOnAction(event -> deleteSelectedTask());

            Button viewButton = new Button("View Task");
            viewButton.setOnAction(event -> showViewTaskDialog());

            Button exportButton = new Button("Export to CSV");
            exportButton.setOnAction(event -> exportToCSV());

            Button saveToFileButton = new Button("Save Tasks to File");
            saveToFileButton.setOnAction(event -> saveTasksToFile());

            // Layout for buttons
            HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, viewButton, exportButton, saveToFileButton);
            buttonBox.setPadding(new Insets(10));
            buttonBox.setStyle("-fx-alignment: center;");

            // Main layout with TableView and buttons
            VBox layout = new VBox(10, taskTable, buttonBox);
            layout.setPadding(new Insets(10));
            VBox.setVgrow(taskTable, Priority.ALWAYS);

            // Scene and styling
            Scene scene = new Scene(layout, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/Style.css").toExternalForm());

            primaryStage.setTitle("HOME CHORES !!");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (SQLException e) {
            showError("Database Connection Error", "Failed to connect to the database: " + e.getMessage());
        }
    }

    @Override
    public void stop() throws Exception {
        DatabaseHelper.closeConnection(connection);
        super.stop();
    }

    private void addTableColumns() {
        // Define and add columns to the TableView
        TableColumn<Task, String> nameColumn = new TableColumn<>("Task Name");
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getTaskName()));
        nameColumn.setPrefWidth(150);

        TableColumn<Task, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCategory()));
        categoryColumn.setPrefWidth(150);

        TableColumn<Task, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDescription()));
        descriptionColumn.setPrefWidth(200);

        TableColumn<Task, String> createdAtColumn = new TableColumn<>("Created At");
        createdAtColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCreatedAt().toString()));
        createdAtColumn.setPrefWidth(150);

        TableColumn<Task, String> deadlineColumn = new TableColumn<>("Deadline");
        deadlineColumn.setCellValueFactory(param -> {
            LocalDateTime deadline = param.getValue().getDeadline();
            return new SimpleStringProperty(deadline != null ? deadline.toString() : "No Deadline");
        });
        deadlineColumn.setPrefWidth(150);

        // Add all columns to the TableView
        taskTable.getColumns().addAll(nameColumn, categoryColumn, descriptionColumn, createdAtColumn, deadlineColumn);
    }


    private void showAddTaskDialog() {
        TextField taskNameField = new TextField();
        taskNameField.setPromptText("Task Name");

        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Description");

        DatePicker deadlinePicker = new DatePicker();
        deadlinePicker.setPromptText("Deadline");

        CheckBox completedCheckBox = new CheckBox("Completed");

        VBox dialogLayout = new VBox(10, taskNameField, categoryField, descriptionField, deadlinePicker, completedCheckBox);
        dialogLayout.setPadding(new Insets(10));

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add New Task");
        dialog.getDialogPane().setContent(dialogLayout);

        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, cancelButton);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                String taskName = taskNameField.getText();
                String category = categoryField.getText();
                String description = descriptionField.getText();
                boolean completed = completedCheckBox.isSelected();
                LocalDateTime deadline = deadlinePicker.getValue() != null ? deadlinePicker.getValue().atStartOfDay() : null;

                Task newTask = new Task(taskName, category, description, completed, LocalDateTime.now(), deadline);

                try {
                    taskController.addTask(newTask);  // Add task to the database
                    taskController.loadTasks();  // Reload tasks to update the TableView
                    taskTable.setItems(taskController.getTaskObservableList());
                    taskTable.refresh();  // Force refresh of TableView
                } catch (Exception e) {
                    showError("Error adding task", e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteSelectedTask() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            int taskId = selectedTask.getId();
            if (taskId > 0) {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Deletion");
                confirmation.setHeaderText("Are you sure you want to delete this task?");
                confirmation.setContentText("Task: " + selectedTask.getTaskName() + "\nDescription: " + selectedTask.getDescription());

                confirmation.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            taskController.deleteTask(taskId);  // Delete task from the database
                            taskController.loadTasks();  // Reload tasks to update the TableView
                            taskTable.setItems(taskController.getTaskObservableList());
                            taskTable.refresh();  // Force refresh of TableView
                            showInfo("Task Deleted", "Task successfully deleted.");
                        } catch (Exception e) {
                            showError("Error Deleting Task", "An error occurred while deleting the task: " + e.getMessage());
                        }
                    }
                });
            } else {
                showError("Invalid Task", "The selected task has an invalid ID and cannot be deleted.");
            }
        } else {
            showError("No Task Selected", "Please select a task to delete.");
        }
    }

    private void showViewTaskDialog() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            String deadline = (selectedTask.getDeadline() != null)
                    ? selectedTask.getDeadline().toString()
                    : "No Deadline";

            String message = "Task Name: " + selectedTask.getTaskName() + "\n" +
                    "Category: " + selectedTask.getCategory() + "\n" +
                    "Description: " + selectedTask.getDescription() + "\n" +
                    "Created At: " + selectedTask.getCreatedAt() + "\n" +
                    "Deadline: " + deadline + "\n" +
                    "Completed: " + (selectedTask.isCompleted() ? "Yes" : "No");

            showInfo("Task Details", message);
        } else {
            showError("No Task Selected", "Please select a task to view.");
        }
    }

    private void showEditTaskDialog() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            // Create UI elements for editing the task
            TextField taskNameField = new TextField(selectedTask.getTaskName());
            taskNameField.setPromptText("Task Name");

            TextField categoryField = new TextField(selectedTask.getCategory());
            categoryField.setPromptText("Category");

            TextField descriptionField = new TextField(selectedTask.getDescription());
            descriptionField.setPromptText("Description");

            DatePicker deadlinePicker = new DatePicker(selectedTask.getDeadline() != null
                    ? selectedTask.getDeadline().toLocalDate()
                    : null);
            deadlinePicker.setPromptText("Deadline");

            CheckBox completedCheckBox = new CheckBox("Completed");
            completedCheckBox.setSelected(selectedTask.isCompleted());

            // Create the dialog layout
            VBox dialogLayout = new VBox(10, taskNameField, categoryField, descriptionField, deadlinePicker, completedCheckBox);
            dialogLayout.setPadding(new Insets(10));

            // Create the dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Edit Task");
            dialog.getDialogPane().setContent(dialogLayout);

            // Add buttons to the dialog
            ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

            // Set the result converter
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButton) {
                    // Update the selected task with the new values
                    selectedTask.setTaskName(taskNameField.getText());
                    selectedTask.setCategory(categoryField.getText());
                    selectedTask.setDescription(descriptionField.getText());
                    selectedTask.setDeadline(deadlinePicker.getValue() != null
                            ? deadlinePicker.getValue().atStartOfDay()
                            : null);
                    selectedTask.setCompleted(completedCheckBox.isSelected());

                    try {
                        // Update the task in the database
                        taskController.updateTask(selectedTask);
                        // Reload the tasks from the database
                        taskController.loadTasks();
                        // Refresh the TableView with the updated data
                        taskTable.setItems(taskController.getTaskObservableList());
                        taskTable.refresh();  // Force refresh of TableView
                    } catch (Exception e) {
                        showError("Error Updating Task", e.getMessage());
                    }
                }
                return null;
            });

            dialog.showAndWait();
        } else {
            showError("No Task Selected", "Please select a task to edit.");
        }
    }

    private void saveTasksToFile() {
        // File chooser to allow user to select where to save the file
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Write the header to the file
                writer.write("ID | Task Name | Category | Description | Created At | Deadline | Completed\n");
                writer.write("------------------------------------------------------------\n");

                // Retrieve tasks from the database and write them to the text file
                try (Statement stmt = connection.createStatement()) {
                    String query = "SELECT * FROM tasks";
                    ResultSet rs = stmt.executeQuery(query);

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String taskName = rs.getString("task_name");
                        String category = rs.getString("category");
                        String description = rs.getString("description");
                        long createdAtMillis = rs.getLong("created_at"); // Get long value (Unix timestamp in milliseconds)
                        long deadlineMillis = rs.getLong("deadline");  // Get long value (Unix timestamp in milliseconds)
                        boolean completed = rs.getBoolean("completed");

                        // Convert Unix timestamps to LocalDateTime
                        String createdAtStr = (createdAtMillis > 0) ? Instant.ofEpochMilli(createdAtMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().toString() : "N/A";
                        String deadlineStr = (deadlineMillis > 0) ? Instant.ofEpochMilli(deadlineMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().toString() : "N/A";

                        // Format the task and write it to the file
                        String taskLine = String.format("%d | %s | %s | %s | %s | %s | %s\n",
                                id,
                                taskName,
                                category != null ? category : "N/A",
                                description != null ? description : "N/A",
                                createdAtStr,
                                deadlineStr,
                                completed ? "Yes" : "No");

                        writer.write(taskLine);
                    }

                    // Notify user that the tasks have been saved successfully
                    showInfo("Save Successful", "Tasks have been successfully saved to the file.");

                } catch (SQLException e) {
                    showError("Database Error", "An error occurred while retrieving tasks: " + e.getMessage());
                }
            } catch (IOException e) {
                showError("File Error", "An error occurred while saving the file: " + e.getMessage());
            }
        }
    }



    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Write header
                writer.write("Task Name, Category, Description, Created At, Deadline, Completed\n");

                // Write task data
                for (Task task : taskTable.getItems()) {
                    writer.write(String.format("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\"\n",
                            task.getTaskName(),
                            task.getCategory(),
                            task.getDescription(),
                            task.getCreatedAt(),
                            task.getDeadline(),
                            task.isCompleted() ? "Yes" : "No"));
                }

                showInfo("Export Successful", "Tasks have been successfully exported to CSV.");

            } catch (IOException e) {
                showError("Export Failed", "An error occurred while exporting tasks: " + e.getMessage());
            }
        }
    }

    // Helper method to delete invalid tasks at startup
    private static void deleteInvalidTasks() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "DELETE FROM tasks WHERE id IS NULL OR task_name IS NULL OR task_name = ''";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
