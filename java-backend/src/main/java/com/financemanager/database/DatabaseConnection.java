package com.financemanager.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Connection Helper Class (JDBC)
 * 
 * JDBC (Java Database Connectivity) is a core Java API that enables Java applications 
 * to connect to relational databases, execute SQL queries, and manage database transactions.
 * 
 * How it works:
 * 1. Register the JDBC driver for your database (e.g., MySQL).
 * 2. Define the connection parameters: URL, Username, and Password.
 * 3. Use DriverManager.getConnection() to establish a connection pool/session.
 */
public class DatabaseConnection {

    // Connection configuration parameters
    private static final String URL = "jdbc:mysql://localhost:3306/finance_java?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    // Add your MySQL root password between the quotes below.
    private static final String PASSWORD = "Hetarth@1508";
    
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found! Make sure you have the MySQL Connector/J jar in your classpath.");
            e.printStackTrace();
            throw new SQLException("JDBC Driver class missing", e);
        } catch (SQLException e) {
            System.err.println("Failed to establish JDBC Connection!");
            e.printStackTrace();
            throw e;
        }
    }

    public static void closeConnection() {
        // Connections are closed by try-with-resources in DAO methods.
    }
}
