# Smart Library Management System Requirements

## Objective
Create a professional desktop application for library administration, librarian circulation work, and student/member self-service using Java 17, JavaFX, Maven, MySQL, JDBC, and MVC architecture.

## Roles
- Admin: manages users, librarians, books, fines, reports, settings, and logs.
- Librarian: registers members, issues and returns books, searches catalog, manages availability, views overdue items, and sends notifications.
- Member: searches books, reserves unavailable books, views personal borrow history, due dates, fines, and notifications.

## Business Rules
- A member can borrow a maximum of 3 active books.
- Duplicate active borrowings for the same member and book are blocked.
- Borrowing duration is 14 days by default.
- Late returns generate a fine of 2.00 per overdue day.
- Unavailable books can be reserved.
- Books with active borrowings cannot be deleted.
- ISBN and email values must be unique.

## HCI Principles Demonstrated
- Role-specific navigation reduces cognitive load.
- Dashboard cards summarize status at a glance.
- Dialogs validate errors close to the workflow.
- Toast notifications provide lightweight feedback.
- Dark mode supports user preference.
- Search, filtering, sorting, and tables support efficient information retrieval.
- Consistent spacing, typography, and color improve recognition and presentation quality.

## Team Structure
- Student 1: authentication, user management, JDBC/MySQL schema.
- Student 2: catalog, borrowing, returns, reservations, fines.
- Student 3: UI/UX, dashboards, notifications, reports, diagrams, documentation.
