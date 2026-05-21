package com.smartlibrary.dao;

import com.smartlibrary.model.Role;
import com.smartlibrary.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private final DatabaseManager databaseManager;

    // Creates a UserDao object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public UserDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Finds one user by email address.
    // Parameters: email is the user email address.
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = """
                SELECT id, full_name, email, password_hash, role_name, active, phone, address, created_at
                FROM users WHERE email = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    // Loads all matching records from storage.
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, full_name, email, password_hash, role_name, active, phone, address, created_at FROM users ORDER BY full_name";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    // Saves a new record to storage and returns the saved model.
    // Parameters: fullName is the user full name; email is the user email address; passwordHash is the already-hashed password
    // Parameters: role is the role being checked or assigned.
    public User save(String fullName, String email, String passwordHash, Role role) throws SQLException {
        String sql = """
                INSERT INTO users(full_name, email, password_hash, role_name, active)
                VALUES (?, ?, ?, ?, TRUE)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getInt(1), fullName, email, passwordHash, role, true, java.time.LocalDateTime.now());
                }
            }
        }
        throw new SQLException("User was inserted, but no generated id was returned.");
    }

    // Saves a new member account that still needs staff approval.
    // Parameters: fullName is the user full name; email is the user email address; passwordHash is the already-hashed password.
    public User savePendingMember(String fullName, String email, String passwordHash) throws SQLException {
        String sql = """
                INSERT INTO users(full_name, email, password_hash, role_name, active)
                VALUES (?, ?, ?, 'MEMBER', FALSE)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getInt(1), fullName, email, passwordHash, Role.MEMBER, false, java.time.LocalDateTime.now());
                }
            }
        }
        throw new SQLException("Pending member was inserted, but no generated id was returned.");
    }

    // Updates whether a user account is active.
    // Parameters: userId is the user id used for the operation; active is whether the account is enabled.
    public void updateActive(int userId, boolean active) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET active=? WHERE id=?")) {
            statement.setBoolean(1, active);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    // Saves profile details edited by the current user.
    // Parameters: user is the user account being checked or updated.
    public void updateUserProfile(User user) throws SQLException {
        String sql = user.getRole() == Role.ADMIN
                ? "UPDATE users SET full_name = ?, email = ?, phone = ?, address = ? WHERE id = ?"
                : "UPDATE users SET phone = ?, address = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (user.getRole() == Role.ADMIN) {
                statement.setString(1, user.getFullName());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getPhone());
                statement.setString(4, user.getAddress());
                statement.setInt(5, user.getId());
            } else {
                statement.setString(1, user.getPhone());
                statement.setString(2, user.getAddress());
                statement.setInt(3, user.getId());
            }
            statement.executeUpdate();
        }
    }

    // Saves admin edits to a user account.
    // Parameters: user is the user account being checked or updated.
    public void updateAdminUser(User user) throws SQLException {
        String sql = """
                UPDATE users
                SET full_name = ?, email = ?, role_name = ?, active = ?, phone = ?, address = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getRole().name());
            statement.setBoolean(4, user.isActive());
            statement.setString(5, user.getPhone());
            statement.setString(6, user.getAddress());
            statement.setInt(7, user.getId());
            statement.executeUpdate();
        }
    }

    // Builds a User object from the current database row.
    // Parameters: rs is the current database result row.
    private User mapUser(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role_name")),
                rs.getBoolean("active"),
                rs.getString("phone"),
                rs.getString("address"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }
}
