package com.smartlibrary.service;

import com.smartlibrary.dao.*;
import com.smartlibrary.model.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryService {
    public static final int MAX_BORROWED_BOOKS = 3;
    public static final int DEFAULT_BORROW_DAYS = 14;
    public static final BigDecimal DAILY_FINE = new BigDecimal("2.00");

    private final BookDao bookDao;
    private final BorrowingDao borrowingDao;
    private final ReservationDao reservationDao;
    private final FineDao fineDao;
    private final ActivityLogDao activityLogDao;
    private final List<Book> books = new ArrayList<>();
    private final List<Borrowing> borrowings = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Fine> fines = new ArrayList<>();
    private final List<ActivityLog> logs = new ArrayList<>();
    private int nextBookId = 100;
    private int nextBorrowingId = 200;
    private int nextReservationId = 300;
    private int nextFineId = 400;
    private int nextLogId = 500;
    private boolean databaseMode;

    // Creates a LibraryService object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public LibraryService(DatabaseManager databaseManager) {
        this.bookDao = new BookDao(databaseManager);
        this.borrowingDao = new BorrowingDao(databaseManager);
        this.reservationDao = new ReservationDao(databaseManager);
        this.fineDao = new FineDao(databaseManager);
        this.activityLogDao = new ActivityLogDao(databaseManager);
        // Seed data keeps the application usable for presentations without MySQL.
        seedBooks();
        seedWorkflow();
        try {
            // When the database is available, replace the demo lists with real data.
            List<Book> databaseBooks = bookDao.findAll();
            if (!databaseBooks.isEmpty()) {
                books.clear();
                books.addAll(databaseBooks);
            }
            borrowings.clear();
            borrowings.addAll(borrowingDao.findAll());
            reservations.clear();
            reservations.addAll(reservationDao.findAll());
            fines.clear();
            fines.addAll(fineDao.findAll());
            logs.clear();
            logs.addAll(activityLogDao.findAll());
            databaseMode = true;
        } catch (SQLException ignored) {
            // Keeps presentation mode functional when MySQL is not running.
            databaseMode = false;
        }
    }

    // Filters books by search text, category, and status.
    // Parameters: query is the search text entered by the user; category is the selected book category
    // Parameters: status is the selected status filter or saved status.
    public List<Book> searchBooks(String query, String category, String status) {
        // Normalize the search text once so comparisons are case-insensitive.
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        return books.stream()
                .filter(book -> normalized.isBlank()
                        || book.getTitle().toLowerCase(Locale.ROOT).contains(normalized)
                        || book.getAuthor().toLowerCase(Locale.ROOT).contains(normalized)
                        || book.getIsbn().toLowerCase(Locale.ROOT).contains(normalized))
                .filter(book -> category == null || category.equals("All") || book.getCategory().equals(category))
                .filter(book -> status == null || status.equals("All") || book.getStatus().equals(status))
                .sorted(Comparator.comparing(Book::getTitle))
                .collect(Collectors.toList());
    }

    // Returns a copy of all books currently loaded by the service.
    public List<Book> allBooks() { return new ArrayList<>(books); }
    // Returns a copy of all borrowing records currently loaded by the service.
    public List<Borrowing> allBorrowings() { return new ArrayList<>(borrowings); }
    // Returns a copy of all reservation records currently loaded by the service.
    public List<Reservation> allReservations() { return new ArrayList<>(reservations); }
    // Returns reservations that are still waiting for approval.
    public List<Reservation> waitingReservations() {
        return reservations.stream()
                .filter(reservation -> reservation.getStatus().equals("Waiting"))
                .sorted(Comparator.comparing(Reservation::getReservationDate))
                .toList();
    }
    // Returns a copy of all fine records currently loaded by the service.
    public List<Fine> allFines() { return new ArrayList<>(fines); }
    // Returns a copy of recent activity log records.
    public List<ActivityLog> logs() { return new ArrayList<>(logs); }

    // Returns the catalog categories used by the filter control.
    public List<String> categories() {
        List<String> categories = books.stream().map(Book::getCategory).distinct().sorted().collect(Collectors.toCollection(ArrayList::new));
        categories.add(0, "All");
        return categories;
    }

    // Validates and adds a new book to the catalog.
    // Parameters: title is the title text; author is the book author; isbn is the book ISBN value
    // Parameters: category is the selected book category; year is the publication year; quantity is the total number of copies
    // Parameters: shelf is the shelf location; coverPath is the optional cover image path
    // Parameters: actor is the user name recorded in the activity log.
    public Book addBook(String title, String author, String isbn, String category, int year, int quantity, String shelf, String coverPath, String actor) {
        validateBook(title, author, isbn, category, year, quantity);
        // Prevent duplicate ISBN values because ISBN is the unique catalog identifier.
        if (books.stream().anyMatch(book -> book.getIsbn().equalsIgnoreCase(isbn))) {
            throw new IllegalArgumentException("Duplicate ISBN detected. Each book record must have a unique ISBN.");
        }
        Book book = new Book(nextBookId++, title, author, isbn, category, year, quantity, quantity, shelf, "Available", coverPath);
        if (databaseMode) {
            try {
                book = bookDao.save(book);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database insert failed for books. Check MySQL category/book constraints.", ex);
            }
        }
        books.add(book);
        log(actor, "BOOK_CREATED", title + " was added to catalog.");
        return book;
    }

    // Validates and saves changes to an existing book.
    // Parameters: book is the book being processed; actor is the user name recorded in the activity log.
    public void updateBook(Book book, String actor) {
        validateBook(book.getTitle(), book.getAuthor(), book.getIsbn(), book.getCategory(), book.getPublicationYear(), book.getQuantity());
        if (book.getAvailableCopies() < 0 || book.getAvailableCopies() > book.getQuantity()) {
            throw new IllegalArgumentException("Available copies must stay between zero and total quantity.");
        }
        book.setStatus(book.getAvailableCopies() > 0 ? "Available" : "Unavailable");
        if (databaseMode) {
            try {
                bookDao.update(book);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database update failed for books.", ex);
            }
        }
        log(actor, "BOOK_UPDATED", book.getTitle() + " was updated.");
    }

    // Deletes a book when it is not currently borrowed.
    // Parameters: book is the book being processed; actor is the user name recorded in the activity log.
    public void deleteBook(Book book, String actor) {
        boolean borrowed = borrowings.stream().anyMatch(item -> item.getBookId() == book.getId() && item.getReturnDate() == null);
        if (borrowed) throw new IllegalArgumentException("This book cannot be deleted while borrowed.");
        if (databaseMode) {
            try {
                bookDao.deleteById(book.getId());
            } catch (SQLException ex) {
                throw new IllegalStateException("Database delete failed for books. Check related borrowings/reservations.", ex);
            }
        }
        books.remove(book);
        log(actor, "BOOK_DELETED", book.getTitle() + " was removed from catalog.");
    }

    // Issues an available book to a library member.
    // Parameters: book is the book being processed; member is the library member involved in the action
    // Parameters: actor is the user name recorded in the activity log.
    public Borrowing borrowBook(Book book, User member, String actor) {
        if (book == null) throw new IllegalArgumentException("Please select a book before issuing a borrowing.");
        if (member == null) throw new IllegalArgumentException("Please select a member before issuing a borrowing.");
        if (member.getRole() != Role.MEMBER) throw new IllegalArgumentException("Books can only be issued to library members.");
        // Enforce the borrowing policy before changing availability.
        long activeBorrowings = borrowings.stream().filter(b -> b.getMemberId() == member.getId() && b.getReturnDate() == null).count();
        if (activeBorrowings >= MAX_BORROWED_BOOKS) throw new IllegalArgumentException("Borrowing limit reached. Maximum allowed books: " + MAX_BORROWED_BOOKS);
        boolean duplicate = borrowings.stream().anyMatch(b -> b.getMemberId() == member.getId() && b.getBookId() == book.getId() && b.getReturnDate() == null);
        if (duplicate) throw new IllegalArgumentException("Duplicate borrowing is not allowed for the same member and book.");
        if (book.getAvailableCopies() <= 0) throw new IllegalArgumentException("Book unavailable. Add the member to the reservation queue instead.");

        // Reduce the available copy count as soon as the book is issued.
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        book.setStatus(book.getAvailableCopies() > 0 ? "Available" : "Unavailable");
        Borrowing borrowing = new Borrowing(nextBorrowingId++, book.getId(), member.getId(), book.getTitle(), member.getFullName(),
                LocalDate.now(), LocalDate.now().plusDays(DEFAULT_BORROW_DAYS), null, "Borrowed");
        if (databaseMode) {
            try {
                borrowing = borrowingDao.save(borrowing, null);
                bookDao.updateAvailability(book);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database insert failed for borrowing.", ex);
            }
        }
        borrowings.add(borrowing);
        log(actor, "BOOK_BORROWED", member.getFullName() + " borrowed " + book.getTitle() + ".");
        return borrowing;
    }

    // Returns a borrowed book and creates a fine if it is overdue.
    // Parameters: borrowing is the borrowing record being processed; actor is the user name recorded in the activity log.
    public void returnBook(Borrowing borrowing, String actor) {
        if (borrowing == null) throw new IllegalArgumentException("Please select a borrowing record before returning a book.");
        if (borrowing.getReturnDate() != null) throw new IllegalArgumentException("This borrowing is already returned.");
        borrowing.setReturnDate(LocalDate.now());
        borrowing.setStatus("Returned");
        books.stream().filter(book -> book.getId() == borrowing.getBookId()).findFirst().ifPresent(book -> {
            book.setAvailableCopies(Math.min(book.getQuantity(), book.getAvailableCopies() + 1));
            book.setStatus(book.getAvailableCopies() > 0 ? "Available" : "Unavailable");
            if (databaseMode) {
                try {
                    bookDao.updateAvailability(book);
                } catch (SQLException ex) {
                    throw new IllegalStateException("Database update failed for book availability.", ex);
                }
            }
        });
        long overdueDays = ChronoUnit.DAYS.between(borrowing.getDueDate(), LocalDate.now());
        if (overdueDays > 0) {
            // Late returns create an unpaid fine based on the number of overdue days.
            Fine fine = new Fine(nextFineId++, borrowing.getId(), borrowing.getMemberId(),
                    DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays)), "Late return: " + overdueDays + " day(s)", "Unpaid", LocalDate.now());
            if (databaseMode) {
                try {
                    fine = fineDao.save(fine);
                } catch (SQLException ex) {
                    throw new IllegalStateException("Database insert failed for fines.", ex);
                }
            }
            fines.add(fine);
        }
        if (databaseMode) {
            try {
                borrowingDao.markReturned(borrowing);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database update failed for returned borrowing.", ex);
            }
        }
        reservations.stream()
                // Notify the first waiting reservation that a copy can now be picked up.
                .filter(reservation -> reservation.getBookId() == borrowing.getBookId() && reservation.getStatus().equals("Waiting"))
                .findFirst()
                .ifPresent(reservation -> {
                    reservation.setStatus("Available");
                    if (databaseMode) {
                        try {
                            reservationDao.updateStatus(reservation);
                        } catch (SQLException ex) {
                            throw new IllegalStateException("Database update failed for reservation status.", ex);
                        }
                    }
                });
        log(actor, "BOOK_RETURNED", borrowing.getBookTitle() + " was returned by " + borrowing.getMemberName() + ".");
    }

    // Adds a member to the waiting list for a book.
    // Parameters: book is the book being processed; member is the library member involved in the action
    // Parameters: actor is the user name recorded in the activity log.
    public Reservation reserveBook(Book book, User member, String actor) {
        if (book == null) throw new IllegalArgumentException("Please select a book before creating a reservation.");
        if (member == null) throw new IllegalArgumentException("Please select a member before creating a reservation.");
        boolean exists = reservations.stream().anyMatch(item -> item.getBookId() == book.getId() && item.getMemberId() == member.getId() && item.getStatus().equals("Waiting"));
        if (exists) throw new IllegalArgumentException("A reservation already exists for this member and book.");
        Reservation reservation = new Reservation(nextReservationId++, book.getId(), member.getId(), book.getTitle(), member.getFullName(), LocalDate.now(), "Waiting");
        if (databaseMode) {
            try {
                reservation = reservationDao.saveWaiting(book.getId(), member.getId(), book.getTitle(), member.getFullName());
            } catch (SQLException ex) {
                throw new IllegalStateException("Database insert failed for reservations.", ex);
            }
        }
        reservations.add(reservation);
        log(actor, "BOOK_RESERVED", member.getFullName() + " reserved " + book.getTitle() + ".");
        return reservation;
    }

    // Creates a reservation from the student home screen.
    // Parameters: book is the book being processed; member is the library member involved in the action.
    public Reservation reserveBookForStudent(Book book, User member) {
        if (member == null || member.getRole() != Role.MEMBER) {
            throw new IllegalArgumentException("Only active member accounts can reserve books from the student home.");
        }
        return reserveBook(book, member, member.getFullName());
    }

    // Approves a waiting reservation and immediately issues the book.
    // Parameters: reservation is the reservation record being processed; staff is the librarian or admin approving the action.
    public Borrowing approveReservationAndIssueBook(Reservation reservation, User staff) {
        if (reservation == null) throw new IllegalArgumentException("Please select a waiting reservation first.");
        if (staff == null || (staff.getRole() != Role.ADMIN && staff.getRole() != Role.LIBRARIAN)) {
            throw new IllegalArgumentException("Only librarians and admins can approve reservations.");
        }
        if (!reservation.getStatus().equals("Waiting")) {
            throw new IllegalArgumentException("Only waiting reservations can be approved.");
        }
        Book book = books.stream()
                .filter(item -> item.getId() == reservation.getBookId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The reserved book no longer exists."));
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalArgumentException("No available copy can be issued for this reservation.");
        }
        boolean alreadyBorrowed = borrowings.stream()
                .anyMatch(item -> item.getBookId() == reservation.getBookId()
                        && item.getMemberId() == reservation.getMemberId()
                        && item.getReturnDate() == null);
        if (alreadyBorrowed) {
            throw new IllegalArgumentException("This member already has an active borrowing for this book.");
        }

        int previousAvailable = book.getAvailableCopies();
        String previousStatus = book.getStatus();
        String previousReservationStatus = reservation.getStatus();
        int availableAfterIssue = previousAvailable - 1;
        Borrowing borrowing = new Borrowing(nextBorrowingId++, book.getId(), reservation.getMemberId(),
                reservation.getBookTitle(), reservation.getMemberName(), LocalDate.now(),
                LocalDate.now().plusDays(DEFAULT_BORROW_DAYS), null, "Borrowed");

        reservation.setStatus("Completed");
        book.setAvailableCopies(availableAfterIssue);
        book.setStatus("Borrowed");
        if (databaseMode) {
            try {
                borrowing = borrowingDao.issueFromReservation(borrowing, reservation.getId(), staff.getId(), availableAfterIssue);
            } catch (SQLException ex) {
                // Restore the in-memory values when the database transaction fails.
                reservation.setStatus(previousReservationStatus);
                book.setAvailableCopies(previousAvailable);
                book.setStatus(previousStatus);
                throw new IllegalStateException("Database transaction failed while approving the reservation.", ex);
            }
        }
        borrowings.add(borrowing);
        log(staff.getFullName(), "RESERVATION_APPROVED", staff.getFullName() + " approved and issued " + reservation.getBookTitle() + " to " + reservation.getMemberName() + ".");
        return borrowing;
    }

    // Cancels a waiting reservation owned by a member.
    // Parameters: reservation is the reservation record being processed; member is the library member involved in the action.
    public void cancelReservation(Reservation reservation, User member) {
        if (reservation == null) throw new IllegalArgumentException("Please select a reservation before cancelling.");
        if (member == null || member.getRole() != Role.MEMBER) {
            throw new IllegalArgumentException("Only member accounts can cancel reservations from this view.");
        }
        if (reservation.getMemberId() != member.getId()) {
            throw new IllegalArgumentException("You can only cancel your own reservations.");
        }
        if (!reservation.getStatus().equals("Waiting")) {
            throw new IllegalArgumentException("Only waiting reservations can be cancelled.");
        }
        if (databaseMode) {
            try {
                reservationDao.deleteById(reservation.getId());
            } catch (SQLException ex) {
                throw new IllegalStateException("Database delete failed for reservations.", ex);
            }
        }
        reservations.removeIf(item -> item.getId() == reservation.getId());
        log(member.getFullName(), "RESERVATION_CANCELLED", member.getFullName() + " cancelled reservation for " + reservation.getBookTitle() + ".");
    }

    // Returns active borrowings for one user.
    // Parameters: user is the user account being checked or updated.
    public List<Borrowing> currentBorrowingsFor(User user) {
        return borrowings.stream().filter(b -> b.getMemberId() == user.getId() && b.getReturnDate() == null).toList();
    }

    // Returns the borrowing history for one user.
    // Parameters: user is the user account being checked or updated.
    public List<Borrowing> historyFor(User user) {
        return borrowings.stream().filter(b -> b.getMemberId() == user.getId()).toList();
    }

    // Returns reservations for one user.
    // Parameters: user is the user account being checked or updated.
    public List<Reservation> reservationsFor(User user) {
        return reservations.stream().filter(r -> r.getMemberId() == user.getId()).toList();
    }

    // Returns fines for one user.
    // Parameters: user is the user account being checked or updated.
    public List<Fine> finesFor(User user) {
        return fines.stream().filter(f -> f.getMemberId() == user.getId()).toList();
    }

    // Calculates the summary numbers shown on the dashboard.
    // Parameters: activeUsers is the number of active user accounts; unreadNotifications is the unread notification count.
    public DashboardStats dashboardStats(int activeUsers, int unreadNotifications) {
        int totalBooks = books.stream().mapToInt(Book::getQuantity).sum();
        int borrowed = (int) borrowings.stream().filter(item -> item.getReturnDate() == null).count();
        int overdue = (int) borrowings.stream().filter(item -> item.getReturnDate() == null && item.getDueDate().isBefore(LocalDate.now())).count();
        int today = (int) borrowings.stream().filter(item -> item.getBorrowDate().equals(LocalDate.now())).count();
        int reservationsCount = (int) reservations.stream().filter(r -> !r.getStatus().equals("Cancelled")).count();
        return new DashboardStats(totalBooks, borrowed, overdue, activeUsers, today, borrowed, unreadNotifications, reservationsCount);
    }

    // Returns the most common book categories.
    public Map<String, Long> topCategories() {
        return getBooksCountByCategory();
    }

    // Returns book counts grouped by category.
    public Map<String, Long> getBooksCountByCategory() {
        if (databaseMode) {
            try {
                Map<String, Long> databaseCategories = bookDao.getBooksCountByCategory();
                if (!databaseCategories.isEmpty()) {
                    return databaseCategories;
                }
            } catch (SQLException ignored) {
                // Falls back to the in-memory catalog used for demo mode.
            }
        }
        return books.stream()
                .collect(Collectors.groupingBy(Book::getCategory, LinkedHashMap::new, Collectors.counting()));
    }

    // Writes the catalog into a CSV file.
    // Parameters: path is the file path used for export.
    public void exportBooksCsv(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("title,author,isbn,category,year,quantity,available,shelf,status");
        for (Book book : books) {
            lines.add(String.join(",",
                    csv(book.getTitle()), csv(book.getAuthor()), csv(book.getIsbn()), csv(book.getCategory()),
                    String.valueOf(book.getPublicationYear()), String.valueOf(book.getQuantity()),
                    String.valueOf(book.getAvailableCopies()), csv(book.getShelfLocation()), csv(book.getStatus())));
        }
        Files.write(path, lines);
    }

    // Escapes one value so it is safe for a CSV file.
    // Parameters: value is the value to format or store.
    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // Checks that book data is complete and reasonable.
    // Parameters: title is the title text; author is the book author; isbn is the book ISBN value
    // Parameters: category is the selected book category; year is the publication year; quantity is the total number of copies.
    private void validateBook(String title, String author, String isbn, String category, int year, int quantity) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Book title is required.");
        if (author == null || author.isBlank()) throw new IllegalArgumentException("Author is required.");
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("ISBN is required.");
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Category is required.");
        if (year < 1400 || year > LocalDate.now().getYear() + 1) throw new IllegalArgumentException("Publication year is invalid.");
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1.");
    }

    // Records an activity message in storage and in memory.
    // Parameters: actor is the user name recorded in the activity log; action is the code to run when selected
    // Parameters: details is the activity details saved in the log.
    public void log(String actor, String action, String details) {
        if (databaseMode) {
            try {
                activityLogDao.save(actor, action, details);
            } catch (SQLException ignored) {
                // Keep UI responsive even if log persistence fails.
            }
        }
        logs.add(0, new ActivityLog(nextLogId++, actor == null ? "System" : actor, action, details, java.time.LocalDateTime.now()));
    }

    // Returns whether the service is currently using the database.
    public boolean isDatabaseMode() {
        return databaseMode;
    }

    // Adds sample books used when the database is unavailable.
    private void seedBooks() {
        books.add(new Book(1, "Designing Interfaces", "Jenifer Tidwell", "9781492051961", "HCI", 2020, 5, 3, "A-12", "Available", ""));
        books.add(new Book(2, "Clean Code", "Robert C. Martin", "9780132350884", "Software Engineering", 2008, 4, 2, "B-04", "Available", ""));
        books.add(new Book(3, "Database System Concepts", "Silberschatz, Korth", "9780078022159", "Database", 2019, 3, 0, "C-02", "Unavailable", ""));
        books.add(new Book(4, "The Design of Everyday Things", "Don Norman", "9780465050659", "HCI", 2013, 6, 5, "A-07", "Available", ""));
        books.add(new Book(5, "Effective Java", "Joshua Bloch", "9780134685991", "Programming", 2018, 4, 4, "B-10", "Available", ""));
        books.add(new Book(6, "Refactoring", "Martin Fowler", "9780134757599", "Software Engineering", 2018, 2, 1, "B-11", "Available", ""));
    }

    // Adds sample borrowings, reservations, fines, and logs for demo mode.
    private void seedWorkflow() {
        borrowings.add(new Borrowing(1, 1, 3, "Designing Interfaces", "Student Lina", LocalDate.now().minusDays(5), LocalDate.now().plusDays(9), null, "Borrowed"));
        borrowings.add(new Borrowing(2, 3, 4, "Database System Concepts", "Yassine Bennani", LocalDate.now().minusDays(20), LocalDate.now().minusDays(6), null, "Overdue"));
        borrowings.add(new Borrowing(3, 2, 5, "Clean Code", "Nora El Fassi", LocalDate.now().minusDays(18), LocalDate.now().minusDays(4), LocalDate.now().minusDays(1), "Returned"));
        reservations.add(new Reservation(1, 3, 3, "Database System Concepts", "Student Lina", LocalDate.now().minusDays(2), "Waiting"));
        fines.add(new Fine(1, 2, 4, new BigDecimal("12.00"), "Late return warning", "Unpaid", LocalDate.now().minusDays(1)));
        log("System", "SEED_DATA", "Demo data loaded for presentation mode.");
    }
}
