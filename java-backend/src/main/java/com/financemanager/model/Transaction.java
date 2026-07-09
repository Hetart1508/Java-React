package com.financemanager.model;

/**
 * Model Class: Transaction
 * 
 * In a standard full-stack application, a Model class represents the data structure 
 * of our domain. In this case, it models a single financial Transaction (Income or Expense).
 * It maps directly to our database table 'transactions'.
 * 
 * Features:
 * - Encapsulates data (using private fields)
 * - Exposes public getters/setters (JavaBeans standard)
 * - Contains constructors for object initialization
 */
public class Transaction {
    // Unique identifier for the transaction (auto-incremented by database)
    private int id;
    
    // Title or short name of the transaction (e.g., "Monthly Salary", "Grocery shopping")
    private String title;
    
    // Monetary value (should be a positive number)
    private double amount;
    
    // Type of transaction: must be "INCOME" or "EXPENSE"
    private String type;
    
    // Category of the transaction (e.g., "Salary", "Food", "Rent", "Utilities")
    private String category;
    
    // Date of transaction (YYYY-MM-DD format as a string, or JDBC Date)
    private String date;
    
    // Detailed description/notes about the transaction
    private String description;

    // Default Constructor (required for frameworks and clean initialization)
    public Transaction() {}

    // Constructor with all fields (useful for retrieving data from database)
    public Transaction(int id, String title, double amount, String type, String category, String date, String description) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    // Constructor without ID (useful for creating a new transaction before saving to DB)
    public Transaction(String title, double amount, String type, String category, String date, String description) {
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    // ==========================================
    // Getters and Setters
    // These allow safe access and modification of private fields (Encapsulation)
    // ==========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type.toUpperCase(); // Ensure uppercase for consistency ("INCOME" or "EXPENSE")
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Overriding toString() for easy debugging
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", category='" + category + '\'' +
                ", date='" + date + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
