package com.smartlibrary.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Fine {
    private final int id;
    private int borrowingId;
    private int memberId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private LocalDate createdDate;

    // Creates a Fine object with the supplied values.
    // Parameters: id is the database id; borrowingId is the id of the borrowing that caused the fine; memberId is the id of the member
    // Parameters: amount is the fine amount; reason is the reason shown for the fine
    // Parameters: status is the selected status filter or saved status; createdDate is the date the fine was created.
    public Fine(int id, int borrowingId, int memberId, BigDecimal amount, String reason, String status, LocalDate createdDate) {
        this.id = id;
        this.borrowingId = borrowingId;
        this.memberId = memberId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.createdDate = createdDate;
    }

    // Returns the id value.
    public int getId() { return id; }
    // Returns the borrowing id value.
    public int getBorrowingId() { return borrowingId; }
    // Returns the member id value.
    public int getMemberId() { return memberId; }
    // Returns the amount value.
    public BigDecimal getAmount() { return amount; }
    // Returns the reason value.
    public String getReason() { return reason; }
    // Returns the status value.
    public String getStatus() { return status; }
    // Returns the created date value.
    public LocalDate getCreatedDate() { return createdDate; }
}
