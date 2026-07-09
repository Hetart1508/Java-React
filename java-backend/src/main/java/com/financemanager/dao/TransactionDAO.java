package com.financemanager.dao;

import com.financemanager.model.Transaction;
import com.financemanager.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO): TransactionDAO
 * 
 * The DAO pattern encapsulates all database operations. It handles low-level 
 * JDBC connections, SQL executions, and maps database rows (ResultSet) to Java Objects (Transaction).
 * 
 * Core Features Shown:
 * 1. Safe SQL execution using PreparedStatement (Prevents SQL Injection).
 * 2. Try-with-resources blocks (Auto-closes Connections, Statements, and ResultSets).
 * 3. Retrieval of auto-incremented primary keys (Generated Keys) on insert.
 * 4. Dynamic query generation for Filters and PATCH (partial update) requests.
 */
public class TransactionDAO {

    /**
     * CREATE - Add a new Transaction (POST)
     * Maps to: INSERT INTO transactions ...
     */
    public Transaction create(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (title, amount, type, category, date, description) VALUES (?, ?, ?, ?, ?, ?)";
        
        // Try-with-resources: automatically closes Connection and PreparedStatement when done
        try (Connection conn = DatabaseConnection.getConnection();
             // We specify RETURN_GENERATED_KEYS to grab the new ID created by the database
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Binding parameters: protects against SQL Injection
            // PreparedStatement sanitizes values before executing the SQL statement
            stmt.setString(1, transaction.getTitle());
            stmt.setDouble(2, transaction.getAmount());
            stmt.setString(3, transaction.getType());
            stmt.setString(4, transaction.getCategory());
            stmt.setDate(5, Date.valueOf(transaction.getDate())); // Parses YYYY-MM-DD string to SQL Date
            stmt.setString(6, transaction.getDescription());

            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating transaction failed, no rows affected.");
            }

            // Retrieve the auto-generated primary key (ID)
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating transaction failed, no ID obtained.");
                }
            }
        }
        return transaction;
    }

    /**
     * READ - Get a single Transaction by ID (GET)
     * Maps to: SELECT * FROM transactions WHERE id = ?
     */
    public Transaction getById(int id) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Extract values from the database row and construct a Transaction model object
                    return new Transaction(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getDouble("amount"),
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getDate("date").toString(),
                        rs.getString("description")
                    );
                }
            }
        }
        return null; // Return null if not found (or throw a custom exception)
    }
    
    /**
     * READ ALL with Dynamic Filtering (GET with query parameters)
     * Maps to: SELECT * FROM transactions WHERE [type=?] AND [category=?] AND [date=?]
     */
    public List<Transaction> getAll(String type, String category, String date, String sortBy, String sortDir, int page, int pageSize) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (type != null && !type.trim().isEmpty()) {
            queryBuilder.append(" AND type = ?");
            params.add(type.toUpperCase());
        }
        if (category != null && !category.trim().isEmpty()) {
            queryBuilder.append(" AND category = ?");
            params.add(category);
        }
        if (date != null && !date.trim().isEmpty()) {
            queryBuilder.append(" AND date = ?");
            params.add(Date.valueOf(date));
        }

        String safeSortBy = getSafeSortColumn(sortBy);
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (safePage - 1) * safePageSize;

        queryBuilder.append(" ORDER BY ").append(safeSortBy).append(" ").append(safeSortDir).append(", id DESC");
        queryBuilder.append(" LIMIT ? OFFSET ?");
        params.add(safePageSize);
        params.add(offset);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
            
            // Set dynamic parameters on the compiled statement
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getDouble("amount"),
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getDate("date").toString(),
                        rs.getString("description")
                    ));
                }
            }
        }
        return list;
    }

    public int countAll(String type, String category, String date) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(*) FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (type != null && !type.trim().isEmpty()) {
            queryBuilder.append(" AND type = ?");
            params.add(type.toUpperCase());
        }
        if (category != null && !category.trim().isEmpty()) {
            queryBuilder.append(" AND category = ?");
            params.add(category);
        }
        if (date != null && !date.trim().isEmpty()) {
            queryBuilder.append(" AND date = ?");
            params.add(Date.valueOf(date));
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private String getSafeSortColumn(String sortBy) {
        if ("title".equals(sortBy) || "amount".equals(sortBy) || "type".equals(sortBy) ||
            "category".equals(sortBy) || "date".equals(sortBy)) {
            return sortBy;
        }
        return "date";
    }

    /**
     * UPDATE - Replaces an entire Transaction (PUT)
     * Maps to: UPDATE transactions SET ... WHERE id = ?
     */
    public boolean update(Transaction transaction) throws SQLException {
        String sql = "UPDATE transactions SET title = ?, amount = ?, type = ?, category = ?, date = ?, description = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, transaction.getTitle());
            stmt.setDouble(2, transaction.getAmount());
            stmt.setString(3, transaction.getType());
            stmt.setString(4, transaction.getCategory());
            stmt.setDate(5, Date.valueOf(transaction.getDate()));
            stmt.setString(6, transaction.getDescription());
            stmt.setInt(7, transaction.getId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0; // Returns true if the transaction was updated successfully
        }
    }

    /**
     * PARTIAL UPDATE - Modifies only specific fields (PATCH)
     * Dynamic SQL generation in plain JDBC
     */
    public boolean patch(int id, Map<String, Object> fieldsToUpdate) throws SQLException {
        if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
            return false;
        }

        // Build the dynamic SET clause
        StringBuilder sqlBuilder = new StringBuilder("UPDATE transactions SET ");
        List<Object> values = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Object> entry : fieldsToUpdate.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            // Validate that we only allow updates on existing table columns to prevent SQL injection
            if (field.equals("title") || field.equals("amount") || field.equals("type") || 
                field.equals("category") || field.equals("date") || field.equals("description")) {
                
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append(field).append(" = ?");
                
                // Format type to uppercase if it's being updated
                if (field.equals("type") && value instanceof String) {
                    values.add(((String) value).toUpperCase());
                } else if (field.equals("date") && value instanceof String) {
                    values.add(Date.valueOf((String) value));
                } else {
                    values.add(value);
                }
                i++;
            }
        }

        // Append the WHERE clause
        sqlBuilder.append(" WHERE id = ?");
        values.add(id);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            
            // Set values on statement
            for (int j = 0; j < values.size(); j++) {
                stmt.setObject(j + 1, values.get(j));
            }

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * DELETE - Remove a Transaction (DELETE)
     * Maps to: DELETE FROM transactions WHERE id = ?
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * AGGREGATE SUMMARY - Calculates totals (Summary Dashboard)
     * Maps to: SELECT SUM(...) FROM transactions
     */
    public Map<String, Double> getSummary() throws SQLException {
        Map<String, Double> summary = new HashMap<>();
        String sql = "SELECT " +
                     "  SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as total_income, " +
                     "  SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as total_expense " +
                     "FROM transactions";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                double totalIncome = rs.getDouble("total_income");
                double totalExpense = rs.getDouble("total_expense");
                double currentBalance = totalIncome - totalExpense;

                summary.put("totalIncome", totalIncome);
                summary.put("totalExpense", totalExpense);
                summary.put("currentBalance", currentBalance);
            } else {
                summary.put("totalIncome", 0.0);
                summary.put("totalExpense", 0.0);
                summary.put("currentBalance", 0.0);
            }
        }
        return summary;
    }
}
