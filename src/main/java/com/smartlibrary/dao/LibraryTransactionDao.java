package com.smartlibrary.dao;

import com.smartlibrary.model.Book;
import com.smartlibrary.model.Borrowing;
import com.smartlibrary.model.Fine;
import com.smartlibrary.model.Reservation;

import java.sql.*;

/** Coordinates library operations that must be committed atomically. */
public class LibraryTransactionDao {
    private final DatabaseManager databaseManager;

    public LibraryTransactionDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Borrowing issueBorrowing(Borrowing borrowing, Book book, Integer issuedBy, int maxBorrowedBooks) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int available = lockBookAvailability(connection, book.getId());
                if (available <= 0) throw new SQLException("The book is no longer available.");
                if (countActiveBorrowings(connection, borrowing.getMemberId()) >= maxBorrowedBooks) {
                    throw new SQLException("The member borrowing limit was reached concurrently.");
                }
                if (hasActiveBorrowing(connection, borrowing.getBookId(), borrowing.getMemberId())) {
                    throw new SQLException("An active borrowing already exists for this member and book.");
                }

                int id = insertBorrowing(connection, borrowing, issuedBy);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE books SET available_copies=available_copies-1, " +
                                "status=CASE WHEN available_copies-1 > 0 THEN 'Available' ELSE 'Unavailable' END WHERE id=?")) {
                    statement.setInt(1, book.getId());
                    requireOneRow(statement.executeUpdate(), "book availability update");
                }
                connection.commit();
                return copyWithId(borrowing, id);
            } catch (SQLException ex) {
                rollback(connection, ex);
                throw ex;
            } finally {
                restoreAutoCommit(connection, oldAutoCommit);
            }
        }
    }

    public Fine returnBorrowing(Borrowing borrowing, Book book, Fine fine, Reservation nextReservation) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT return_date FROM borrowings WHERE id=? FOR UPDATE")) {
                    statement.setInt(1, borrowing.getId());
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Borrowing not found: " + borrowing.getId());
                        if (rs.getDate(1) != null) throw new SQLException("Borrowing was already returned.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE borrowings SET return_date=?, status='Returned' WHERE id=? AND return_date IS NULL")) {
                    statement.setDate(1, Date.valueOf(borrowing.getReturnDate()));
                    statement.setInt(2, borrowing.getId());
                    requireOneRow(statement.executeUpdate(), "borrowing return update");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE books SET available_copies=LEAST(quantity, available_copies+1), status='Available' WHERE id=?")) {
                    statement.setInt(1, book.getId());
                    requireOneRow(statement.executeUpdate(), "returned book availability update");
                }

                Fine savedFine = fine == null ? null : insertFine(connection, fine);
                if (nextReservation != null) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE reservations SET status='Available' WHERE id=? AND status='Waiting'")) {
                        statement.setInt(1, nextReservation.getId());
                        requireOneRow(statement.executeUpdate(), "reservation availability update");
                    }
                }
                connection.commit();
                return savedFine;
            } catch (SQLException ex) {
                rollback(connection, ex);
                throw ex;
            } finally {
                restoreAutoCommit(connection, oldAutoCommit);
            }
        }
    }

    private int lockBookAvailability(Connection connection, int bookId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT available_copies FROM books WHERE id=? FOR UPDATE")) {
            statement.setInt(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SQLException("Book not found: " + bookId);
                return rs.getInt(1);
            }
        }
    }

    private int countActiveBorrowings(Connection connection, int memberId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM borrowings WHERE member_id=? AND return_date IS NULL FOR UPDATE")) {
            statement.setInt(1, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) count++;
                return count;
            }
        }
    }

    private boolean hasActiveBorrowing(Connection connection, int bookId, int memberId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM borrowings WHERE book_id=? AND member_id=? AND return_date IS NULL FOR UPDATE")) {
            statement.setInt(1, bookId);
            statement.setInt(2, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int insertBorrowing(Connection connection, Borrowing borrowing, Integer issuedBy) throws SQLException {
        String sql = "INSERT INTO borrowings(book_id,member_id,issued_by,borrow_date,due_date,return_date,status) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, borrowing.getBookId());
            statement.setInt(2, borrowing.getMemberId());
            if (issuedBy == null) statement.setNull(3, Types.INTEGER); else statement.setInt(3, issuedBy);
            statement.setDate(4, Date.valueOf(borrowing.getBorrowDate()));
            statement.setDate(5, Date.valueOf(borrowing.getDueDate()));
            statement.setNull(6, Types.DATE);
            statement.setString(7, borrowing.getStatus());
            requireOneRow(statement.executeUpdate(), "borrowing insert");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Borrowing inserted without a generated id.");
                return keys.getInt(1);
            }
        }
    }

    private Fine insertFine(Connection connection, Fine fine) throws SQLException {
        String sql = "INSERT INTO fines(borrowing_id,member_id,amount,reason,status,created_date) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, fine.getBorrowingId());
            statement.setInt(2, fine.getMemberId());
            statement.setBigDecimal(3, fine.getAmount());
            statement.setString(4, fine.getReason());
            statement.setString(5, fine.getStatus());
            statement.setDate(6, Date.valueOf(fine.getCreatedDate()));
            requireOneRow(statement.executeUpdate(), "fine insert");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Fine inserted without a generated id.");
                return new Fine(keys.getInt(1), fine.getBorrowingId(), fine.getMemberId(), fine.getAmount(),
                        fine.getReason(), fine.getStatus(), fine.getCreatedDate());
            }
        }
    }

    private Borrowing copyWithId(Borrowing source, int id) {
        return new Borrowing(id, source.getBookId(), source.getMemberId(), source.getBookTitle(), source.getMemberName(),
                source.getBorrowDate(), source.getDueDate(), source.getReturnDate(), source.getStatus());
    }

    private void requireOneRow(int rows, String operation) throws SQLException {
        if (rows != 1) throw new SQLException("Expected one row for " + operation + ", got " + rows + ".");
    }

    private void rollback(Connection connection, SQLException original) {
        try { connection.rollback(); } catch (SQLException rollbackError) { original.addSuppressed(rollbackError); }
    }

    private void restoreAutoCommit(Connection connection, boolean autoCommit) throws SQLException {
        if (!connection.isClosed()) connection.setAutoCommit(autoCommit);
    }
}
