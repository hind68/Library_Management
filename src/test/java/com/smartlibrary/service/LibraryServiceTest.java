package com.smartlibrary.service;

import com.smartlibrary.dao.DatabaseManager;
import com.smartlibrary.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {
    private LibraryService service;
    private User member;
    private User staff;

    @BeforeEach
    void setUp() {
        service = new LibraryService(new DatabaseManager(), true);
        member = new User(90, "Test Member", "member@test.invalid", "hash", Role.MEMBER, true, LocalDateTime.now());
        staff = new User(80, "Test Librarian", "staff@test.invalid", "hash", Role.LIBRARIAN, true, LocalDateTime.now());
    }

    @Test
    void borrowingDecrementsAvailabilityAndSetsDueDate() {
        Book book = book(5);
        int before = book.getAvailableCopies();

        Borrowing borrowing = service.borrowBook(book, member, staff);

        assertEquals(before - 1, book.getAvailableCopies());
        assertEquals(LocalDate.now().plusDays(LibraryService.DEFAULT_BORROW_DAYS), borrowing.getDueDate());
        assertNull(borrowing.getReturnDate());
    }

    @Test
    void unavailableBookCannotBeBorrowed() {
        Book unavailable = book(3);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.borrowBook(unavailable, member, staff));

        assertTrue(error.getMessage().contains("unavailable"));
        assertEquals(0, unavailable.getAvailableCopies());
    }

    @Test
    void duplicateActiveBorrowingIsRejectedWithoutChangingStockTwice() {
        Book book = book(5);
        service.borrowBook(book, member, staff);
        int afterFirstBorrowing = book.getAvailableCopies();

        assertThrows(IllegalArgumentException.class, () -> service.borrowBook(book, member, staff));

        assertEquals(afterFirstBorrowing, book.getAvailableCopies());
    }

    @Test
    void memberCannotExceedBorrowingLimit() {
        service.borrowBook(book(1), member, staff);
        service.borrowBook(book(2), member, staff);
        service.borrowBook(book(4), member, staff);

        assertThrows(IllegalArgumentException.class, () -> service.borrowBook(book(5), member, staff));
        assertEquals(LibraryService.MAX_BORROWED_BOOKS, service.currentBorrowingsFor(member).size());
    }

    @Test
    void returnRestoresAvailabilityAndCannotBeRepeated() {
        Book book = book(5);
        int initialAvailability = book.getAvailableCopies();
        Borrowing borrowing = service.borrowBook(book, member, staff);

        service.returnBook(borrowing, staff);

        assertEquals(initialAvailability, book.getAvailableCopies());
        assertEquals("Returned", borrowing.getStatus());
        assertEquals(LocalDate.now(), borrowing.getReturnDate());
        assertThrows(IllegalArgumentException.class, () -> service.returnBook(borrowing, staff));
    }

    @Test
    void overdueReturnCreatesFineUsingDailyRate() {
        Borrowing borrowing = service.borrowBook(book(5), member, staff);
        borrowing.setDueDate(LocalDate.now().minusDays(4));

        service.returnBook(borrowing, staff);

        Fine fine = service.finesFor(member).get(0);
        assertEquals(0, new BigDecimal("8.00").compareTo(fine.getAmount()));
        assertEquals("Unpaid", fine.getStatus());
    }

    @Test
    void duplicateWaitingReservationIsRejected() {
        Book book = book(3);
        Reservation reservation = service.reserveBook(book, member, member);

        assertEquals("Waiting", reservation.getStatus());
        assertThrows(IllegalArgumentException.class, () -> service.reserveBook(book, member, member));
        assertEquals(1, service.reservationsFor(member).size());
    }

    @Test
    void returningBookMakesOldestWaitingReservationAvailable() {
        Book book = book(5);
        User waitingMember = new User(91, "Waiting Member", "waiting@test.invalid", "hash", Role.MEMBER, true, LocalDateTime.now());
        Reservation reservation = service.reserveBook(book, waitingMember, waitingMember);
        Borrowing borrowing = service.borrowBook(book, member, staff);

        service.returnBook(borrowing, staff);

        assertEquals("Available", reservation.getStatus());
    }

    @Test
    void forcedDemoModeIsExplicit() {
        assertFalse(service.isDatabaseMode());
        assertEquals("Demo mode was explicitly requested.", service.getDatabaseError().orElseThrow());
    }

    @Test
    void memberCannotIssueBooksOrReserveForAnotherMember() {
        User otherMember = new User(92, "Other", "other@test.invalid", "hash", Role.MEMBER, true, LocalDateTime.now());

        assertThrows(SecurityException.class, () -> service.borrowBook(book(5), member, member));
        assertThrows(SecurityException.class, () -> service.reserveBook(book(5), otherMember, member));
    }

    private Book book(int id) {
        return service.allBooks().stream().filter(candidate -> candidate.getId() == id).findFirst().orElseThrow();
    }
}
