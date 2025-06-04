/**
 * Configuration class for database connection settings in the Hospital Management System.
 * Provides centralized management of database connection parameters and connection creation.
 * This class follows the Singleton pattern to ensure consistent database configuration.
 */
package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    /** Database host address */
    private static final String HOST = "localhost";
    
    /** Database port number */
    private static final String PORT = "3306";
    
    /** Database name */
    private static final String DATABASE = "hospital";
    
    /** Admin username for initial setup */
    private static final String ADMIN_USERNAME = "root";
    
    /** Admin password for initial setup */
    private static final String ADMIN_PASSWORD = "root";
    
    /** Application database username */
    private static final String APP_USERNAME = "hospital_user";
    
    /** Application database password */
    private static final String APP_PASSWORD = "hospital_pwd";

    static {
        // Register MariaDB JDBC driver explicitly
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            System.out.println("MariaDB JDBC Driver registered successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("Error registering MariaDB JDBC driver: " + e.getMessage());
            throw new RuntimeException("Failed to register MariaDB JDBC driver", e);
        }
    }

    /**
     * Private constructor to prevent instantiation.
     * This class should only be used through its static methods.
     */
    private DatabaseConfig() {}

    /**
     * Gets the complete database URL including all necessary parameters.
     * @return The formatted database URL string
     */
    public static String getUrl() {
        // Use basic format without auth parameters that might conflict
        return String.format("jdbc:mariadb://%s:%s/%s", HOST, PORT, DATABASE);
    }

    /**
     * Gets the database URL without specifying a database name.
     * Useful for initial database creation and management.
     * @return The formatted database URL string without database name
     */
    public static String getUrlWithoutDatabase() {
        // Use basic format without auth parameters that might conflict
        return String.format("jdbc:mariadb://%s:%s", HOST, PORT);
    }

    /**
     * Gets the configured database username.
     * @return The database username
     */
    public static String getUsername() {
        return APP_USERNAME;
    }

    /**
     * Gets the configured database password.
     * @return The database password
     */
    public static String getPassword() {
        return APP_PASSWORD;
    }

    /**
     * Gets the configured database name.
     * @return The database name
     */
    public static String getDatabaseName() {
        return DATABASE;
    }

    /**
     * Creates and configures database connection properties.
     * Includes all necessary settings for secure and efficient database operations.
     * @return Properties object containing all database connection settings
     */
    public static Properties getProperties() {
        Properties props = new Properties();
        props.setProperty("user", APP_USERNAME);
        props.setProperty("password", APP_PASSWORD);
        // Only keep essential properties, remove authentication-related ones
        props.setProperty("useSSL", "false");
        return props;
    }

    /**
     * Creates and returns a new database connection using the configured settings.
     * This method should be used for all database operations in the application.
     * If the database doesn't exist or the user doesn't exist, they will be created.
     * 
     * @return A new Connection object to the database
     * @throws SQLException if a database access error occurs or the URL is null
     */
    public static Connection getConnection() throws SQLException {
        System.out.println("Attempting to connect to database with application user: " + APP_USERNAME);
        
        Properties props = getProperties();
        
        try {
            System.out.println("Connecting to: " + getUrl());
            Connection conn = DriverManager.getConnection(getUrl(), props);
            System.out.println("Connection successful with application user!");
            return conn;
        } catch (SQLException e) {
            System.err.println("Error connecting with application user: " + e.getMessage());
            // Error codes:
            // 1049: Database doesn't exist
            // 1045: Access denied (likely the user doesn't exist)
            if (e.getErrorCode() == 1049 || e.getErrorCode() == 1045) {
                System.out.println("Database or user does not exist. Setting up with admin credentials...");
                setupDatabaseAndUser();
                // Try again with the application user
                System.out.println("Attempting to connect again with application user after setup");
                return DriverManager.getConnection(getUrl(), props);
            } else {
                // For any other error, throw the exception
                System.err.println("Database connection error: " + e.getMessage());
                throw new SQLException("Unable to connect to the database. Please check that the MySQL server is running and accessible.", e);
            }
        }
    }

    /**
     * Sets up both the database and application user.
     * 
     * @throws SQLException if a database access error occurs
     */
    private static void setupDatabaseAndUser() throws SQLException {
        System.out.println("Setting up database and user...");
        
        System.out.println("Trying to connect with admin credentials: " + ADMIN_USERNAME);
        
        // Create admin properties - keep it simple
        Properties adminProps = new Properties();
        adminProps.setProperty("user", ADMIN_USERNAME);
        adminProps.setProperty("password", ADMIN_PASSWORD);
        
        try (Connection adminConn = DriverManager.getConnection(getUrlWithoutDatabase(), adminProps);
             java.sql.Statement stmt = adminConn.createStatement()) {
            
            System.out.println("Admin connection successful!");
            
            // Create the database if it doesn't exist
            System.out.println("Creating database: " + DATABASE);
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
            System.out.println("Database '" + DATABASE + "' created or verified successfully");
            
            try {
                // Create user if doesn't exist - use simple SQL syntax
                System.out.println("Creating user: " + APP_USERNAME);
                stmt.executeUpdate(String.format(
                    "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s'",
                    APP_USERNAME, APP_PASSWORD));
                System.out.println("User '" + APP_USERNAME + "' created or verified successfully");
                
                // Grant privileges to the user
                System.out.println("Granting privileges to: " + APP_USERNAME);
                stmt.executeUpdate(String.format(
                    "GRANT ALL PRIVILEGES ON %s.* TO '%s'@'%%'",
                    DATABASE, APP_USERNAME));
                stmt.executeUpdate("FLUSH PRIVILEGES");
                System.out.println("Privileges granted to user '" + APP_USERNAME + "'");
            } catch (SQLException e) {
                // Handle "create user" errors differently
                System.err.println("Error setting up user: " + e.getMessage());
                
                // Try to update the password as a fallback
                try {
                    System.out.println("Updating user password for: " + APP_USERNAME);
                    stmt.executeUpdate(String.format(
                        "ALTER USER '%s'@'%%' IDENTIFIED BY '%s'",
                        APP_USERNAME, APP_PASSWORD));
                    
                    // Grant privileges again to be sure
                    System.out.println("Granting privileges after password update");
                    stmt.executeUpdate(String.format(
                        "GRANT ALL PRIVILEGES ON %s.* TO '%s'@'%%'",
                        DATABASE, APP_USERNAME));
                    stmt.executeUpdate("FLUSH PRIVILEGES");
                    System.out.println("User password updated and privileges granted");
                } catch (SQLException innerEx) {
                    System.err.println("Failed to update user: " + innerEx.getMessage());
                    throw new SQLException("Failed to update database user privileges. You may need to check your MySQL server configuration.", innerEx);
                }
            }
        } catch (SQLException e) {
            System.err.println("Admin connection failed: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Could not connect to the MySQL server with admin credentials. Please check that your MySQL server is properly configured and running.", e);
        }
    }
} 