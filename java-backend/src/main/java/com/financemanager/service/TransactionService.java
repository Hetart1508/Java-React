package com.financemanager.service;

import com.financemanager.dao.TransactionDAO;
import com.financemanager.model.Transaction;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service Layer Class: TransactionService
 * 
 * In a standard enterprise architecture, the Service layer handles "Business Logic".
 * It is located between the HTTP Controller/Servlet layer and the low-level DAO layer.
 * 
 * Why have a Service layer?
 * - Business Rule Validation: (e.g., "Cannot insert a transaction with a negative amount", "Title must not be blank").
 * - Sanitization: Trimming leading/trailing whitespace, standardizing casing.
 * - Transaction Management: Ensuring multiple database actions complete together or fail together.
 * - Separation of Concerns: The controller shouldn't care *how* a transaction is validated, only that it is.
 */
public class TransactionService {

    private final TransactionDAO transactionDAO;

    // Constructor Injection (easier to unit test than static singletons)
    public TransactionService() {
        this.transactionDAO = new TransactionDAO();
    }

    /**
     * Retrieves all transactions with filter options applied.
     */
    public List<Transaction> getAllTransactions(String type, String category, String date, String sortBy, String sortDir, int page, int pageSize) throws SQLException {
        return transactionDAO.getAll(type, category, date, sortBy, sortDir, page, pageSize);
    }

    public int countTransactions(String type, String category, String date) throws SQLException {
        return transactionDAO.countAll(type, category, date);
    }

    /**
     * Retrieves a single transaction by its unique ID.
     */
    public Transaction getTransactionById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Transaction ID must be a positive integer.");
        }
        Transaction t = transactionDAO.getById(id);
        if (t == null) {
            throw new IllegalArgumentException("Transaction with ID " + id + " does not exist.");
        }
        return t;
    }

    /**
     * Validates and inserts a new Transaction.
     */
    public Transaction addTransaction(Transaction transaction) throws SQLException {
        // Run validations
        validateTransaction(transaction);

        // Standardize data (Trim titles, categories)
        transaction.setTitle(transaction.getTitle().trim());
        transaction.setCategory(transaction.getCategory().trim());
        if (transaction.getDescription() != null) {
            transaction.setDescription(transaction.getDescription().trim());
        }

        // Delegate persistence to DAO
        return transactionDAO.create(transaction);
    }

    /**
     * Validates and updates an existing Transaction fully (PUT).
     */
    public boolean updateTransaction(Transaction transaction) throws SQLException {
        // Ensure ID is valid
        if (transaction.getId() <= 0) {
            throw new IllegalArgumentException("A valid Transaction ID is required for a PUT update.");
        }

        // Ensure transaction exists in database first
        Transaction existing = transactionDAO.getById(transaction.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Transaction with ID " + transaction.getId() + " not found.");
        }

        // Run validation rules
        validateTransaction(transaction);

        // Sanitize
        transaction.setTitle(transaction.getTitle().trim());
        transaction.setCategory(transaction.getCategory().trim());

        // Delegate to DAO
        return transactionDAO.update(transaction);
    }

    /**
     * Partially updates an existing Transaction (PATCH).
     */
    public boolean patchTransaction(int id, Map<String, Object> fieldsToUpdate) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("A valid Transaction ID is required for a PATCH update.");
        }

        // Ensure transaction exists
        Transaction existing = transactionDAO.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Transaction with ID " + id + " not found.");
        }

        // Perform partial validation if certain fields are being updated
        if (fieldsToUpdate.containsKey("amount")) {
            Object amtObj = fieldsToUpdate.get("amount");
            double amount = Double.parseDouble(amtObj.toString());
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }
        }

        if (fieldsToUpdate.containsKey("type")) {
            String type = fieldsToUpdate.get("type").toString().toUpperCase();
            if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
                throw new IllegalArgumentException("Type must be either INCOME or EXPENSE.");
            }
        }

        if (fieldsToUpdate.containsKey("title")) {
            String title = fieldsToUpdate.get("title").toString();
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Title cannot be blank.");
            }
        }

        // Delegate to DAO for dynamic query execution
        return transactionDAO.patch(id, fieldsToUpdate);
    }

    /**
     * Deletes a transaction from the system.
     */
    public boolean deleteTransaction(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Transaction ID must be a positive integer.");
        }

        // Ensure transaction exists
        Transaction existing = transactionDAO.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Transaction with ID " + id + " not found.");
        }

        return transactionDAO.delete(id);
    }

    /**
     * Retrieves overall income/expense totals for the dashboard cards.
     */
    public Map<String, Double> getFinanceSummary() throws SQLException {
        return transactionDAO.getSummary();
    }

    // ==========================================
    // Business Rule Validation Helper Method
    // ==========================================
    private void validateTransaction(Transaction t) {
        if (t == null) {
            throw new IllegalArgumentException("Transaction object cannot be null.");
        }
        if (t.getTitle() == null || t.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction title is required and cannot be empty.");
        }
        if (t.getAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be a positive value greater than zero.");
        }
        if (t.getType() == null || (!t.getType().equals("INCOME") && !t.getType().equals("EXPENSE"))) {
            throw new IllegalArgumentException("Transaction type must be either 'INCOME' or 'EXPENSE'.");
        }
        if (t.getCategory() == null || t.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction category is required.");
        }
        if (t.getDate() == null || t.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction date is required.");
        }
        // Date format validation (regex for YYYY-MM-DD)
        if (!t.getDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Date must be in the standard format: YYYY-MM-DD.");
        }
    }
}
