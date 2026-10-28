package com.smartlibrary.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private final int id;
    private final String actor;
    private final String action;
    private final String details;
    private final LocalDateTime createdAt;

    // Creates an ActivityLog object with the supplied values.
    // Parameters: id is the database id; actor is the user name recorded in the activity log; action is the code to run when selected
    // Parameters: details is the activity details saved in the log; createdAt is the date and time the record was created.
    public ActivityLog(int id, String actor, String action, String details, LocalDateTime createdAt) {
        this.id = id;
        this.actor = actor;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }

    // Returns the id value.
    public int getId() { return id; }
    // Returns the actor value.
    public String getActor() { return actor; }
    // Returns the action value.
    public String getAction() { return action; }
    // Returns the details value.
    public String getDetails() { return details; }
    // Returns the created at value.
    public LocalDateTime getCreatedAt() { return createdAt; }
}
