# Smart Library Management System

Java 17 + JavaFX + Maven + MySQL/JDBC desktop application built for an HCI final project.

## Highlights
- MVC-style package separation: `controller`, `model`, `dao`, `service`, `util`, and `view`.
- Role-based authentication for Admin, Librarian, and Member/Student users.
- Modern JavaFX interface with sidebar navigation, dashboard cards, charts, dialogs, notifications, and smooth screen transitions.
- Catalog management with search, category/status filters, book cover path, QR-style book preview, and CSV export.
- Borrow/return workflow with due dates, duplicate-borrowing prevention, max-book rule, overdue detection, and automatic fines.
- Reservation workflow for students, with librarian/admin approval and issuing from the waiting queue.
- User management for admins, member registration for librarians, pending student account requests, and profile editing through My Account.
- Reports screen with fines, reservations, activity logs, and a generated PDF-style project report.
- Academic artifacts: SQL schema, ER diagram, UML diagrams, requirements, and installation material.

## Run With Maven
```bash
mvn clean javafx:run
```

## Run In Eclipse
1. Import the project as an existing Maven project.
2. Make sure the JDK is set to Java 17.
3. Let Eclipse download Maven dependencies.
4. Run `com.smartlibrary.MainApp`.

## Demo Accounts
- Admin: `admin@library.edu` / `admin123`
- Librarian: `librarian@library.edu` / `lib123`
- Member: `student@library.edu` / `student123`

## MySQL Setup
Run [schema.sql](src/main/resources/sql/schema.sql), then start the app with your MySQL credentials if needed:

```bash
mvn javafx:run -Dsmartlibrary.db.user=root -Dsmartlibrary.db.password=your_password
```

You can also set MySQL credentials in [database.properties](src/main/resources/database.properties):

```properties
db.url=jdbc:mysql://localhost:3306/smart_library?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.user=root
db.password=your_password
```

If MySQL is unavailable, the app still opens with built-in presentation data. Database-only actions such as creating a new pending account require a working MySQL connection.

## Persistence Map
- Users, librarians, admins, and student members: `users`
- User roles: `roles`
- Books: `books`
- Categories: `categories`
- Borrow/return operations: `borrowings`
- Reservations: `reservations`
- Late penalties: `fines`
- User and broadcast notifications: `notifications`
- Audit trail: `activity_logs`
- System settings: `library_settings`
