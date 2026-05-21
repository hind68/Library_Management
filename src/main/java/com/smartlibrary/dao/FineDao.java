package com.smartlibrary.dao;

import com.smartlibrary.model.Fine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FineDao {
    private final DatabaseManager databaseManager;

    // Creates a FineDao object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public FineDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Loads all matching records from storage.
    public List<Fine> findAll() throws SQLException {
        List<Fine> fines = new ArrayList<>();
        String sql = "SELECT id, borrowing_id, member_id, amount, reason, status, created_date FROM fines ORDER BY created_date DESC, id DESC";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                fines.add(new Fine(
                        rs.getInt("id"),
                        rs.getInt("borrowing_id"),
                        rs.getInt("member_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("reason"),
                        rs.getString("status"),
                        rs.getDate("created_date").toLocalDate()
                ));
            }
        }
        return fines;
    }

    // Saves a new record to storage and returns the saved model.
    // Parameters: fine is the fine record being saved.
    public Fine save(Fine fine) throws SQLException {
        String sql = "INSERT INTO fines(borrowing_id, member_id, amount, reason, status, created_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, fine.getBorrowingId());
            statement.setInt(2, fine.getMemberId());
            statement.setBigDecimal(3, fine.getAmount());
            statement.setString(4, fine.getReason());
            statement.setString(5, fine.getStatus());
            statement.setDate(6, Date.valueOf(fine.getCreatedDate()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Fine(keys.getInt(1), fine.getBorrowingId(), fine.getMemberId(),
                            fine.getAmount(), fine.getReason(), fine.getStatus(), fine.getCreatedDate());
                }
            }
        }
        throw new SQLException("Fine was inserted, but no generated id was returned.");
    }
}
