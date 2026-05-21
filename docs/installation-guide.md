# Installation Guide

## Requirements
- JDK 17
- Maven 3.9+
- MySQL 8+

## Database Setup
1. Open MySQL Workbench or terminal.
2. Run `src/main/resources/sql/schema.sql`.
3. Optional JVM properties:
   - `-Dsmartlibrary.db.url=jdbc:mysql://localhost:3306/smart_library?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
   - `-Dsmartlibrary.db.user=root`
   - `-Dsmartlibrary.db.password=your_password`

The application also includes demo-mode seed data so the interface can be presented even when MySQL is not running.

## Run
```bash
mvn clean javafx:run
```

## Demo Accounts
- Admin: `admin@library.edu` / `admin123`
- Librarian: `librarian@library.edu` / `lib123`
- Member: `student@library.edu` / `student123`

## Presentation Notes
- Use Admin to show analytics, staff management, reports, and logs.
- Use Librarian to show borrowing, returning, member registration, and notifications.
- Use Member to show personal borrowings, reservations, penalties, and self-service search.
