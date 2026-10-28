# Software Requirements Specification

## Project Identification

**Project Title:** Smart Library Management System  
**Project Type:** Desktop Library Management Application  
**Technologies:** Java 17, JavaFX, Maven, JDBC, MySQL  
**Architecture:** MVC-oriented layered architecture with Controller, Service, DAO, Model, Utility, and View packages.

## 1. Introduction

The Smart Library Management System is a desktop application designed to support academic library workflows. The system manages books, members, borrowing operations, returns, fines, notifications, dashboards, authentication, and administrative monitoring. It is intended for a Human Computer Interaction and software engineering academic project, therefore the interface and internal organization emphasize usability, role separation, validation, and maintainability.

## 2. Project Overview

The application provides three user roles: Administrator, Librarian, and Member. Each role has a different navigation structure and a different set of permissions. The application uses JavaFX for the graphical user interface, MySQL as the persistent database, JDBC for database access, and Maven for dependency management.

The project is implemented as a structured desktop application rather than a simple CRUD prototype. It contains dashboard statistics, role-based navigation, form validation, popup dialogs, toast notifications, contextual menus, tabs, charts, dark mode, and audit logs.

## 3. Objectives

- Provide a realistic library management workflow.
- Support role-based access control.
- Store operational data in MySQL.
- Offer an intuitive JavaFX interface with organized layouts.
- Demonstrate UML modeling, database modeling, and academic documentation.
- Provide validation, error handling, notifications, and confirmation dialogs.

## 4. Scope

### In Scope

- User authentication.
- Admin, Librarian, and Member roles.
- Book catalog management.
- Borrowing and return operations.
- Reservation management.
- Fine calculation for late returns.
- Notifications.
- Dashboard analytics.
- Activity logs.
- MySQL schema and JDBC integration.
- UML and database documentation.

### Out of Scope

- Real email sending.
- Real online payment of fines.
- Barcode scanner hardware integration.
- Multi-branch distributed library synchronization.

## 5. Technologies Used

| Technology | Purpose |
|---|---|
| Java 17 | Main programming language |
| JavaFX | Desktop graphical user interface |
| Maven | Project build and dependency management |
| MySQL | Relational database |
| JDBC | Database access layer |
| PlantUML | UML modeling |
| CSS | JavaFX visual styling |

## 6. Functional Requirements

| ID | Requirement | Description |
|---|---|---|
| FR-01 | Authentication | Users must log in using email and password. |
| FR-02 | Role-based navigation | The sidebar menu must change according to the user role. |
| FR-03 | User management | Admin can create and activate/suspend users. |
| FR-04 | Member registration | Librarian can register members. |
| FR-05 | Book management | Admin/Librarian can add, update, delete, search, filter, and export books. |
| FR-06 | Book borrowing | Librarian/Admin can issue books to members. |
| FR-07 | Book return | Librarian/Admin can return books and update availability. |
| FR-08 | Fine calculation | The system generates fines for overdue returns. |
| FR-09 | Reservation | Members and librarians can reserve unavailable books. |
| FR-10 | Notifications | The system displays login, borrowing, return, reservation, and broadcast notifications. |
| FR-11 | Dashboard | The system displays statistics, charts, and recent activities. |
| FR-12 | Logs | Administrative actions are recorded in activity logs. |
| FR-13 | Reports | The system generates a simple PDF report. |
| FR-14 | CSV export | Book catalog can be exported to CSV. |
| FR-15 | Dark mode | Users can switch between light and dark visual modes. |

## 7. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | Common UI operations should respond immediately for academic-size datasets. |
| Security | Passwords are stored as SHA-256 hashes instead of plain text. |
| Reliability | Database failures produce clear messages and do not silently claim success. |
| Usability | Screens use clear labels, spacing, tooltips, tabs, alerts, and role-specific navigation. |
| Maintainability | Code is separated into model, DAO, service, controller, utility, and view packages. |
| Scalability | The database schema is normalized enough to support additional reports and workflows. |
| Portability | The project runs through Maven with Java 17 and JavaFX dependencies. |

## 8. Business Rules

| ID | Rule |
|---|---|
| BR-01 | A member cannot borrow more than three active books. |
| BR-02 | A book cannot be borrowed if no copy is available. |
| BR-03 | Duplicate active borrowing for the same member and book is not allowed. |
| BR-04 | Returned books increase the available copy count. |
| BR-05 | Late returns generate fines based on overdue days. |
| BR-06 | ISBN values must be unique. |
| BR-07 | Email values must be unique. |
| BR-08 | Only Admin can manage all users. |
| BR-09 | Admin and Librarian can manage books and circulation. |
| BR-10 | Members can view their own borrowings, fines, reservations, and notifications. |

## 9. Constraints and Assumptions

- MySQL must be running for full persistence.
- Database credentials are configured in `src/main/resources/database.properties` or JVM properties.
- The application can run in demo mode for presentation, but database operations require MySQL connectivity.
- The project targets an academic environment and moderate data volume.
- Real email delivery is simulated through the notification module.

## 10. User Roles

### Administrator

The Administrator manages system-level operations. This role can view global dashboard statistics, manage staff and members, manage catalog records, access reports, view logs, and control user activation status.

### Librarian

The Librarian handles operational circulation tasks. This role can register members, issue books, return books, reserve unavailable books, view pending items, and send notifications.

### Member

The Member uses the system for self-service. This role can search books, view current borrowings, view due dates, check penalties, reserve books, and receive notifications.

