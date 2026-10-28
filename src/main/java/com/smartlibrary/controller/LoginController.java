package com.smartlibrary.controller;

import com.smartlibrary.model.User;
import com.smartlibrary.service.LibraryService;
import com.smartlibrary.service.NotificationService;
import com.smartlibrary.service.ReportService;
import com.smartlibrary.service.UserService;
import com.smartlibrary.util.SessionManager;
import com.smartlibrary.util.UiUtil;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

public class LoginController {
    private final Stage stage;
    private final UserService userService;
    private final LibraryService libraryService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final SessionManager sessionManager;
    private final StackPane root = new StackPane();
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private Label statusLabel;

    // Creates a LoginController object with the supplied values.
    // Parameters: stage is the main JavaFX window; userService is the helper that manages user accounts
    // Parameters: libraryService is the helper that manages books and borrowings; notificationService is the helper that manages notifications
    // Parameters: reportService is the helper that creates reports; sessionManager is the helper that stores the current login.
    public LoginController(Stage stage, UserService userService, LibraryService libraryService,
                           NotificationService notificationService, ReportService reportService,
                           SessionManager sessionManager) {
        this.stage = stage;
        this.userService = userService;
        this.libraryService = libraryService;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.sessionManager = sessionManager;
        build();
    }

    // Returns the root JavaFX view for this controller.
    public Parent getView() {
        return root;
    }

    // Builds the main layout and connects the screen navigation.
    private void build() {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("login-shell");

        VBox hero = new VBox(18);
        hero.setPadding(new Insets(52));
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("login-hero");
        Label badge = new Label("HCI Final Project");
        badge.getStyleClass().add("badge");
        Label title = new Label("Smart Library\nManagement System");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("A role-based desktop system for catalog management, circulation workflows, analytics, notifications, fines, and academic reporting.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("login-subtitle");
        VBox demo = new VBox(8,
                new Label("Demo accounts"),
                new Label("Admin: admin@library.edu / admin123"),
                new Label("Librarian: librarian@library.edu / lib123"),
                new Label("Member: student@library.edu / student123"));
        demo.getStyleClass().add("demo-box");
        hero.getChildren().addAll(badge, title, subtitle, demo);

        VBox form = new VBox(18);
        form.setPadding(new Insets(44));
        form.setMaxWidth(430);
        form.setAlignment(Pos.CENTER);
        form.getStyleClass().add("login-card");
        Label formTitle = new Label("Secure sign in");
        formTitle.getStyleClass().add("section-title");
        TextField emailField = new TextField("admin@library.edu");
        emailField.setPromptText("Email address");
        emailField.getStyleClass().add("input");
        passwordField = new PasswordField();
        passwordField.setText("admin123");
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("input");
        visiblePasswordField = new TextField();
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.getStyleClass().add("input");
        StackPane passwordStack = new StackPane(passwordField, visiblePasswordField);
        CheckBox showPassword = new CheckBox("Show password");
        showPassword.selectedProperty().addListener((obs, oldValue, show) -> {
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            visiblePasswordField.setVisible(show);
            visiblePasswordField.setManaged(show);
        });
        CheckBox rememberMe = new CheckBox("Remember me");
        HBox options = new HBox(18, rememberMe, showPassword);
        options.setAlignment(Pos.CENTER_LEFT);
        Button loginButton = new Button("Sign in");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        Button createAccountButton = new Button("Create Account");
        createAccountButton.getStyleClass().add("link-button");
        createAccountButton.setMaxWidth(Double.MAX_VALUE);
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        form.getChildren().addAll(formTitle, emailField, passwordStack, options, loginButton, createAccountButton, statusLabel);

        loginButton.setOnAction(event -> authenticate(emailField.getText(), passwordField.getText(), loginButton));
        createAccountButton.setOnAction(event -> showCreateAccountDialog());
        passwordField.setOnAction(event -> authenticate(emailField.getText(), passwordField.getText(), loginButton));
        visiblePasswordField.setOnAction(event -> authenticate(emailField.getText(), visiblePasswordField.getText(), loginButton));

        shell.setLeft(hero);
        shell.setCenter(form);
        root.getChildren().setAll(shell);
        UiUtil.fadeIn(shell);
    }

    // Checks the login form credentials and opens the correct home screen.
    // Parameters: email is the user email address; password is the plain password entered by the user
    // Parameters: loginButton is the login button being updated during authentication.
    private void authenticate(String email, String password, Button loginButton) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            statusLabel.setText("Please enter both email and password.");
            return;
        }
        loginButton.setDisable(true);
        loginButton.setText("Checking access...");
        // Short pause gives the UI time to show feedback before changing screens.
        PauseTransition pause = new PauseTransition(Duration.millis(520));
        pause.setOnFinished(event -> {
            Optional<User> user = userService.authenticate(email, password);
            if (user.isPresent()) {
                sessionManager.login(user.get());
                notificationService.notify(user.get(), "Login successful", "Welcome back, " + user.get().getFullName(), "SUCCESS");
                MainController mainController = new MainController(stage, userService, libraryService, notificationService, reportService, sessionManager);
                Scene scene = stage.getScene();
                scene.setRoot(mainController.getView());
            } else {
                statusLabel.setText("Invalid credentials or inactive account.");
                loginButton.setDisable(false);
                loginButton.setText("Sign in");
            }
        });
        pause.play();
    }

    // Opens the dialog for creating a pending student account.
    private void showCreateAccountDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Create Account");
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("glass-dialog");
        dialog.getDialogPane().getStylesheets().addAll(stage.getScene().getStylesheets());

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");
        fullNameField.getStyleClass().add("input");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.getStyleClass().add("input");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("input");

        Label title = new Label("Create your member account");
        title.getStyleClass().add("modal-title");
        Label subtitle = new Label("Your request will be reviewed by a librarian before activation.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("modal-subtitle");
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("status-label");
        errorLabel.setWrapText(true);

        VBox content = new VBox(14, title, subtitle, fullNameField, emailField, passwordField, errorLabel);
        content.setPadding(new Insets(12));
        content.setPrefWidth(380);
        content.getStyleClass().add("signup-card");
        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Submit request");
        okButton.getStyleClass().add("primary-button");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                // The new account is saved as inactive until staff approves it.
                userService.createPendingMember(fullNameField.getText(), emailField.getText(), passwordField.getText());
                dialog.setResult(null);
                dialog.close();
                UiUtil.toast(root, "Account created. Pending librarian approval.", "toast-success");
            } catch (RuntimeException ex) {
                errorLabel.setText(ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }
}
