package com.smartlibrary.dao;

import com.smartlibrary.model.LibraryNotification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {
    private final DatabaseManager databaseManager;

    // Creates a NotificationDao object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public NotificationDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Loads all matching records from storage.
    public List<LibraryNotification> findAll() throws SQLException {
        List<LibraryNotification> notifications = new ArrayList<>();
        String sql = "SELECT id, user_id, title, message, type, is_read, created_at FROM notifications ORDER BY created_at DESC, id DESC";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                notifications.add(new LibraryNotification(
                        rs.getInt("id"),
                        rs.getObject("user_id") == null ? 0 : rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getBoolean("is_read"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        }
        return notifications;
    }

    // Saves a new record to storage and returns the saved model.
    // Parameters: userId is the user id used for the operation; title is the title text; message is the message shown or saved
    // Parameters: type is the notification type.
    public LibraryNotification save(Integer userId, String title, String message, String type) throws SQLException {
        String sql = "INSERT INTO notifications(user_id, title, message, type, is_read) VALUES (?, ?, ?, ?, FALSE)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (userId == null || userId == 0) statement.setNull(1, Types.INTEGER);
            else statement.setInt(1, userId);
            statement.setString(2, title);
            statement.setString(3, message);
            statement.setString(4, type);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new LibraryNotification(keys.getInt(1), userId == null ? 0 : userId,
                            title, message, type, false, java.time.LocalDateTime.now());
                }
            }
        }
        throw new SQLException("Notification was inserted, but no generated id was returned.");
    }

    // Deletes a record from storage using its id.
    // Parameters: id is the database id.
    public void deleteById(int id) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM notifications WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    // Deletes notifications that belong directly to one user.
    // Parameters: userId is the user id used for the operation.
    public void deleteAllForUser(int userId) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM notifications WHERE user_id = ?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    // Deletes notifications visible to one user, including broadcast messages.
    // Parameters: userId is the user id used for the operation.
    public void deleteVisibleForUser(int userId) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM notifications WHERE user_id = ? OR user_id IS NULL")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    // Deletes every notification from the database.
    public void deleteAll() throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM notifications")) {
            statement.executeUpdate();
        }
    }
}
