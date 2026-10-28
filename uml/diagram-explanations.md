# UML Diagram Explanations

## Use Case Diagram

The use case diagram identifies the three main actors: Administrator, Librarian, and Member. It shows that all roles authenticate and access a dashboard, while permissions differ according to responsibility. Admin has the broadest access, Librarian focuses on circulation and member support, and Member uses self-service functions.

## Class Diagram

The class diagram reflects the implemented project structure. Controllers coordinate JavaFX screens, services enforce business rules, DAOs manage JDBC communication, and models represent the core domain entities. Relationships show how users borrow and reserve books and how fines are linked to borrowings.

## Login Sequence

The login sequence describes credential submission, password verification through `PasswordUtil`, session creation through `SessionManager`, and role-based transition to `MainController`.

## Borrowing Sequence

The borrowing sequence shows selection of a member and book, validation of business rules, insertion into `borrowings`, update of book availability, notification generation, and UI feedback.

## Returning Sequence

The return sequence shows how the system updates the borrowing record, restores available copies, creates a fine when the return is late, updates reservations, and displays confirmation.

## Add Book Sequence

The add book sequence describes form submission, validation, category creation if needed, insertion into `books`, audit logging, and catalog refresh.

## User Management Sequence

The user management sequence shows Admin creating a user, password hashing, database insertion, and UI refresh.

