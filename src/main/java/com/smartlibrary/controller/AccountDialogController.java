package com.smartlibrary.controller;

import com.smartlibrary.model.User;
import com.smartlibrary.model.Role;
import com.smartlibrary.service.UserService;
import com.smartlibrary.util.UiUtil;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class AccountDialogController {
    private final User currentUser;
    private final UserService userService;
    private final StackPane toastHost;
    private final Runnable closeDialog;

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;

    // Creates an AccountDialogController object with the supplied values.
    // Parameters: currentUser is the user requesting the action; userService is the helper that manages user accounts
    // Parameters: toastHost is the root pane used to show toast messages; closeDialog is the action that closes the dialog.
    public AccountDialogController(User currentUser, UserService userService, StackPane toastHost, Runnable closeDialog) {
        this.currentUser = currentUser;
        this.userService = userService;
        this.toastHost = toastHost;
        this.closeDialog = closeDialog;
    }

    @FXML
    // Initializes the view controls after the FXML file is loaded.
    private void initialize() {
        fullNameField.setText(currentUser.getFullName());
        emailField.setText(currentUser.getEmail());
        boolean admin = currentUser.getRole() == Role.ADMIN;
        fullNameField.setDisable(!admin);
        emailField.setDisable(!admin);
        phoneField.setText(currentUser.getPhone() == null ? "" : currentUser.getPhone());
        addressField.setText(currentUser.getAddress() == null ? "" : currentUser.getAddress());
    }

    @FXML
    // Validates the form and saves the edited account information.
    private void saveChanges() {
        String previousPhone = currentUser.getPhone();
        String previousAddress = currentUser.getAddress();
        String previousName = currentUser.getFullName();
        String previousEmail = currentUser.getEmail();
        // Members can edit contact details, while admins can also edit name and email.
        if (currentUser.getRole() == Role.ADMIN) {
            currentUser.setFullName(fullNameField.getText() == null ? "" : fullNameField.getText().trim());
            currentUser.setEmail(emailField.getText() == null ? "" : emailField.getText().trim());
        }
        currentUser.setPhone(phoneField.getText() == null ? "" : phoneField.getText().trim());
        currentUser.setAddress(addressField.getText() == null ? "" : addressField.getText().trim());
        try {
            userService.updateUserProfile(currentUser);
            closeDialog.run();
            UiUtil.toast(toastHost, "Profile updated successfully", "toast-success");
        } catch (RuntimeException ex) {
            // Restore the original values if saving fails.
            currentUser.setFullName(previousName);
            currentUser.setEmail(previousEmail);
            currentUser.setPhone(previousPhone);
            currentUser.setAddress(previousAddress);
            UiUtil.showError("Profile update", ex.getMessage());
        }
    }
}
