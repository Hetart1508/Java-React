package com.financemanager.dao;

import com.financemanager.database.DatabaseConnection;
import com.financemanager.model.User;

import java.sql.*;

/**
 * Data Access Object for user accounts.
 *
 * This keeps all SQL related to authentication in one place, separate from the
 * Servlet and Service layers.
 */
public class UserDAO {

    public User create(String name, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new User(generatedKeys.getInt(1), name, email);
                }
            }
        }

        throw new SQLException("Creating user failed, no ID obtained.");
    }

    public UserRecord findByEmail(String email) throws SQLException {
        String sql = "SELECT id, name, email, password_hash FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserRecord(
                        new User(rs.getInt("id"), rs.getString("name"), rs.getString("email")),
                        rs.getString("password_hash")
                    );
                }
            }
        }

        return null;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, name, email FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"));
                }
            }
        }

        return null;
    }

    public static class UserRecord {
        private final User user;
        private final String passwordHash;

        public UserRecord(User user, String passwordHash) {
            this.user = user;
            this.passwordHash = passwordHash;
        }

        public User getUser() {
            return user;
        }

        public String getPasswordHash() {
            return passwordHash;
        }
    }
}
