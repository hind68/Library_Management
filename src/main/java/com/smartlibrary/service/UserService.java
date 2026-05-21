package com.smartlibrary.service;

import com.smartlibrary.dao.DatabaseManager;
import com.smartlibrary.dao.UserDao;
import com.smartlibrary.model.Role;
import com.smartlibrary.model.User;
import com.smartlibrary.util.PasswordUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final DatabaseManager databaseManager;
    private final UserDao userDao;
    private final List<User> users = new ArrayList<>();
    private int nextId = 10;
    private boolean databaseMode;

    // Creates a UserService object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public UserService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.userDao = new UserDao(databaseManager);
        seedUsers();
        try {
            // Database records replace demo users when MySQL is available.
            List<User> databaseUsers = userDao.findAll();
            if (!databaseUsers.isEmpty()) {
                users.clear();
                users.addAll(databaseUsers);
                databaseMode = true;
            }
        } catch (SQLException ex) {
            // Demo mode remains available when MySQL has not been configured yet.
            databaseMode = false;
        }
    }

    // Checks whether the login credentials match an active user.
    // Parameters: email is the user email address; password is the plain password entered by the user.
    public Optional<User> authenticate(String email, String password) {
        return users.stream()
                .filter(User::isActive)
                .filter(user -> user.getEmail().equalsIgnoreCase(email.trim()))
                .filter(user -> PasswordUtil.matches(password, user.getPasswordHash()))
                .findFirst();
    }

    // Loads all matching records from storage.
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    // Returns all users who have the member role.
    public List<User> members() {
        return users.stream().filter(user -> user.getRole() == Role.MEMBER).toList();
    }

    // Validates and creates an active user account.
    // Parameters: name is the person or category name; email is the user email address
    // Parameters: password is the plain password entered by the user; role is the role being checked or assigned.
    public User createUser(String name, String email, String password, Role role) {
        validateUser(name, email, password);
        // Emails must stay unique because they are used as login names.
        users.stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findAny()
                .ifPresent(user -> { throw new IllegalArgumentException("This email is already registered."); });
        String passwordHash = PasswordUtil.hash(password);
        User user;
        if (databaseMode) {
            try {
                user = userDao.save(name, email, passwordHash, role);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database insert failed. Check MySQL connection and the users table constraints.", ex);
            }
        } else {
            throw new IllegalStateException(databaseUnavailableMessage());
        }
        users.add(user);
        return user;
    }

    // Creates a student account that waits for staff activation.
    // Parameters: fullName is the user full name; email is the user email address; password is the plain password entered by the user.
    public User createPendingMember(String fullName, String email, String password) {
        validateUser(fullName, email, password);
        String normalizedEmail = email.trim();
        users.stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(normalizedEmail))
                .findAny()
                .ifPresent(user -> { throw new IllegalArgumentException("This email is already registered."); });
        if (!databaseMode) {
            throw new IllegalStateException(databaseUnavailableMessage());
        }
        String passwordHash = PasswordUtil.hash(password);
        try {
            User user = userDao.savePendingMember(fullName.trim(), normalizedEmail, passwordHash);
            users.add(user);
            return user;
        } catch (SQLException ex) {
            throw new IllegalStateException("Database insert failed. Check MySQL connection and the users table constraints.", ex);
        }
    }

    // Returns whether the service is currently using the database.
    public boolean isDatabaseMode() {
        return databaseMode;
    }

    // Returns a user-friendly explanation when the database is offline.
    public String databaseUnavailableMessage() {
        String error = databaseManager.getLastError().orElse("No successful MySQL connection was detected.");
        return "MySQL is not connected, so this action cannot be saved in the database. "
                + "Configured URL: " + databaseManager.getUrl()
                + ", user: " + databaseManager.getUsername()
                + ". Last error: " + error;
    }

    // Switches a user between active and suspended status.
    // Parameters: user is the user account being checked or updated.
    public void toggleActive(User user) {
        user.setActive(!user.isActive());
        if (databaseMode) {
            try {
                userDao.updateActive(user.getId(), user.isActive());
            } catch (SQLException ex) {
                user.setActive(!user.isActive());
                throw new IllegalStateException("Database update failed for user status.", ex);
            }
        }
    }

    // Saves profile details edited by the current user.
    // Parameters: user is the user account being checked or updated.
    public void updateUserProfile(User user) {
        if (user == null) throw new IllegalArgumentException("No user profile is currently selected.");
        if (user.getRole() == Role.ADMIN) {
            validateAdminUser(user.getFullName(), user.getEmail(), user.getRole());
            users.stream()
                    .filter(existing -> existing.getId() != user.getId())
                    .filter(existing -> existing.getEmail().equalsIgnoreCase(user.getEmail().trim()))
                    .findAny()
                    .ifPresent(existing -> { throw new IllegalArgumentException("This email is already registered."); });
        }
        if (!databaseMode) {
            throw new IllegalStateException(databaseUnavailableMessage());
        }
        try {
            userDao.updateUserProfile(user);
        } catch (SQLException ex) {
            throw new IllegalStateException("Database update failed for user profile.", ex);
        }
    }

    // Saves admin edits to a user account.
    // Parameters: user is the user account being checked or updated; fullName is the user full name; email is the user email address
    // Parameters: role is the role being checked or assigned; active is whether the account is enabled; phone is the user phone number
    // Parameters: address is the user address.
    public void updateAdminUser(User user, String fullName, String email, Role role, boolean active, String phone, String address) {
        if (user == null) throw new IllegalArgumentException("Please select a user before editing.");
        validateAdminUser(fullName, email, role);
        String normalizedEmail = email.trim();
        users.stream()
                .filter(existing -> existing.getId() != user.getId())
                .filter(existing -> existing.getEmail().equalsIgnoreCase(normalizedEmail))
                .findAny()
                .ifPresent(existing -> { throw new IllegalArgumentException("This email is already registered."); });

        String previousName = user.getFullName();
        String previousEmail = user.getEmail();
        Role previousRole = user.getRole();
        boolean previousActive = user.isActive();
        String previousPhone = user.getPhone();
        String previousAddress = user.getAddress();

        // Update the in-memory copy first so the UI can reflect the edited values.
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setRole(role);
        user.setActive(active);
        user.setPhone(phone == null ? "" : phone.trim());
        user.setAddress(address == null ? "" : address.trim());

        if (!databaseMode) {
            // Undo the in-memory change because the edit cannot be persisted.
            rollbackUser(user, previousName, previousEmail, previousRole, previousActive, previousPhone, previousAddress);
            throw new IllegalStateException(databaseUnavailableMessage());
        }
        try {
            userDao.updateAdminUser(user);
        } catch (SQLException ex) {
            // Keep the screen consistent with the database if the update fails.
            rollbackUser(user, previousName, previousEmail, previousRole, previousActive, previousPhone, previousAddress);
            throw new IllegalStateException("Database update failed for user details.", ex);
        }
    }

    // Counts users whose accounts are active.
    public long countActiveUsers() {
        return users.stream().filter(User::isActive).count();
    }

    // Checks required fields for a new account.
    // Parameters: name is the person or category name; email is the user email address
    // Parameters: password is the plain password entered by the user.
    private void validateUser(String name, String email, String password) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("A valid email is required.");
        if (password == null || password.length() < 4) throw new IllegalArgumentException("Password must contain at least 4 characters.");
    }

    // Checks required fields when an admin edits an account.
    // Parameters: name is the person or category name; email is the user email address; role is the role being checked or assigned.
    private void validateAdminUser(String name, String email, Role role) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("A valid email is required.");
        if (role == null) throw new IllegalArgumentException("Role is required.");
    }

    // Restores user fields after a failed database update.
    // Parameters: user is the user account being checked or updated; fullName is the user full name; email is the user email address
    // Parameters: role is the role being checked or assigned; active is whether the account is enabled; phone is the user phone number
    // Parameters: address is the user address.
    private void rollbackUser(User user, String fullName, String email, Role role, boolean active, String phone, String address) {
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(active);
        user.setPhone(phone);
        user.setAddress(address);
    }

    // Adds sample users used when the database is unavailable.
    private void seedUsers() {
        users.add(new User(1, "Admin Sofia", "admin@library.edu", PasswordUtil.hash("admin123"), Role.ADMIN, true, LocalDateTime.now().minusMonths(6)));
        users.add(new User(2, "Librarian Omar", "librarian@library.edu", PasswordUtil.hash("lib123"), Role.LIBRARIAN, true, LocalDateTime.now().minusMonths(4)));
        users.add(new User(3, "Student Lina", "student@library.edu", PasswordUtil.hash("student123"), Role.MEMBER, true, LocalDateTime.now().minusMonths(2)));
        users.add(new User(4, "Yassine Bennani", "yassine@student.edu", PasswordUtil.hash("student123"), Role.MEMBER, true, LocalDateTime.now().minusWeeks(8)));
        users.add(new User(5, "Nora El Fassi", "nora@student.edu", PasswordUtil.hash("student123"), Role.MEMBER, true, LocalDateTime.now().minusWeeks(5)));
    }
}
