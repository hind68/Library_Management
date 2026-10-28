# Merise Model

## Conceptual Data Model

Entities:

- Role
- User
- Category
- Book
- Borrowing
- Reservation
- Fine
- Notification
- ActivityLog
- LibrarySetting

## Associations

- A role can be assigned to many users.
- A category can contain many books.
- A user can create many borrowings as a member.
- A staff user can issue many borrowings.
- A book can appear in many borrowing records.
- A user can create many reservations.
- A book can have many reservations.
- A borrowing can generate zero or one fine.
- A user can receive many notifications.
- A user can appear in many activity logs.

## Logical Data Model

The logical model is represented by the SQL schema in `src/main/resources/sql/schema.sql`. Foreign keys enforce the relationships described above.

## Physical Data Model

The physical implementation uses MySQL data types:

- `INT AUTO_INCREMENT` for identifiers.
- `VARCHAR` for textual data.
- `DATE` for borrowing and fine dates.
- `TIMESTAMP` for creation dates.
- `DECIMAL(10,2)` for fine amounts.
- `BOOLEAN` for account activation and notification read status.

