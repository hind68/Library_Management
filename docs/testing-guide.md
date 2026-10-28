# Testing Guide

## Automated tests

Run the unit and service tests without contacting the production database:

```bash
mvn clean test
```

The current suite validates borrowing limits, availability, duplicate borrowing, returns, fines, reservations, authorization and password compatibility.

## JDBC integration tests

No isolated MySQL test database is bundled or created automatically. Never point integration tests at `smart_library`.

For manual transaction verification, an operator should first create a disposable database such as `smart_library_test` and import a test-only copy of the schema whose `CREATE DATABASE` and `USE` statements have been changed to that exact name. Configure the application against it through environment variables or a temporary ignored local configuration.

Verify the following cases on that disposable database:

1. Successful borrowing creates one borrowing and decrements `available_copies` once.
2. A forced borrowing failure leaves both the borrowing table and stock unchanged.
3. Successful return marks the borrowing returned, restores stock and creates the expected fine when overdue.
4. A forced failure during return rolls back the borrowing, stock, fine and reservation status together.
5. Reservation approval completes the reservation, creates the borrowing and decrements stock atomically.
6. Concurrent attempts cannot issue the final copy twice or create duplicate active borrowing records.
7. A successful login with a copied legacy SHA-256 account replaces only that account's hash with BCrypt.

The application and automated tests do not delete databases.

## JavaFX manual checks

These checks require launching the graphical application and are not covered by the Maven test run:

- Login with each role and verify the permitted navigation items.
- Create a temporary member, borrow and return a book, then confirm the dashboard refreshes.
- Reserve an unavailable book as a member and process it as librarian/admin.
- Restart the application and confirm persisted MySQL records are still visible.
- Stop MySQL and confirm the application clearly enters demonstration mode.
