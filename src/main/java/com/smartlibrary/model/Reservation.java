package com.smartlibrary.model;

import java.time.LocalDate;

public class Reservation {
    private final int id;
    private int bookId;
    private int memberId;
    private String bookTitle;
    private String memberName;
    private LocalDate reservationDate;
    private String status;

    // Creates a Reservation object with the supplied values.
    // Parameters: id is the database id; bookId is the id of the book; memberId is the id of the member
    // Parameters: bookTitle is the title shown for the book; memberName is the member name shown in the UI
    // Parameters: reservationDate is the date the member made the reservation; status is the saved reservation status.
    public Reservation(int id, int bookId, int memberId, String bookTitle, String memberName, LocalDate reservationDate, String status) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.bookTitle = bookTitle;
        this.memberName = memberName;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    // Returns the id value.
    public int getId() { return id; }
    // Returns the book id value.
    public int getBookId() { return bookId; }
    // Returns the member id value.
    public int getMemberId() { return memberId; }
    // Returns the book title value.
    public String getBookTitle() { return bookTitle; }
    // Returns the member name value.
    public String getMemberName() { return memberName; }
    // Returns the reservation date value.
    public LocalDate getReservationDate() { return reservationDate; }
    // Returns the status value.
    public String getStatus() { return status; }
    // Updates the status value.
    // Parameters: status is the selected status filter or saved status.
    public void setStatus(String status) { this.status = status; }
}
