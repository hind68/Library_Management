package com.smartlibrary;

import com.smartlibrary.controller.LoginController;
import com.smartlibrary.dao.DatabaseManager;
import com.smartlibrary.service.LibraryService;
import com.smartlibrary.service.NotificationService;
import com.smartlibrary.service.ReportService;
import com.smartlibrary.service.UserService;
import com.smartlibrary.util.SessionManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    // Starts the JavaFX application and opens the first screen.
    // Parameters: stage is the main JavaFX window.
    public void start(Stage stage) {
        DatabaseManager databaseManager = new DatabaseManager();
        LibraryService libraryService = new LibraryService(databaseManager);
        UserService userService = new UserService(databaseManager);
        NotificationService notificationService = new NotificationService(databaseManager);
        ReportService reportService = new ReportService(libraryService, userService);
        SessionManager sessionManager = new SessionManager();

        LoginController loginController = new LoginController(
                stage, userService, libraryService, notificationService, reportService, sessionManager);
        Scene scene = new Scene(loginController.getView(), 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("Smart Library Management System");
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    // Launches the application from the command line.
    // Parameters: args is command-line arguments.
    public static void main(String[] args) {
        launch(args);
    }
}
