package config;

import exception.LibraryException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static config.password.yash;

/**
 * Thread-safe Singleton JDBC connection manager.
 *
 * ► Edit the three constants below to match your MySQL installation.
 */
public class DBConnection {
    password a=new password();
    // ── Configuration ───────────────────────────────────────────
    private static final String URL      = "jdbc:mysql://localhost:3306/library_db"
            + "?useSSL=false&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";          // ← change if needed
    private static final String PASSWORD =yash; // ← change to your password

    private static DBConnection instance;
    private Connection connection;

    // ── Private constructor ──────────────────────────────────────
    private DBConnection() throws LibraryException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅  Connected to MySQL database (library_db)");
        } catch (ClassNotFoundException e) {
            throw new LibraryException(
                    "MySQL JDBC Driver not found. Add mysql-connector-j to classpath.",
                    LibraryException.DB_ERROR, e);
        } catch (SQLException e) {
            throw new LibraryException(
                    "Cannot connect to database: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    /**
     * Returns the singleton instance. Creates it on first call.
     */
    public static synchronized DBConnection getInstance() throws LibraryException {
        if (instance == null || isClosed()) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Returns the underlying JDBC Connection. */
    public Connection getConnection() throws LibraryException {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to get DB connection: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
        return connection;
    }

    /** Gracefully closes the connection. */
    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("🔌  Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("Warning: Could not close connection — " + e.getMessage());
            }
        }
    }

    private static boolean isClosed() {
        try {
            return instance == null
                    || instance.connection == null
                    || instance.connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }
}