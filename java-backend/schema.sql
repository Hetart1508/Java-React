-- =====================================================================
-- DATABASE SCHEMA: users + transactions tables
-- =====================================================================
-- This file contains the table creation query and seed data for the 
-- API Practice Finance Manager. It maps direct relationships to the Java
-- model class 'com.financemanager.model.Transaction'.
-- =====================================================================

-- Step 1: Drop tables if they already exist (useful for testing/re-seeding)
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS users;

-- Step 2: Create the users table
-- This table supports signup/login. Passwords are stored as salted hashes,
-- never as plain text.
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 3: Create the transactions table
CREATE TABLE transactions (
    -- Unique identifier of each transaction.
    -- AUTO_INCREMENT creates keys automatically in MySQL.
    -- Maps to: private int id; in Transaction.java
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- Owner of this transaction.
    -- This is what lets sessions control which rows the logged-in user can see.
    user_id INT NOT NULL,
    
    -- Title or brief name of the transaction.
    -- VARCHAR(255) ensures we support descriptive titles while constraining max length.
    -- Maps to: private String title; in Transaction.java
    title VARCHAR(255) NOT NULL,
    
    -- Monetary value.
    -- NUMERIC(12, 2) is used rather than FLOAT or DOUBLE because financial systems
    -- require absolute decimal precision for currency (prevents rounding errors).
    -- Maps to: private double amount; in Transaction.java
    amount NUMERIC(12, 2) NOT NULL,
    
    -- Transaction direction: either INCOME (positive cash flow) or EXPENSE (negative).
    -- VARCHAR(10) with CHECK constraints guarantees data integrity directly at the DB layer.
    -- Maps to: private String type; in Transaction.java
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    
    -- Grouping category (e.g. 'Food', 'Salary', 'Entertainment', 'Bills').
    -- Maps to: private String category; in Transaction.java
    category VARCHAR(100) NOT NULL,
    
    -- Date when the transaction was completed.
    -- DATE stores calendar dates without timezones (YYYY-MM-DD).
    -- Maps to: private String date; in Transaction.java
    date DATE NOT NULL,
    
    -- Optional extended notes or descriptions.
    -- TEXT column allows long description lengths without limits.
    -- Maps to: private String description; in Transaction.java
    description TEXT,

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- Step 4: Performance Optimization (Index Creation)
-- In finance managers, filtering by date and type is extremely common.
-- Creating indexes speeds up SELECT statements when matching WHERE type = ? and WHERE date = ?.
CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_date ON transactions(date);


-- =====================================================================
-- SEED DATA (Initial mock records to populate your learning app)
-- =====================================================================
-- Demo login:
-- Email: demo@example.com
-- Password: password123
INSERT INTO users (name, email, password_hash) VALUES
('Demo User', 'demo@example.com', 'YGgz0V5v20RzcTjAAyRBBQ==:Mju1DYA4qkuQNT5mEaXJLyFCjZomVKAA83mZcPRNNTA=');

INSERT INTO transactions (user_id, title, amount, type, category, date, description) VALUES
(1, 'Monthly Salary Credit', 4500.00, 'INCOME', 'Salary', '2026-07-01', 'Primary job paycheck for July'),
(1, 'Apartment Monthly Rent', 1200.00, 'EXPENSE', 'Rent', '2026-07-02', 'Monthly rent payment including water bill'),
(1, 'Weekly Grocery Run', 154.30, 'EXPENSE', 'Food', '2026-07-03', 'Whole Foods - weekly stock of produce and meals'),
(1, 'Freelance Mobile App Design', 850.00, 'INCOME', 'Freelance', '2026-07-05', 'Ui/Ux landing screen design client milestone'),
(1, 'Electric & Power Bill', 85.00, 'EXPENSE', 'Utilities', '2026-07-06', 'Summer utility billing period'),
(1, 'Local Cafe Coffee & Snack', 12.50, 'EXPENSE', 'Food', '2026-07-07', 'Vanilla latte and croissant with study group');


-- =====================================================================
-- UNDERSTANDING DATA FLOW: REACT -> JAVA BACKEND -> SQL DATABASE
-- =====================================================================
-- Here is how a "POST" transaction request flows step-by-step:
--
-- 1. [REACT FRONTEND]
--    The user enters details into the transaction form and clicks "Add".
--    Axios sends an HTTP POST request:
--    URL: http://localhost:8080/api/transactions
--    Payload: { "title": "Gym Membership", "amount": 45.00, "type": "EXPENSE", ... }
--    The browser also sends the JSESSIONID cookie after login.
--
-- 2. [JAVA SERVLET CONTROLLER]
--    The Tomcat server receives the HTTP request on port 8080 and maps it to TransactionServlet.java.
--    The servlet checks `req.getSession(false)` to find the logged-in userId.
--    It then reads the payload using `req.getReader()`, parses the raw JSON string 
--    and converts it into a Java Transaction object: `new Transaction("Gym Membership", 45.00, "EXPENSE", ...)`
--    The servlet then forwards this Transaction object to TransactionService.java.
--
-- 3. [JAVA SERVICE LAYER]
--    TransactionService.java runs validations (checks if amount is positive, title is not empty).
--    It sanitizes values (trims spaces) and forwards the validated object to TransactionDAO.java.
--
-- 4. [JAVA DAO (JDBC) LAYER]
--    TransactionDAO.java requests a database connection from DatabaseConnection.java (JDBC).
--    It compiles the SQL INSERT template:
--    `INSERT INTO transactions (user_id, title, amount, type, category, date, description) VALUES (?, ?, ?, ?, ?, ?, ?)`
--    It binds the values to avoid SQL injection:
--       - stmt.setString(1, "Gym Membership")
--       - stmt.setDouble(2, 45.00)
--       - etc.
--    It executes the prepared query: `stmt.executeUpdate()`.
--
-- 5. [SQL DATABASE]
--    The SQL Database receives the query, writes a new row to the 'transactions' table,
--    generates a unique primary key ID (e.g. 7), and sends back a success status.
--
-- 6. [FLOW REVERSE]
--    - The database returns the generated ID to the DAO.
--    - The DAO sets the ID on the Transaction object and returns it to the Service.
--    - The Service returns it to the Servlet.
--    - The Servlet converts the Transaction object back into a JSON string and sends an HTTP 201 Created response.
--    - React receives the JSON response via Axios, updates its UI state, and displays the new transaction!
-- =====================================================================
