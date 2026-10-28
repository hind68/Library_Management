# Installation Guide

## Requirements

- JDK 17
- Maven 3.9+
- MySQL 8+ or WAMP/MySQL

## MySQL setup

1. Start the MySQL service.
2. Execute `src/main/resources/sql/schema.sql` once on a local development installation.
3. Copy `database.properties.example` to `database-local.properties` in the project root.
4. Set the local URL, username and password in that copied file.

The local configuration is ignored by Git. JVM properties and the environment variables `SMART_LIBRARY_DB_URL`, `SMART_LIBRARY_DB_USER` and `SMART_LIBRARY_DB_PASSWORD` may be used instead.

## IntelliJ IDEA

1. Open the repository as a Maven project.
2. Select JDK 17 as the Project SDK.
3. Reload Maven dependencies.
4. Run `com.smartlibrary.MainApp` or the Maven goal `javafx:run`.

## Command line

```bash
mvn clean test
mvn javafx:run
```

If MySQL is unavailable, the interface starts with demonstration data. Account creation and other persistence-only operations require a working MySQL connection.
