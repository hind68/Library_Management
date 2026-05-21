package com.smartlibrary.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

public class DatabaseManager {
    private final String url;
    private final String username;
    private final String password;
    private String lastError = "";

    // Creates a DatabaseManager object with the supplied values.
    public DatabaseManager() {
        Properties properties = loadProperties();
        // System properties override the file so the app can be configured at launch time.
        this.url = System.getProperty("smartlibrary.db.url",
                properties.getProperty("db.url", "jdbc:mysql://localhost:3306/smart_library?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"));
        this.username = System.getProperty("smartlibrary.db.user", properties.getProperty("db.user", "root"));
        this.password = System.getProperty("smartlibrary.db.password", properties.getProperty("db.password", ""));
    }

    // Opens a database connection using the configured settings.
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // Checks whether the application can reach the database.
    public boolean canConnect() {
        try (Connection ignored = getConnection()) {
            lastError = "";
            return true;
        } catch (SQLException ex) {
            // Store the message so services can show a helpful database error later.
            lastError = ex.getMessage();
            return false;
        }
    }

    // Returns the url value.
    public String getUrl() {
        return url;
    }

    // Returns the username value.
    public String getUsername() {
        return username;
    }

    // Returns the last error value.
    public Optional<String> getLastError() {
        return lastError == null || lastError.isBlank() ? Optional.empty() : Optional.of(lastError);
    }

    // Loads database settings from the application properties file.
    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseManager.class.getResourceAsStream("/database.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // System properties and defaults are still available.
        }
        return properties;
    }
}
