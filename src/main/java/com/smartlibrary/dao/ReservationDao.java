package com.smartlibrary.dao;

import com.smartlibrary.model.Reservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDao {
    private final DatabaseManager databaseManager;

    // Creates a ReservationDao object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public ReservationDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Loads all matching records from storage.
    public List<Reservation> findAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
                SELECT r.id, r.book_id, r.member_id, b.title AS book_title, u.full_name AS member_name,
                       r.reservation_date, r.status
                FROM reservations r
                JOIN books b ON b.id = r.book_id
                JOIN users u ON u.id = r.member_id
                ORDER BY r.reservation_date DESC, r.id DESC
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                reservations.add(new Reservation(
                        rs.getInt("id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getString("book_title"),
                        rs.getString("member_name"),
                        rs.getDate("reservation_date").toLocalDate(),
                        rs.getString("status")
                ));
            }
        }
        return reservations;
    }

    // Saves a new record to storage and returns the saved model.
    // Parameters: reservation is the reservation record being processed.
    public Reservation save(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations(book_id, member_id, reservation_date, status) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reservation.getBookId());
            statement.setInt(2, reservation.getMemberId());
            statement.setDate(3, Date.valueOf(reservation.getReservationDate()));
            statement.setString(4, reservation.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Reservation(keys.getInt(1), reservation.getBookId(), reservation.getMemberId(),
                            reservation.getBookTitle(), reservation.getMemberName(),
                            reservation.getReservationDate(), reservation.getStatus());
                }
            }
        }
        throw new SQLException("Reservation was inserted, but no generated id was returned.");
    }

    // Saves a new waiting reservation for a member and book.
    // Parameters: bookId is the id of the book; memberId is the id of the member
    // Parameters: bookTitle is the title shown for the book; memberName is the member name shown in the UI.
    public Reservation saveWaiting(int bookId, int memberId, String bookTitle, String memberName) throws SQLException {
        Reservation reservation = new Reservation(0, bookId, memberId, bookTitle, memberName, java.time.LocalDate.now(), "Waiting");
        return save(reservation);
    }

    // Updates the status value of an existing reservation.
    // Parameters: reservation is the reservation record being processed.
    public void updateStatus(Reservation reservation) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE reservations SET status=? WHERE id=?")) {
            statement.setString(1, reservation.getStatus());
            statement.setInt(2, reservation.getId());
            statement.executeUpdate();
        }
    }

    // Deletes a record from storage using its id.
    // Parameters: id is the database id.
    public void deleteById(int id) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM reservations WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }
}
