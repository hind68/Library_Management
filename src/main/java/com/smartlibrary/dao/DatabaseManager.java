package com.smartlibrary.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        this.url = firstNonBlank(System.getProperty("smartlibrary.db.url"), System.getenv("SMART_LIBRARY_DB_URL"),
                properties.getProperty("db.url"),
                "jdbc:mysql://localhost:3306/smart_library?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        this.username = firstNonBlank(System.getProperty("smartlibrary.db.user"), System.getenv("SMART_LIBRARY_DB_USER"),
                properties.getProperty("db.user"), "root");
        this.password = firstNonBlankAllowEmpty(System.getProperty("smartlibrary.db.password"),
                System.getenv("SMART_LIBRARY_DB_PASSWORD"), properties.getProperty("db.password"), "");
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
        } catch (IOException ex) {
            lastError = "Could not read database.properties: " + ex.getMessage();
            System.err.println(lastError);
        }
        Path localConfig = Path.of("database-local.properties");
        if (Files.isRegularFile(localConfig)) {
            try (InputStream input = Files.newInputStream(localConfig)) {
                properties.load(input);
            } catch (IOException ex) {
                lastError = "Could not read database-local.properties: " + ex.getMessage();
                System.err.println(lastError);
            }
        }
        return properties;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String firstNonBlankAllowEmpty(String systemValue, String environmentValue,
                                            String propertyValue, String defaultValue) {
        if (systemValue != null) return systemValue;
        if (environmentValue != null) return environmentValue;
        if (propertyValue != null) return propertyValue;
        return defaultValue;
    }
}
