package com.smartlibrary.dao;

import com.smartlibrary.model.Borrowing;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BorrowingDao {
    private final DatabaseManager databaseManager;

    // Creates a BorrowingDao object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public BorrowingDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Loads all matching records from storage.
    public List<Borrowing> findAll() throws SQLException {
        List<Borrowing> borrowings = new ArrayList<>();
        String sql = """
                SELECT br.id, br.book_id, br.member_id, b.title AS book_title, u.full_name AS member_name,
                       br.borrow_date, br.due_date, br.return_date, br.status
                FROM borrowings br
                JOIN books b ON b.id = br.book_id
                JOIN users u ON u.id = br.member_id
                ORDER BY br.borrow_date DESC, br.id DESC
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                borrowings.add(map(rs));
            }
        }
        return borrowings;
    }

    // Saves a new record to storage and returns the saved model.
    // Parameters: borrowing is the borrowing record being processed; issuedBy is the staff user id that issued the book.
    public Borrowing save(Borrowing borrowing, Integer issuedBy) throws SQLException {
        String sql = """
                INSERT INTO borrowings(book_id, member_id, issued_by, borrow_date, due_date, return_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, borrowing.getBookId());
            statement.setInt(2, borrowing.getMemberId());
            if (issuedBy == null) statement.setNull(3, Types.INTEGER);
            else statement.setInt(3, issuedBy);
            statement.setDate(4, Date.valueOf(borrowing.getBorrowDate()));
            statement.setDate(5, Date.valueOf(borrowing.getDueDate()));
            if (borrowing.getReturnDate() == null) statement.setNull(6, Types.DATE);
            else statement.setDate(6, Date.valueOf(borrowing.getReturnDate()));
            statement.setString(7, borrowing.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Borrowing(keys.getInt(1), borrowing.getBookId(), borrowing.getMemberId(),
                            borrowing.getBookTitle(), borrowing.getMemberName(), borrowing.getBorrowDate(),
                            borrowing.getDueDate(), borrowing.getReturnDate(), borrowing.getStatus());
                }
            }
        }
        throw new SQLException("Borrowing was inserted, but no generated id was returned.");
    }

    // Issues a reserved book inside one database transaction.
    // Parameters: borrowing is the borrowing record being processed; reservationId is the reservation id being completed
    // Parameters: issuedBy is the staff user id that issued the book
    // Parameters: availableCopiesAfterIssue is the remaining copy count after issuing.
    public Borrowing issueFromReservation(Borrowing borrowing, int reservationId, int issuedBy, int availableCopiesAfterIssue) throws SQLException {
        String updateReservationSql = "UPDATE reservations SET status = 'Completed' WHERE id = ?";
        String insertBorrowingSql = """
                INSERT INTO borrowings(book_id, member_id, issued_by, borrow_date, due_date, return_date, status)
                VALUES (?, ?, ?, ?, ?, NULL, ?)
                """;
        String updateBookSql = "UPDATE books SET available_copies = ?, status = 'Borrowed' WHERE id = ?";
        try (Connection connection = databaseManager.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            // The reservation, borrowing, and book count must succeed or fail together.
            connection.setAutoCommit(false);
            try (PreparedStatement reservationStatement = connection.prepareStatement(updateReservationSql);
                 PreparedStatement borrowingStatement = connection.prepareStatement(insertBorrowingSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement bookStatement = connection.prepareStatement(updateBookSql)) {
                reservationStatement.setInt(1, reservationId);
                reservationStatement.executeUpdate();

                borrowingStatement.setInt(1, borrowing.getBookId());
                borrowingStatement.setInt(2, borrowing.getMemberId());
                borrowingStatement.setInt(3, issuedBy);
                borrowingStatement.setDate(4, Date.valueOf(borrowing.getBorrowDate()));
                borrowingStatement.setDate(5, Date.valueOf(borrowing.getDueDate()));
                borrowingStatement.setString(6, borrowing.getStatus());
                borrowingStatement.executeUpdate();

                int borrowingId;
                try (ResultSet keys = borrowingStatement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Borrowing was inserted, but no generated id was returned.");
                    }
                    borrowingId = keys.getInt(1);
                }

                bookStatement.setInt(1, availableCopiesAfterIssue);
                bookStatement.setInt(2, borrowing.getBookId());
                bookStatement.executeUpdate();

                // Commit only after all three related database changes are complete.
                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
                return new Borrowing(borrowingId, borrowing.getBookId(), borrowing.getMemberId(),
                        borrowing.getBookTitle(), borrowing.getMemberName(), borrowing.getBorrowDate(),
                        borrowing.getDueDate(), borrowing.getReturnDate(), borrowing.getStatus());
            } catch (SQLException ex) {
                // Roll back so a failed issue does not leave partial reservation data.
                connection.rollback();
                connection.setAutoCommit(previousAutoCommit);
                throw ex;
            }
        }
    }

    // Marks a borrowing record as returned in storage.
    // Parameters: borrowing is the borrowing record being processed.
    public void markReturned(Borrowing borrowing) throws SQLException {
        String sql = "UPDATE borrowings SET return_date=?, status=? WHERE id=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(borrowing.getReturnDate()));
            statement.setString(2, borrowing.getStatus());
            statement.setInt(3, borrowing.getId());
            statement.executeUpdate();
        }
    }

    // Builds a model object from the current database row.
    // Parameters: rs is the current database result row.
    private Borrowing map(ResultSet rs) throws SQLException {
        Date returnDate = rs.getDate("return_date");
        return new Borrowing(
                rs.getInt("id"),
                rs.getInt("book_id"),
                rs.getInt("member_id"),
                rs.getString("book_title"),
                rs.getString("member_name"),
                rs.getDate("borrow_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                returnDate == null ? null : returnDate.toLocalDate(),
                rs.getString("status")
        );
    }
}
