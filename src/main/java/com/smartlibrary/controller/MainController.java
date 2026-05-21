package com.smartlibrary.controller;

import com.smartlibrary.model.*;
import com.smartlibrary.service.LibraryService;
import com.smartlibrary.service.NotificationService;
import com.smartlibrary.service.ReportService;
import com.smartlibrary.service.UserService;
import com.smartlibrary.util.SessionManager;
import com.smartlibrary.util.UiUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class MainController {
    private final Stage stage;
    private final UserService userService;
    private final LibraryService libraryService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final SessionManager sessionManager;
    private final StackPane root = new StackPane();
    private final BorderPane shell = new BorderPane();
    private final VBox sidebar = new VBox(10);
    private final StackPane content = new StackPane();
    private final User currentUser;

    // Creates a MainController object with the supplied values.
    // Parameters: stage is the main JavaFX window; userService is the helper that manages user accounts
    // Parameters: libraryService is the helper that manages books and borrowings; notificationService is the helper that manages notifications
    // Parameters: reportService is the helper that creates reports; sessionManager is the helper that stores the current login.
    public MainController(Stage stage, UserService userService, LibraryService libraryService,
                          NotificationService notificationService, ReportService reportService,
                          SessionManager sessionManager) {
        this.stage = stage;
        this.userService = userService;
        this.libraryService = libraryService;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.sessionManager = sessionManager;
        this.currentUser = sessionManager.currentUser().orElseThrow();
        build();
        if (currentUser.getRole() == Role.MEMBER) {
            showStudentHome();
        } else {
            showDashboard();
        }
    }

    // Returns the root JavaFX view for this controller.
    public Parent getView() {
        return root;
    }

    // Builds the main layout and connects the screen navigation.
    private void build() {
        shell.getStyleClass().add("app-shell");
        sidebar.setPadding(new Insets(24));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(245);
        Label logo = new Label("Smart Library");
        logo.getStyleClass().add("logo");
        Label role = new Label(currentUser.getRole().displayName());
        role.getStyleClass().add("role-chip");
        sidebar.getChildren().addAll(logo, role);

        if (currentUser.getRole() == Role.MEMBER) {
            sidebar.getChildren().add(nav("Home", this::showStudentHome));
        } else {
            sidebar.getChildren().addAll(nav("Dashboard", this::showDashboard), nav("Books", this::showBooks));
        }

        if (currentUser.getRole() == Role.ADMIN) {
            sidebar.getChildren().addAll(nav("Users & Staff", this::showUsers), nav("Reports", this::showReports), nav("System Logs", this::showLogs));
        }
        if (currentUser.getRole() == Role.LIBRARIAN || currentUser.getRole() == Role.ADMIN) {
            sidebar.getChildren().addAll(nav("Borrow / Return", this::showBorrowing), nav("Reservations", this::showReservations), nav("Members", this::showMembers), nav("Notifications", this::showNotifications));
        }
        if (currentUser.getRole() == Role.MEMBER) {
            sidebar.getChildren().addAll(nav("My Borrowings", this::showMyLibrary), nav("Reservations", this::showMyReservations), nav("Notifications", this::showNotifications));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button account = new Button("My Account");
        account.getStyleClass().add("sidebar-button");
        account.setMaxWidth(Double.MAX_VALUE);
        account.setOnAction(event -> showMyAccountDialog());
        Button logout = new Button("Logout");
        logout.getStyleClass().add("danger-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(event -> logout());
        sidebar.getChildren().addAll(spacer, account, logout);

        shell.setLeft(sidebar);
        shell.setCenter(content);
        root.getChildren().setAll(shell);
    }

    // Creates one sidebar navigation button.
    // Parameters: text is the button or label text; action is the code to run when selected.
    private Button nav(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("sidebar-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setTooltip(new Tooltip("Open " + text + " screen"));
        button.setOnAction(event -> action.run());
        return button;
    }

    // Opens the dialog where the current user can edit account details.
    private void showMyAccountDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("My Account");
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("glass-dialog");
        dialog.getDialogPane().getStylesheets().addAll(stage.getScene().getStylesheets());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account-dialog.fxml"));
            AccountDialogController controller = new AccountDialogController(currentUser, userService, root, dialog::close);
            loader.setController(controller);
            dialog.getDialogPane().setContent(loader.load());
            dialog.showAndWait();
        } catch (IOException ex) {
            UiUtil.showError("My Account", "Unable to load the account form: " + ex.getMessage());
        }
    }

    // Loads the student home screen for member accounts.
    private void showStudentHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-home.fxml"));
            StudentHomeController controller = new StudentHomeController(libraryService, currentUser, root);
            loader.setController(controller);
            setContent(loader.load());
        } catch (IOException ex) {
            UiUtil.showError("Student home", "Unable to load the student home view: " + ex.getMessage());
        }
    }

    // Creates a standard page container with a title and subtitle.
    // Parameters: title is the title text; subtitle is the subtitle text.
    private VBox page(String title, String subtitle) {
        VBox page = new VBox(18);
        page.setPadding(new Insets(26));
        page.getStyleClass().add("page");
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("page-subtitle");
        page.getChildren().addAll(heading, sub);
        return page;
    }

    // Replaces the center content area with a new screen.
    // Parameters: node is the JavaFX node to show.
    private void setContent(Parent node) {
        content.getChildren().setAll(node);
        UiUtil.fadeIn(node);
    }

    // Displays the dashboard summary and charts.
    private void showDashboard() {
        VBox page = page(currentUser.getRole().displayName() + " Dashboard", "Real-time overview tailored to your permissions and daily workflow.");
        DashboardStats stats = libraryService.dashboardStats((int) userService.countActiveUsers(), notificationService.unreadFor(currentUser));
        GridPane cards = new GridPane();
        cards.setHgap(14);
        cards.setVgap(14);
        VBox totalBooksCard = dashboardCard("totalBooksCard", "Total books", String.valueOf(stats.totalBooks()), "Catalog inventory", this::showBooks);
        VBox borrowedCard = dashboardCard("borrowedCard", "Borrowed", String.valueOf(stats.borrowedBooks()), "Currently issued", this::showBorrowing);
        VBox overdueCard = dashboardCard("overdueCard", "Overdue", String.valueOf(stats.overdueBooks()), "Needs attention", this::showBorrowing);
        VBox activeUsersCard = dashboardCard("activeUsersCard", "Active users", String.valueOf(stats.activeUsers()), "Enabled accounts", this::showUsers);
        VBox todayCard = dashboardCard("todayCard", "Today", String.valueOf(stats.todayBorrowings()), "New borrowings", this::showBorrowing);
        VBox pendingReturnsCard = dashboardCard("pendingReturnsCard", "Pending returns", String.valueOf(stats.pendingReturns()), "Open loans", this::showBorrowing);
        VBox notificationsCard = dashboardCard("notificationsCard", "Notifications", String.valueOf(stats.unreadNotifications()), "Unread alerts", this::showNotifications);
        VBox reservationsCard = dashboardCard("reservationsCard", "Reservations", String.valueOf(stats.reservations()), "Queue items", this::showReservations);
        cards.add(totalBooksCard, 0, 0);
        cards.add(borrowedCard, 1, 0);
        cards.add(overdueCard, 2, 0);
        cards.add(activeUsersCard, 3, 0);
        cards.add(todayCard, 0, 1);
        cards.add(pendingReturnsCard, 1, 1);
        cards.add(notificationsCard, 2, 1);
        cards.add(reservationsCard, 3, 1);

        HBox analytics = new HBox(16, categoryPie(), borrowedChart(), activityTimeline());
        analytics.setAlignment(Pos.TOP_LEFT);
        page.getChildren().addAll(cards, analytics, quickActions());
        setContent(new ScrollPane(page));
    }

    // Creates a small statistic card for the dashboard.
    // Parameters: title is the title text; value is the value to format or store; subtitle is the subtitle text.
    private VBox card(String title, String value, String subtitle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setMinSize(185, 116);
        Label t = new Label(title);
        t.getStyleClass().add("stat-title");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label s = new Label(subtitle);
        s.getStyleClass().add("stat-subtitle");
        card.getChildren().addAll(t, v, s);
        return card;
    }

    // Creates a clickable dashboard card that opens a related screen.
    // Parameters: id is the database id; title is the title text; value is the value to format or store
    // Parameters: subtitle is the subtitle text; action is the code to run when selected.
    private VBox dashboardCard(String id, String title, String value, String subtitle, Runnable action) {
        VBox card = card(title, value, subtitle);
        card.setId(id);
        card.getStyleClass().add("clickable-card");
        card.setOnMouseClicked(event -> action.run());
        return card;
    }

    // Builds the pie chart that shows books grouped by category.
    private PieChart categoryPie() {
        PieChart chart = new PieChart();
        chart.setTitle("Catalog by category");

        // Keep the legend on the right so the chart labels have more room.
        chart.setLegendVisible(true);
        chart.setLegendSide(javafx.geometry.Side.RIGHT);
        chart.setLabelsVisible(true);

        // Give JavaFX enough chart space to display the labels clearly.
        chart.setPrefSize(400, 280);

        chart.getData().clear();
        var data = FXCollections.<PieChart.Data>observableArrayList();
        for (Map.Entry<String, Long> item : libraryService.getBooksCountByCategory().entrySet()) {
            if (item.getValue() > 0) {
                data.add(new PieChart.Data(item.getKey(), item.getValue()));
            }
        }
        chart.setData(data);
        chart.getData().forEach(item ->
            item.setName(item.getName() + " (" + (int) item.getPieValue() + ")")
        );
        chart.getStyleClass().add("panel");
        return chart;
    }

    // Builds the bar chart for borrowing, return, late, and reservation activity.
    private BarChart<String, Number> borrowedChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        long borrowCount = libraryService.allBorrowings().size();
        long returnCount = libraryService.allBorrowings().stream().filter(b -> b.getReturnDate() != null).count();
        long lateCount = libraryService.allBorrowings().stream().filter(b -> b.getDueDate().isBefore(java.time.LocalDate.now()) && b.getReturnDate() == null).count();
        long reserveCount = libraryService.allReservations().size();
        double maxValue = Math.max(Math.max(borrowCount, returnCount), Math.max(lateCount, reserveCount));
        if (maxValue == 0) {
            maxValue = 5;
        }
        y.setAutoRanging(false);
        y.setLowerBound(0);
        y.setUpperBound(maxValue + 1);
        y.setMinorTickVisible(false);
        y.setTickUnit(1.0);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle("Weekly activity");
        chart.setLegendVisible(false);
        chart.setPrefSize(350, 250);
        BarChart.Series<String, Number> series = new BarChart.Series<>();
        series.getData().add(new BarChart.Data<>("Borrow", borrowCount));
        series.getData().add(new BarChart.Data<>("Return", returnCount));
        series.getData().add(new BarChart.Data<>("Late", lateCount));
        series.getData().add(new BarChart.Data<>("Reserve", reserveCount));
        chart.getData().add(series);
        chart.getStyleClass().add("panel");
        return chart;
    }

    // Builds the recent activity panel shown on the dashboard.
    private VBox activityTimeline() {
        VBox box = new VBox(10);
        box.getStyleClass().add("panel-box");
        box.setId("recentActivityPanel");
        box.getStyleClass().add("clickable-card");
        box.setOnMouseClicked(event -> showLogs());
        box.setPrefSize(330, 250);
        Label title = new Label("Recent activity");
        title.getStyleClass().add("panel-title");
        box.getChildren().add(title);
        libraryService.logs().stream().limit(5).forEach(log -> {
            Label item = new Label(log.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) + "  " + log.getAction() + " - " + log.getDetails());
            item.setWrapText(true);
            item.getStyleClass().add("timeline-item");
            box.getChildren().add(item);
        });
        return box;
    }

    // Creates the row of shortcut buttons used on the dashboard.
    private HBox quickActions() {
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button books = actionButton("Manage books", this::showBooks);
        Button notifications = actionButton("Send notification", this::showNotifications);
        Button reports = actionButton("Generate report", this::showReports);
        actions.getChildren().addAll(books, notifications, reports);
        return actions;
    }

    // Creates a reusable styled action button.
    // Parameters: label is the button text; action is the code to run when selected.
    private Button actionButton(String label, Runnable action) {
        Button button = new Button(label);
        button.getStyleClass().add("secondary-button");
        button.setTooltip(new Tooltip(label));
        button.setOnAction(event -> action.run());
        return button;
    }

    // Displays the book catalog with search, filters, and row actions.
    private void showBooks() {
        VBox page = page("Book Catalog", "Advanced search, filtering, QR preview, covers, CSV export, and availability management.");
        TextField search = new TextField();
        search.setPromptText("Search by title, author, or ISBN");
        search.setTooltip(new Tooltip("Type a title, author name, or ISBN to filter the catalog."));
        ComboBox<String> category = new ComboBox<>(FXCollections.observableArrayList(libraryService.categories()));
        category.setTooltip(new Tooltip("Filter books by category."));
        category.getSelectionModel().select("All");
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("All", "Available", "Unavailable"));
        status.setTooltip(new Tooltip("Filter books by availability status."));
        status.getSelectionModel().select("All");
        Button add = actionButton("Add book", () -> bookDialog(null));
        Button export = actionButton("Export CSV", this::exportCsv);
        HBox filters = new HBox(10, search, category, status, add, export);
        filters.setAlignment(Pos.CENTER_LEFT);

        TableView<Book> table = bookTable();
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(libraryService.searchBooks(search.getText(), category.getValue(), status.getValue())));
        search.textProperty().addListener((obs, old, value) -> refresh.run());
        category.setOnAction(event -> refresh.run());
        status.setOnAction(event -> refresh.run());
        refresh.run();

        table.setRowFactory(tv -> {
            TableRow<Book> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem edit = new MenuItem("Edit book");
            edit.setOnAction(event -> {
                if (!row.isEmpty()) bookDialog(row.getItem());
            });
            MenuItem delete = new MenuItem("Delete book");
            delete.setOnAction(event -> {
                if (!row.isEmpty() && UiUtil.confirm("Delete book", "Delete selected book from the catalog?")) {
                    try {
                        libraryService.deleteBook(row.getItem(), currentUser.getFullName());
                        UiUtil.toast(root, "Book deleted.", "toast-success");
                        showBooks();
                    } catch (RuntimeException ex) {
                        UiUtil.showError("Delete error", ex.getMessage());
                    }
                }
            });
            MenuItem reserve = new MenuItem("Reserve for me");
            reserve.setOnAction(event -> {
                if (!row.isEmpty()) {
                    try {
                        libraryService.reserveBook(row.getItem(), currentUser, currentUser.getFullName());
                        UiUtil.toast(root, "Reservation added.", "toast-success");
                    } catch (RuntimeException ex) {
                        UiUtil.showError("Reservation error", ex.getMessage());
                    }
                }
            });
            if (sessionManager.canManageBooks()) {
                menu.getItems().addAll(edit, delete);
            }
            if (currentUser.getRole() == Role.MEMBER) {
                menu.getItems().add(reserve);
            }
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && sessionManager.canManageBooks()) {
                    bookDialog(row.getItem());
                }
            });
            return row;
        });
        page.getChildren().addAll(filters, table);
        setContent(page);
    }

    // Creates the table used to display book records.
    private TableView<Book> bookTable() {
        TableView<Book> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Title", "title"));
        table.getColumns().add(column("Author", "author"));
        table.getColumns().add(column("ISBN", "isbn"));
        table.getColumns().add(column("Category", "category"));
        table.getColumns().add(column("Year", "publicationYear"));
        table.getColumns().add(column("Qty", "quantity"));
        table.getColumns().add(column("Available", "availableCopies"));
        table.getColumns().add(column("Shelf", "shelfLocation"));
        table.getColumns().add(column("Status", "status"));
        table.setPlaceholder(new Label("No books match the current filters."));
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    // Creates a table column bound to a model property.
    // Parameters: title is the title text; property is the model property name used by the table column.
    private <T> TableColumn<T, Object> column(String title, String property) {
        TableColumn<T, Object> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    // Opens the add or edit dialog for a book record.
    // Parameters: existing is the book being edited, or null when adding a new one.
    private void bookDialog(Book existing) {
        if (!sessionManager.canManageBooks()) {
            UiUtil.showError("Access denied", "Your role cannot modify catalog records.");
            return;
        }
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add book" : "Edit book");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        TextField title = new TextField(existing == null ? "" : existing.getTitle());
        TextField author = new TextField(existing == null ? "" : existing.getAuthor());
        TextField isbn = new TextField(existing == null ? "" : existing.getIsbn());
        TextField category = new TextField(existing == null ? "" : existing.getCategory());
        Spinner<Integer> year = new Spinner<>(1400, java.time.LocalDate.now().getYear() + 1, existing == null ? 2024 : existing.getPublicationYear());
        Spinner<Integer> quantity = new Spinner<>(1, 200, existing == null ? 1 : existing.getQuantity());
        TextField shelf = new TextField(existing == null ? "" : existing.getShelfLocation());
        TextField cover = new TextField(existing == null ? "" : existing.getCoverImagePath());
        Button upload = new Button("Choose cover");
        upload.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select book cover");
            File file = chooser.showOpenDialog(stage);
            if (file != null) cover.setText(file.getAbsolutePath());
        });
        GridPane grid = formGrid();
        grid.addRow(0, new Label("Title"), title);
        grid.addRow(1, new Label("Author"), author);
        grid.addRow(2, new Label("ISBN"), isbn);
        grid.addRow(3, new Label("Category"), category);
        grid.addRow(4, new Label("Publication year"), year);
        grid.addRow(5, new Label("Quantity"), quantity);
        grid.addRow(6, new Label("Shelf"), shelf);
        grid.addRow(7, new Label("Cover"), new HBox(8, cover, upload));
        grid.add(qrPreview(existing == null ? "NEW-BOOK" : existing.getIsbn()), 1, 8);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (existing == null) {
                    return libraryService.addBook(title.getText(), author.getText(), isbn.getText(), category.getText(), year.getValue(), quantity.getValue(), shelf.getText(), cover.getText(), currentUser.getFullName());
                }
                existing.setTitle(title.getText());
                existing.setAuthor(author.getText());
                existing.setIsbn(isbn.getText());
                existing.setCategory(category.getText());
                existing.setPublicationYear(year.getValue());
                existing.setQuantity(quantity.getValue());
                existing.setShelfLocation(shelf.getText());
                existing.setCoverImagePath(cover.getText());
                libraryService.updateBook(existing, currentUser.getFullName());
                return existing;
            }
            return null;
        });
        try {
            dialog.showAndWait();
            showBooks();
            UiUtil.toast(root, "Book catalog updated.", "toast-success");
        } catch (RuntimeException ex) {
            UiUtil.showError("Book validation", ex.getMessage());
        }
    }

    // Creates a reusable grid layout for form fields.
    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        return grid;
    }

    // Builds a simple visual QR-style preview from a text seed.
    // Parameters: seed is the text used to draw the preview.
    private GridPane qrPreview(String seed) {
        GridPane qr = new GridPane();
        qr.getStyleClass().add("qr");
        int hash = Math.abs(seed.hashCode());
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                Rectangle r = new Rectangle(9, 9);
                boolean dark = ((hash >> ((x + y) % 16)) & 1) == 1 || (x < 2 && y < 2) || (x > 6 && y < 2) || (x < 2 && y > 6);
                r.setFill(dark ? Color.web("#111827") : Color.WHITE);
                qr.add(r, x, y);
            }
        }
        return qr;
    }

    // Displays borrowing, returning, and reservation approval workflows.
    private void showBorrowing() {
        VBox page = page("Borrowing & Returns", "Issue books, handle returns, detect overdue items, generate fines, and manage reservation queues.");
        ComboBox<Book> books = new ComboBox<>(FXCollections.observableArrayList(libraryService.allBooks()));
        books.setTooltip(new Tooltip("Select the book to issue or reserve."));
        books.setCellFactory(list -> bookCell());
        books.setButtonCell(bookCell());
        ComboBox<User> members = new ComboBox<>(FXCollections.observableArrayList(userService.members()));
        members.setTooltip(new Tooltip("Select the student/member who receives the book."));
        members.setCellFactory(list -> userCell());
        members.setButtonCell(userCell());
        Button issue = actionButton("Issue book", () -> {
            try {
                libraryService.borrowBook(books.getValue(), members.getValue(), currentUser.getFullName());
                notificationService.notify(members.getValue(), "Book issued", books.getValue().getTitle() + " is due in 14 days.", "SUCCESS");
                UiUtil.toast(root, "Borrowing created.", "toast-success");
                showBorrowing();
            } catch (RuntimeException ex) {
                UiUtil.showError("Borrowing error", ex.getMessage());
            }
        });
        Button reserve = actionButton("Reserve unavailable", () -> {
            try {
                libraryService.reserveBook(books.getValue(), members.getValue(), currentUser.getFullName());
                UiUtil.toast(root, "Reservation added.", "toast-success");
                showBorrowing();
            } catch (RuntimeException ex) {
                UiUtil.showError("Reservation error", ex.getMessage());
            }
        });
        HBox actions = new HBox(10, books, members, issue, reserve);
        TableView<Borrowing> table = borrowingTable(libraryService.allBorrowings());
        TableView<Reservation> waitingReservations = reservationsTable(libraryService.waitingReservations());
        Button approveReservation = actionButton("Approve & Issue Book", () -> {
            Reservation selectedReservation = waitingReservations.getSelectionModel().getSelectedItem();
            try {
                libraryService.approveReservationAndIssueBook(selectedReservation, currentUser);
                UiUtil.toast(root, "Reservation approved and book issued.", "toast-success");
                showBorrowing();
            } catch (RuntimeException ex) {
                UiUtil.showError("Reservation approval", ex.getMessage());
            }
        });
        Button returns = actionButton("Return selected", () -> {
            Borrowing borrowing = table.getSelectionModel().getSelectedItem();
            try {
                libraryService.returnBook(borrowing, currentUser.getFullName());
                UiUtil.toast(root, "Return completed.", "toast-success");
                showBorrowing();
            } catch (RuntimeException ex) {
                UiUtil.showError("Return error", ex.getMessage());
            }
        });
        Label waitingTitle = new Label("Waiting reservations");
        waitingTitle.getStyleClass().add("panel-title");
        Label borrowingsTitle = new Label("Active and historical borrowings");
        borrowingsTitle.getStyleClass().add("panel-title");
        page.getChildren().addAll(actions, waitingTitle, approveReservation, waitingReservations, borrowingsTitle, returns, table);
        setContent(page);
    }

    // Creates the display cell used for book selection lists.
    private ListCell<Book> bookCell() {
        return new ListCell<>() {
            // Updates how one book option is displayed in the list.
            // Parameters: item is the row value being displayed; empty is whether the cell has no row value.
            @Override protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitle() + " (" + item.getAvailableCopies() + " available)");
            }
        };
    }

    // Creates the display cell used for user selection lists.
    private ListCell<User> userCell() {
        return new ListCell<>() {
            // Updates how one user option is displayed in the list.
            // Parameters: item is the row value being displayed; empty is whether the cell has no row value.
            @Override protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getFullName());
            }
        };
    }

    // Creates a table for borrowing records.
    // Parameters: data is the records displayed in the table.
    private TableView<Borrowing> borrowingTable(List<Borrowing> data) {
        TableView<Borrowing> table = new TableView<>(FXCollections.observableArrayList(data));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Book", "bookTitle"));
        table.getColumns().add(column("Member", "memberName"));
        table.getColumns().add(column("Borrowed", "borrowDate"));
        table.getColumns().add(column("Due", "dueDate"));
        table.getColumns().add(column("Returned", "returnDate"));
        table.getColumns().add(column("Status", "status"));
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    // Displays the admin user and staff management screen.
    private void showUsers() {
        VBox page = page("User & Staff Management", "Admin-only area for librarians, members, activation status, and secure role assignment.");
        TableView<User> table = new TableView<>(FXCollections.observableArrayList(userService.findAll()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Name", "fullName"));
        table.getColumns().add(column("Email", "email"));
        table.getColumns().add(column("Role", "role"));
        table.getColumns().add(column("Active", "active"));
        table.setPlaceholder(new Label("No users found."));
        Button add = actionButton("Add user", this::userDialog);
        Button edit = actionButton("Edit User", () -> editUserDialog(table.getSelectionModel().getSelectedItem()));
        Button toggle = actionButton("Activate / suspend", () -> {
            User user = table.getSelectionModel().getSelectedItem();
            if (user != null) {
                userService.toggleActive(user);
                UiUtil.toast(root, "User status updated.", "toast-success");
                showUsers();
            }
        });
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("All users", table),
                new Tab("Members only", userTable(userService.members()))
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        page.getChildren().addAll(new HBox(10, add, edit, toggle), tabs);
        setContent(page);
    }

    // Creates a table for user records.
    // Parameters: users is the users displayed in the table.
    private TableView<User> userTable(List<User> users) {
        TableView<User> table = new TableView<>(FXCollections.observableArrayList(users));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Name", "fullName"));
        table.getColumns().add(column("Email", "email"));
        table.getColumns().add(column("Role", "role"));
        table.getColumns().add(column("Active", "active"));
        return table;
    }

    // Opens the dialog for creating a new user account.
    private void userDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Create account");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        TextField name = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        role.getSelectionModel().select(Role.MEMBER);
        GridPane grid = formGrid();
        grid.addRow(0, new Label("Full name"), name);
        grid.addRow(1, new Label("Email"), email);
        grid.addRow(2, new Label("Password"), password);
        grid.addRow(3, new Label("Role"), role);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? userService.createUser(name.getText(), email.getText(), password.getText(), role.getValue()) : null);
        try {
            dialog.showAndWait();
            showUsers();
        } catch (RuntimeException ex) {
            UiUtil.showError("User validation", ex.getMessage());
        }
    }

    // Opens the dialog for editing an existing user account.
    // Parameters: selectedUser is the user selected for editing.
    private void editUserDialog(User selectedUser) {
        if (selectedUser == null) {
            UiUtil.showError("Edit User", "Please select a user to edit.");
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("glass-dialog");
        dialog.getDialogPane().getStylesheets().addAll(stage.getScene().getStylesheets());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin-user-dialog.fxml"));
            AdminUserDialogController controller = new AdminUserDialogController(selectedUser, userService, root, () -> {
                dialog.close();
                showUsers();
            });
            loader.setController(controller);
            dialog.getDialogPane().setContent(loader.load());
            dialog.showAndWait();
        } catch (IOException ex) {
            UiUtil.showError("Edit User", "Unable to load the user editor: " + ex.getMessage());
        }
    }

    // Displays the member registry screen.
    private void showMembers() {
        VBox page = page("Member Registry", "Librarian workflow for registering, reviewing, and supporting active student members.");
        TableView<User> table = new TableView<>(FXCollections.observableArrayList(userService.members()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Member", "fullName"));
        table.getColumns().add(column("Email", "email"));
        table.getColumns().add(column("Active", "active"));
        page.getChildren().addAll(actionButton("Register member", this::memberDialog), table);
        setContent(page);
    }

    // Opens the dialog for registering a new member account.
    private void memberDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Register member");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        TextField name = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        password.setText("student123");
        GridPane grid = formGrid();
        grid.addRow(0, new Label("Full name"), name);
        grid.addRow(1, new Label("Email"), email);
        grid.addRow(2, new Label("Temporary password"), password);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? userService.createUser(name.getText(), email.getText(), password.getText(), Role.MEMBER) : null);
        try {
            dialog.showAndWait();
            showMembers();
        } catch (RuntimeException ex) {
            UiUtil.showError("Member validation", ex.getMessage());
        }
    }

    // Displays the current member borrowing history and fines.
    private void showMyLibrary() {
        VBox page = page("My Library", "Personal borrowing history, due dates, overdue status, and fine visibility.");
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Current borrowings", borrowingTable(libraryService.currentBorrowingsFor(currentUser))),
                new Tab("Borrow history", borrowingTable(libraryService.historyFor(currentUser))),
                new Tab("Penalties", finesTable(libraryService.finesFor(currentUser)))
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        page.getChildren().add(tabs);
        setContent(page);
    }

    // Displays reservations owned by the current member.
    private void showMyReservations() {
        VBox page = page("My Reservations", "Your reservation queue and availability alerts.");
        TableView<Reservation> table = reservationsTable(libraryService.reservationsFor(currentUser));
        Button cancel = actionButton("Cancel Reservation", () -> {
            Reservation reservation = table.getSelectionModel().getSelectedItem();
            if (reservation == null) {
                UiUtil.showError("Reservation", "Please select a reservation to cancel.");
                return;
            }
            try {
                libraryService.cancelReservation(reservation, currentUser);
                table.setItems(FXCollections.observableArrayList(libraryService.reservationsFor(currentUser)));
                UiUtil.toast(root, "Reservation cancelled.", "toast-success");
            } catch (RuntimeException ex) {
                UiUtil.showError("Reservation cancellation", ex.getMessage());
            }
        });
        cancel.setVisible(currentUser.getRole() == Role.MEMBER);
        cancel.setManaged(currentUser.getRole() == Role.MEMBER);
        page.getChildren().addAll(cancel, table);
        setContent(page);
    }

    // Displays reservations for the current role.
    private void showReservations() {
        if (currentUser.getRole() == Role.MEMBER) {
            showMyReservations();
            return;
        }
        VBox page = page("Reservations", "Reservation queue, pickup status, and completed reservation history.");
        TableView<Reservation> table = reservationsTable(libraryService.allReservations());
        page.getChildren().add(table);
        setContent(page);
    }

    // Creates a table for reservation records.
    // Parameters: reservations is the reservations displayed in the table.
    private TableView<Reservation> reservationsTable(List<Reservation> reservations) {
        TableView<Reservation> table = new TableView<>(FXCollections.observableArrayList(reservations));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Book", "bookTitle"));
        TableColumn<Reservation, Object> memberColumn = column("Member", "memberName");
        if (currentUser.getRole() == Role.MEMBER) {
            memberColumn.setVisible(false);
        }
        table.getColumns().add(memberColumn);
        table.getColumns().add(column("Date", "reservationDate"));
        table.getColumns().add(column("Status", "status"));
        if (currentUser.getRole() == Role.MEMBER) {
            table.setRowFactory(view -> {
                TableRow<Reservation> row = new TableRow<>();
                MenuItem cancel = new MenuItem("Cancel Reservation");
                cancel.setOnAction(event -> {
                    Reservation reservation = row.getItem();
                    if (reservation != null) {
                        try {
                            libraryService.cancelReservation(reservation, currentUser);
                            table.setItems(FXCollections.observableArrayList(libraryService.reservationsFor(currentUser)));
                            UiUtil.toast(root, "Reservation cancelled.", "toast-success");
                        } catch (RuntimeException ex) {
                            UiUtil.showError("Reservation cancellation", ex.getMessage());
                        }
                    }
                });
                ContextMenu menu = new ContextMenu(cancel);
                row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
                return row;
            });
        }
        table.setPlaceholder(new Label("No reservations available."));
        return table;
    }

    // Creates a table for fine records.
    // Parameters: fines is the fines displayed in the table.
    private TableView<Fine> finesTable(List<Fine> fines) {
        TableView<Fine> table = new TableView<>(FXCollections.observableArrayList(fines));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Amount", "amount"));
        table.getColumns().add(column("Reason", "reason"));
        table.getColumns().add(column("Status", "status"));
        table.getColumns().add(column("Date", "createdDate"));
        table.setPlaceholder(new Label("No fines recorded."));
        return table;
    }

    // Displays notifications and notification actions.
    private void showNotifications() {
        VBox page = page("Notifications", "Popup alerts, email-simulation notices, overdue warnings, and reservation availability messages.");
        Button broadcast = actionButton("Broadcast overdue reminder", () -> {
            notificationService.broadcast("Overdue reminder", "Please review due dates and return borrowed books on time.", "WARNING");
            UiUtil.toast(root, "Notification sent.", "toast-success");
            showNotifications();
        });
        boolean member = currentUser.getRole() == Role.MEMBER;
        broadcast.setVisible(!member);
        broadcast.setManaged(!member);
        VBox list = new VBox(10);
        Button clearAll = actionButton("Clear All Notifications", () -> {
            try {
                User sessionUser = sessionManager.currentUser().orElse(currentUser);
                notificationService.clearAllForUser(sessionUser);
                UiUtil.toast(root, "Notification(s) deleted", "toast-success");
                showNotifications();
            } catch (RuntimeException ex) {
                UiUtil.showError("Notifications", ex.getMessage());
            }
        });
        for (LibraryNotification notification : notificationService.forUser(currentUser)) {
            VBox item = new VBox(6);
            item.getStyleClass().add("notification-item");
            Label title = new Label(notification.getTitle());
            title.getStyleClass().addAll("panel-title", "notification-title");
            Label date = new Label(formatDate(notification.getCreatedAt()));
            date.getStyleClass().add("metadata-label");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button delete = new Button("X");
            delete.getStyleClass().add("icon-button");
            delete.setTooltip(new Tooltip("Delete notification"));
            delete.setOnAction(event -> {
                try {
                    notificationService.deleteNotification(notification, currentUser);
                    UiUtil.toast(root, "Notification(s) deleted", "toast-success");
                    showNotifications();
                } catch (RuntimeException ex) {
                    UiUtil.showError("Notifications", ex.getMessage());
                }
            });
            HBox header = new HBox(10, title, spacer, date, delete);
            header.setAlignment(Pos.CENTER_LEFT);
            Label message = new Label(notification.getMessage());
            message.getStyleClass().add("notification-message");
            message.setWrapText(true);
            item.getChildren().addAll(header, message);
            list.getChildren().add(item);
        }
        ScrollPane scroll = transparentScroll(list);
        HBox notificationActions = new HBox(10, broadcast, clearAll);
        notificationActions.setAlignment(Pos.CENTER_LEFT);
        page.getChildren().addAll(notificationActions, scroll);
        setContent(page);
    }

    // Displays reporting tools and report data tabs.
    private void showReports() {
        VBox page = page("Reports & Academic Deliverables", "Analytics, fines, top categories, printable report export, and presentation artifacts.");
        Button report = actionButton("Generate PDF report", () -> {
            try {
                Path output = Path.of("smart-library-report.pdf");
                reportService.generatePdfLikeReport(output);
                UiUtil.showInfo("Report generated", "Report saved to " + output.toAbsolutePath());
            } catch (IOException ex) {
                UiUtil.showError("Report error", ex.getMessage());
            }
        });
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Fines", finesTable(libraryService.allFines())),
                new Tab("Reservations", reservationsTable(libraryService.allReservations())),
                new Tab("Activity", logTable())
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        page.getChildren().addAll(report, tabs);
        setContent(page);
    }

    // Displays the system activity log screen.
    private void showLogs() {
        VBox page = page("System Logs", "Security and activity audit trail for administration and evaluation.");
        VBox list = new VBox(10);
        for (ActivityLog log : libraryService.logs()) {
            VBox item = new VBox(6);
            item.getStyleClass().add("notification-item");
            Label action = new Label(log.getAction());
            action.getStyleClass().addAll("panel-title", "notification-title");
            Label date = new Label(formatDate(log.getCreatedAt()));
            date.getStyleClass().add("metadata-label");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(10, action, spacer, date);
            header.setAlignment(Pos.CENTER_LEFT);
            Label details = new Label(log.getActor() + " - " + log.getDetails());
            details.getStyleClass().add("notification-message");
            details.setWrapText(true);
            item.getChildren().addAll(header, details);
            list.getChildren().add(item);
        }
        page.getChildren().add(transparentScroll(list));
        setContent(page);
    }

    // Wraps content in a scroll pane that matches the app style.
    // Parameters: contentNode is the content placed inside the scroll pane.
    private ScrollPane transparentScroll(Parent contentNode) {
        ScrollPane scroll = new ScrollPane(contentNode);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    // Formats a date and time for display in the UI.
    // Parameters: dateTime is the date and time to format.
    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm"));
    }

    // Creates a table for activity log records.
    private TableView<ActivityLog> logTable() {
        TableView<ActivityLog> table = new TableView<>(FXCollections.observableArrayList(libraryService.logs()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Actor", "actor"));
        table.getColumns().add(column("Action", "action"));
        table.getColumns().add(column("Details", "details"));
        table.getColumns().add(column("Date", "createdAt"));
        table.setPlaceholder(new Label("No activity recorded."));
        return table;
    }

    // Exports the book catalog to a CSV file.
    private void exportCsv() {
        try {
            Path path = Path.of("books-export.csv");
            libraryService.exportBooksCsv(path);
            UiUtil.showInfo("CSV exported", "Catalog exported to " + path.toAbsolutePath());
        } catch (IOException ex) {
            UiUtil.showError("Export error", ex.getMessage());
        }
    }

    // Logs out the current user and returns to the login screen.
    private void logout() {
        if (!UiUtil.confirm("Logout confirmation", "Do you want to end the current session?")) return;
        notificationService.notify(currentUser, "Logout", "Session closed successfully.", "INFO");
        sessionManager.logout();
        LoginController login = new LoginController(stage, userService, libraryService, notificationService, reportService, sessionManager);
        stage.getScene().setRoot(login.getView());
    }
}
