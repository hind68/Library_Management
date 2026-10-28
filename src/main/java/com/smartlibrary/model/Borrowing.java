package com.smartlibrary.model;

import java.time.LocalDate;

public class Borrowing {
    private final int id;
    private int bookId;
    private int memberId;
    private String bookTitle;
    private String memberName;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;

    // Creates a Borrowing object with the supplied values.
    // Parameters: id is the database id; bookId is the id of the book; memberId is the id of the member
    // Parameters: bookTitle is the title shown for the book; memberName is the member name shown in the UI
    // Parameters: borrowDate is the date the book was borrowed; dueDate is the date the book should be returned
    // Parameters: returnDate is the actual return date; status is the selected status filter or saved status.
    public Borrowing(int id, int bookId, int memberId, String bookTitle, String memberName,
                     LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, String status) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.bookTitle = bookTitle;
        this.memberName = memberName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // Returns the id value.
    public int getId() { return id; }
    // Returns the book id value.
    public int getBookId() { return bookId; }
    // Updates the book id value.
    // Parameters: bookId is the id of the book.
    public void setBookId(int bookId) { this.bookId = bookId; }
    // Returns the member id value.
    public int getMemberId() { return memberId; }
    // Updates the member id value.
    // Parameters: memberId is the id of the member.
    public void setMemberId(int memberId) { this.memberId = memberId; }
    // Returns the book title value.
    public String getBookTitle() { return bookTitle; }
    // Updates the book title value.
    // Parameters: bookTitle is the title shown for the book.
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    // Returns the member name value.
    public String getMemberName() { return memberName; }
    // Updates the member name value.
    // Parameters: memberName is the member name shown in the UI.
    public void setMemberName(String memberName) { this.memberName = memberName; }
    // Returns the borrow date value.
    public LocalDate getBorrowDate() { return borrowDate; }
    // Updates the borrow date value.
    // Parameters: borrowDate is the date the book was borrowed.
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    // Returns the due date value.
    public LocalDate getDueDate() { return dueDate; }
    // Updates the due date value.
    // Parameters: dueDate is the date the book should be returned.
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    // Returns the return date value.
    public LocalDate getReturnDate() { return returnDate; }
    // Updates the return date value.
    // Parameters: returnDate is the actual return date.
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    // Returns the status value.
    public String getStatus() { return status; }
    // Updates the status value.
    // Parameters: status is the selected status filter or saved status.
    public void setStatus(String status) { this.status = status; }
}
