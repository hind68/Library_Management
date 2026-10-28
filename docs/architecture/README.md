# Architecture

These diagrams reflect the current Java packages and SQL schema. They intentionally omit UI widgets and routine CRUD methods to keep the system structure readable.

## Application layers

```mermaid
flowchart TB
    subgraph Presentation
        FXML[FXML and JavaFX views]
        Controllers[LoginController, MainController and dialog controllers]
        Session[SessionManager]
    end

    subgraph Business
        UserService
        LibraryService
        NotificationService
        ReportService
        PasswordUtil[PasswordUtil: BCrypt and legacy verification]
    end

    subgraph Persistence
        UserDao
        BookDao
        BorrowingDao
        ReservationDao
        OtherDaos[Fine, Notification and ActivityLog DAOs]
        Transactions[LibraryTransactionDao]
        DatabaseManager
    end

    FXML --> Controllers
    Controllers --> Session
    Controllers --> UserService
    Controllers --> LibraryService
    Controllers --> NotificationService
    Controllers --> ReportService
    UserService --> PasswordUtil
    UserService --> UserDao
    LibraryService --> BookDao
    LibraryService --> BorrowingDao
    LibraryService --> ReservationDao
    LibraryService --> OtherDaos
    LibraryService --> Transactions
    NotificationService --> OtherDaos
    UserDao --> DatabaseManager
    BookDao --> DatabaseManager
    BorrowingDao --> DatabaseManager
    ReservationDao --> DatabaseManager
    OtherDaos --> DatabaseManager
    Transactions --> DatabaseManager
    DatabaseManager --> MySQL[(MySQL smart_library)]
```

`LibraryTransactionDao` owns the multi-table borrowing and return transactions. `BorrowingDao` retains the atomic reservation approval workflow. Service methods update in-memory state only after database success or restore it after rollback.

## Relational model

```mermaid
erDiagram
    ROLES {
        INT id PK
        VARCHAR role_name UK
    }
    USERS {
        INT id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR role_name FK
        BOOLEAN active
    }
    CATEGORIES {
        INT id PK
        VARCHAR name UK
    }
    BOOKS {
        INT id PK
        VARCHAR isbn UK
        VARCHAR category_name FK
        INT quantity
        INT available_copies
    }
    BORROWINGS {
        INT id PK
        INT book_id FK
        INT member_id FK
        INT issued_by FK
        DATE due_date
        DATE return_date
    }
    RESERVATIONS {
        INT id PK
        INT book_id FK
        INT member_id FK
        VARCHAR status
    }
    FINES {
        INT id PK
        INT borrowing_id FK
        INT member_id FK
        DECIMAL amount
    }
    NOTIFICATIONS {
        INT id PK
        INT user_id FK
        BOOLEAN is_read
    }
    ACTIVITY_LOGS {
        INT id PK
        INT actor_id FK
        VARCHAR action
    }

    ROLES ||--o{ USERS : assigns
    CATEGORIES ||--o{ BOOKS : groups
    USERS ||--o{ BORROWINGS : member
    USERS ||--o{ BORROWINGS : staff
    BOOKS ||--o{ BORROWINGS : loaned
    USERS ||--o{ RESERVATIONS : places
    BOOKS ||--o{ RESERVATIONS : reserves
    BORROWINGS ||--o| FINES : generates
    USERS ||--o{ FINES : owes
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ ACTIVITY_LOGS : performs
```

The authoritative schema is [`../../src/main/resources/sql/schema.sql`](../../src/main/resources/sql/schema.sql).
