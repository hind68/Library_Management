package com.smartlibrary.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Optional;

public final class UiUtil {
    // Creates a UiUtil object with the supplied values.
    private UiUtil() {}

    // Plays a short fade animation for a JavaFX node.
    // Parameters: node is the JavaFX node to show.
    public static void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    // Shows an information dialog to the user.
    // Parameters: title is the title text; message is the message shown or saved.
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    // Shows an error dialog to the user.
    // Parameters: title is the title text; message is the message shown or saved.
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    // Shows a confirmation dialog and returns the user choice.
    // Parameters: title is the title text; message is the message shown or saved.
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // Shows a temporary message over the current screen.
    // Parameters: host is the root pane that displays the toast; message is the message shown or saved
    // Parameters: styleClass is the CSS class used to style the toast.
    public static void toast(StackPane host, String message, String styleClass) {
        Label toast = new Label(message);
        toast.getStyleClass().addAll("toast", styleClass);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        host.getChildren().add(toast);
        fadeIn(toast);
        PauseTransition wait = new PauseTransition(Duration.seconds(2.4));
        wait.setOnFinished(event -> host.getChildren().remove(toast));
        wait.play();
    }
}
