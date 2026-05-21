package com.smartlibrary.service;

import com.smartlibrary.dao.DatabaseManager;
import com.smartlibrary.dao.NotificationDao;
import com.smartlibrary.model.LibraryNotification;
import com.smartlibrary.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final NotificationDao notificationDao;
    private final List<LibraryNotification> notifications = new ArrayList<>();
    private int nextId = 1;
    private boolean databaseMode;

    // Creates a NotificationService object with the supplied values.
    // Parameters: databaseManager is the helper used to open database connections.
    public NotificationService(DatabaseManager databaseManager) {
        this.notificationDao = new NotificationDao(databaseManager);
        // Demo notifications make the screen useful before the database is connected.
        notifications.add(new LibraryNotification(nextId++, 1, "System health", "MySQL mode is supported. Demo data is active until database credentials are configured.", "INFO", false, LocalDateTime.now().minusHours(2)));
        notifications.add(new LibraryNotification(nextId++, 2, "Overdue books", "One member has an overdue database book. Send a reminder today.", "WARNING", false, LocalDateTime.now().minusMinutes(45)));
        notifications.add(new LibraryNotification(nextId++, 3, "Reservation update", "Database System Concepts is in your reservation queue.", "INFO", false, LocalDateTime.now().minusMinutes(22)));
        try {
            List<LibraryNotification> databaseNotifications = notificationDao.findAll();
            notifications.clear();
            notifications.addAll(databaseNotifications);
            databaseMode = true;
        } catch (SQLException ignored) {
            databaseMode = false;
        }
    }

    // Returns notifications visible to one user.
    // Parameters: user is the user account being checked or updated.
    public List<LibraryNotification> forUser(User user) {
        // Admins can review every notification, while members see their own plus broadcasts.
        if (user.getRole().name().equals("ADMIN")) return new ArrayList<>(notifications);
        return notifications.stream().filter(n -> n.getUserId() == user.getId() || n.getUserId() == 0).toList();
    }

    // Counts unread notifications visible to one user.
    // Parameters: user is the user account being checked or updated.
    public int unreadFor(User user) {
        return (int) forUser(user).stream().filter(n -> !n.isRead()).count();
    }

    // Creates a notification for one user.
    // Parameters: user is the user account being checked or updated; title is the title text; message is the message shown or saved
    // Parameters: type is the notification type.
    public void notify(User user, String title, String message, String type) {
        LibraryNotification notification = new LibraryNotification(nextId++, user.getId(), title, message, type, false, LocalDateTime.now());
        if (databaseMode) {
            try {
                notification = notificationDao.save(user.getId(), title, message, type);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database insert failed for notifications.", ex);
            }
        }
        notifications.add(0, notification);
    }

    // Creates the same notification for all users.
    // Parameters: title is the title text; message is the message shown or saved; type is the notification type.
    public void broadcast(String title, String message, String type) {
        LibraryNotification notification = new LibraryNotification(nextId++, 0, title, message, type, false, LocalDateTime.now());
        if (databaseMode) {
            try {
                notification = notificationDao.save(null, title, message, type);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database insert failed for broadcast notification.", ex);
            }
        }
        notifications.add(0, notification);
    }

    // Deletes one notification when the current user is allowed to remove it.
    // Parameters: notification is the notification being removed; currentUser is the user requesting the action.
    public void deleteNotification(LibraryNotification notification, User currentUser) {
        if (notification == null) throw new IllegalArgumentException("Please select a notification to delete.");
        boolean admin = currentUser.getRole().name().equals("ADMIN");
        // Non-admin users may delete their own notifications and visible broadcast notifications.
        if (!admin && notification.getUserId() != currentUser.getId() && notification.getUserId() != 0) {
            throw new IllegalArgumentException("You can only delete your own notifications.");
        }
        if (databaseMode) {
            try {
                notificationDao.deleteById(notification.getId());
            } catch (SQLException ex) {
                throw new IllegalStateException("Database delete failed for notification.", ex);
            }
        }
        notifications.removeIf(item -> item.getId() == notification.getId());
    }

    // Deletes all notifications visible to one user.
    // Parameters: user is the user account being checked or updated.
    public void clearAllForUser(User user) {
        if (user == null) throw new IllegalArgumentException("No user is currently selected.");
        boolean admin = user.getRole().name().equals("ADMIN");
        if (databaseMode) {
            try {
                if (admin) {
                    notificationDao.deleteAll();
                } else {
                    notificationDao.deleteVisibleForUser(user.getId());
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Database delete failed for notifications.", ex);
            }
        }
        if (admin) {
            notifications.clear();
        } else {
            notifications.removeIf(notification -> notification.getUserId() == user.getId() || notification.getUserId() == 0);
        }
    }

    // Deletes all private and broadcast notifications visible to one user id.
    // Parameters: userId is the user id used for the operation.
    public void clearAllForUser(int userId) {
        if (databaseMode) {
            try {
                notificationDao.deleteVisibleForUser(userId);
            } catch (SQLException ex) {
                throw new IllegalStateException("Database delete failed for notifications.", ex);
            }
        }
        notifications.removeIf(notification -> notification.getUserId() == userId || notification.getUserId() == 0);
    }

    // Returns whether the service is currently using the database.
    public boolean isDatabaseMode() {
        return databaseMode;
    }
}
