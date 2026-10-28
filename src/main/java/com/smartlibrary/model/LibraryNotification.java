package com.smartlibrary.model;

import java.time.LocalDateTime;

public class LibraryNotification {
    private final int id;
    private final int userId;
    private final String title;
    private final String message;
    private final String type;
    private boolean read;
    private final LocalDateTime createdAt;

    // Creates a LibraryNotification object with the supplied values.
    // Parameters: id is the database id; userId is the user id used for the operation; title is the title text
    // Parameters: message is the message shown or saved; type is the notification type; read is whether the notification has been read
    // Parameters: createdAt is the date and time the record was created.
    public LibraryNotification(int id, int userId, String title, String message, String type, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = read;
        this.createdAt = createdAt;
    }

    // Returns the id value.
    public int getId() { return id; }
    // Returns the user id value.
    public int getUserId() { return userId; }
    // Returns the title value.
    public String getTitle() { return title; }
    // Returns the message value.
    public String getMessage() { return message; }
    // Returns the type value.
    public String getType() { return type; }
    // Returns whether read is true.
    public boolean isRead() { return read; }
    // Updates the read value.
    // Parameters: read is whether the notification has been read.
    public void setRead(boolean read) { this.read = read; }
    // Returns the created at value.
    public LocalDateTime getCreatedAt() { return createdAt; }
}
