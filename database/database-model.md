# Database Model and SQL Documentation

## Database Overview

The project uses a MySQL relational database named `smart_library`. The database is normalized into separate tables for roles, users, books, categories, borrowings, reservations, fines, notifications, activity logs, and settings.

## Tables Description

| Table | Purpose |
|---|---|
| `roles` | Stores available system roles: ADMIN, LIBRARIAN, MEMBER. |
| `users` | Stores user accounts, authentication data, role assignment, and activation status. |
| `categories` | Stores book categories. |
| `books` | Stores catalog records, ISBN, quantity, availability, shelf location, and cover path. |
| `borrowings` | Stores book issue and return operations. |
| `reservations` | Stores member reservations for books. |
| `fines` | Stores late return penalties. |
| `notifications` | Stores user and broadcast notifications. |
| `activity_logs` | Stores audit trail entries. |
| `library_settings` | Stores configurable business values such as borrowing limit and daily fine. |

## Primary Keys

Each main entity table uses an auto-increment integer primary key:

- `users.id`
- `books.id`
- `borrowings.id`
- `reservations.id`
- `fines.id`
- `notifications.id`
- `activity_logs.id`

Configuration table `library_settings` uses `setting_key` as the primary key.

## Foreign Keys

| Relationship | Foreign Key |
|---|---|
| User role | `users.role_name` references `roles.role_name` |
| Book category | `books.category_name` references `categories.name` |
| Borrowing book | `borrowings.book_id` references `books.id` |
| Borrowing member | `borrowings.member_id` references `users.id` |
| Borrowing staff | `borrowings.issued_by` references `users.id` |
| Reservation book | `reservations.book_id` references `books.id` |
| Reservation member | `reservations.member_id` references `users.id` |
| Fine borrowing | `fines.borrowing_id` references `borrowings.id` |
| Fine member | `fines.member_id` references `users.id` |
| Notification user | `notifications.user_id` references `users.id` |
| Log actor | `activity_logs.actor_id` references `users.id` |

## Constraints

- `users.email` is unique.
- `books.isbn` is unique.
- `books.quantity` must be greater than zero.
- `books.available_copies` must be greater than or equal to zero.
- `books.available_copies` cannot exceed `books.quantity`.
- Roles and categories are separated to reduce duplication.

## SQL Script

The SQL creation script is located at:

`src/main/resources/sql/schema.sql`

It creates the database, all tables, foreign keys, constraints, settings, roles, default users, and sample books. Seed statements use `INSERT IGNORE` so the script can be executed multiple times without duplicate key failures.

## Persistence Mapping

| Application Operation | Database Table |
|---|---|
| Create admin/librarian/member | `users` |
| Add/update/delete book | `books` and possibly `categories` |
| Borrow book | `borrowings` and `books` |
| Return book | `borrowings`, `books`, possibly `fines` and `reservations` |
| Reserve book | `reservations` |
| Send notification | `notifications` |
| Audit action | `activity_logs` |

