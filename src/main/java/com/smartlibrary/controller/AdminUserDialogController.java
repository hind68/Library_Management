package com.smartlibrary.controller;

import com.smartlibrary.model.Role;
import com.smartlibrary.model.User;
import com.smartlibrary.service.UserService;
import com.smartlibrary.util.UiUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class AdminUserDialogController {
    private final User user;
    private final UserService userService;
    private final StackPane toastHost;
    private final Runnable afterSave;

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private CheckBox activeCheck;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;

    // Creates an AdminUserDialogController object with the supplied values.
    // Parameters: user is the user account being checked or updated; userService is the helper that manages user accounts
    // Parameters: toastHost is the root pane used to show toast messages; afterSave is the action to run after saving.
    public AdminUserDialogController(User user, UserService userService, StackPane toastHost, Runnable afterSave) {
        this.user = user;
        this.userService = userService;
        this.toastHost = toastHost;
        this.afterSave = afterSave;
    }

    @FXML
    // Initializes the view controls after the FXML file is loaded.
    private void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(Role.values()));
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        roleCombo.getSelectionModel().select(user.getRole());
        activeCheck.setSelected(user.isActive());
        phoneField.setText(user.getPhone() == null ? "" : user.getPhone());
        addressField.setText(user.getAddress() == null ? "" : user.getAddress());
    }

    @FXML
    // Validates the form and saves the edited account information.
    private void saveChanges() {
        try {
            // Send all edited fields to the service so validation stays in one place.
            userService.updateAdminUser(
                    user,
                    fullNameField.getText(),
                    emailField.getText(),
                    roleCombo.getValue(),
                    activeCheck.isSelected(),
                    phoneField.getText(),
                    addressField.getText()
            );
            afterSave.run();
            UiUtil.toast(toastHost, "User updated successfully", "toast-success");
        } catch (RuntimeException ex) {
            UiUtil.showError("User update", ex.getMessage());
        }
    }
}
