package com.smartlibrary.controller;

import com.smartlibrary.model.Book;
import com.smartlibrary.model.User;
import com.smartlibrary.service.LibraryService;
import com.smartlibrary.util.UiUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;

public class StudentHomeController {
    private final LibraryService libraryService;
    private final User currentUser;
    private final StackPane toastHost;

    @FXML private ScrollPane scrollPane;
    @FXML private GridPane catalogGrid;

    // Creates a StudentHomeController object with the supplied values.
    // Parameters: libraryService is the helper that manages books and borrowings; currentUser is the user requesting the action
    // Parameters: toastHost is the root pane used to show toast messages.
    public StudentHomeController(LibraryService libraryService, User currentUser, StackPane toastHost) {
        this.libraryService = libraryService;
        this.currentUser = currentUser;
        this.toastHost = toastHost;
    }

    @FXML
    // Initializes the view controls after the FXML file is loaded.
    private void initialize() {
        scrollPane.setFitToWidth(true);
        renderCatalog();
    }

    // Refreshes the student catalog cards on screen.
    private void renderCatalog() {
        catalogGrid.getChildren().clear();
        catalogGrid.setHgap(16);
        catalogGrid.setVgap(16);
        catalogGrid.setPadding(new Insets(4, 0, 20, 0));

        int column = 0;
        int row = 0;
        for (Book book : libraryService.allBooks()) {
            catalogGrid.add(bookCard(book), column, row);
            column++;
            if (column == 3) {
                column = 0;
                row++;
            }
        }
    }

    // Creates one visual card for a book in the student catalog.
    // Parameters: book is the book being processed.
    private VBox bookCard(Book book) {
        VBox card = new VBox(10);
        card.getStyleClass().add("book-card");
        card.setPrefWidth(255);
        card.setMinHeight(390);

        ImageView cover = new ImageView(loadCoverImage(book));
        cover.setFitWidth(120);
        cover.setFitHeight(160);
        cover.setPreserveRatio(true);
        cover.getStyleClass().add("book-cover");

        StackPane coverFrame = new StackPane(cover);
        coverFrame.getStyleClass().add("book-cover-frame");
        coverFrame.setAlignment(Pos.CENTER);

        Label title = new Label(book.getTitle());
        title.getStyleClass().add("book-card-title");
        title.setWrapText(true);

        Label author = new Label(book.getAuthor());
        author.getStyleClass().add("book-card-author");
        author.setWrapText(true);

        Label status = new Label(book.getStatus());
        status.getStyleClass().addAll("badge-view", "book-card-badge", book.getAvailableCopies() > 0 ? "badge-success" : "badge-warning");
        status.setMaxWidth(Region.USE_COMPUTED_SIZE);
        status.setAlignment(Pos.CENTER);

        Label category = new Label(book.getCategory());
        category.getStyleClass().addAll("book-card-category", "book-card-badge");
        category.setMaxWidth(Region.USE_COMPUTED_SIZE);
        category.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button reserve = new Button("Reserve");
        reserve.getStyleClass().add("primary-button");
        reserve.setMaxWidth(Double.MAX_VALUE);
        reserve.setOnAction(event -> reserve(book));

        VBox badges = new VBox(8, status, category);
        badges.setFillWidth(false);
        badges.setAlignment(Pos.CENTER_LEFT);

        VBox cardFooter = new VBox(8, badges, reserve);
        cardFooter.getStyleClass().add("book-card-footer");

        card.getChildren().addAll(coverFrame, title, author, spacer, cardFooter);
        return card;
    }

    // Loads a cover image for a book, falling back when needed.
    // Parameters: book is the book being processed.
    private Image loadCoverImage(Book book) {
        String coverPath = book.getCoverImagePath();
        if (coverPath != null && !coverPath.isBlank()) {
            try {
                File file = new File(coverPath);
                if (file.isFile()) {
                    Image image = new Image(file.toURI().toString(), 120, 160, true, true);
                    return image.isError() ? placeholderCover() : image;
                }
                var resource = getClass().getResource(coverPath.startsWith("/") ? coverPath : "/" + coverPath);
                if (resource != null) {
                    Image image = new Image(resource.toExternalForm(), 120, 160, true, true);
                    return image.isError() ? placeholderCover() : image;
                }
            } catch (RuntimeException ignored) {
                // Fall through to the generated placeholder.
            }
        }
        return placeholderCover();
    }

    // Creates a simple placeholder cover image when no file is available.
    private Image placeholderCover() {
        int width = 120;
        int height = 160;
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double shade = 0.88 - (y / (double) height) * 0.16;
                writer.setColor(x, y, javafx.scene.paint.Color.color(0.84 * shade, 0.94 * shade, 0.92 * shade));
            }
        }
        return image;
    }

    // Creates a reservation for the selected book and current member.
    // Parameters: book is the book being processed.
    private void reserve(Book book) {
        try {
            libraryService.reserveBookForStudent(book, currentUser);
            UiUtil.toast(toastHost, "Book reserved successfully. Please pick it up at the desk.", "toast-success");
            renderCatalog();
        } catch (RuntimeException ex) {
            UiUtil.showError("Reservation error", ex.getMessage());
        }
    }
}
