-- =====================================================================
-- AUTH MIGRATION: add signup/login tables without dropping existing data
-- =====================================================================
-- Run this if you already had the old finance_java.transactions table and
-- started seeing database errors after adding sessions/login.
--
-- Demo login after this migration:
-- Email: demo@example.com
-- Password: password123
-- =====================================================================

USE finance_java;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (name, email, password_hash)
SELECT 'Demo User', 'demo@example.com', 'YGgz0V5v20RzcTjAAyRBBQ==:Mju1DYA4qkuQNT5mEaXJLyFCjZomVKAA83mZcPRNNTA='
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'demo@example.com'
);

SET @has_user_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND COLUMN_NAME = 'user_id'
);

SET @add_user_id_sql = IF(
    @has_user_id = 0,
    'ALTER TABLE transactions ADD COLUMN user_id INT NULL AFTER id',
    'SELECT ''transactions.user_id already exists'' AS message'
);

PREPARE add_user_id_stmt FROM @add_user_id_sql;
EXECUTE add_user_id_stmt;
DEALLOCATE PREPARE add_user_id_stmt;

UPDATE transactions
SET user_id = (SELECT id FROM users WHERE email = 'demo@example.com')
WHERE user_id IS NULL;

ALTER TABLE transactions
    MODIFY user_id INT NOT NULL;

SET @has_user_index = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND INDEX_NAME = 'idx_transactions_user'
);

SET @add_user_index_sql = IF(
    @has_user_index = 0,
    'CREATE INDEX idx_transactions_user ON transactions(user_id)',
    'SELECT ''idx_transactions_user already exists'' AS message'
);

PREPARE add_user_index_stmt FROM @add_user_index_sql;
EXECUTE add_user_index_stmt;
DEALLOCATE PREPARE add_user_index_stmt;

SET @has_user_fk = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND CONSTRAINT_NAME = 'fk_transactions_user'
);

SET @add_user_fk_sql = IF(
    @has_user_fk = 0,
    'ALTER TABLE transactions ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'SELECT ''fk_transactions_user already exists'' AS message'
);

PREPARE add_user_fk_stmt FROM @add_user_fk_sql;
EXECUTE add_user_fk_stmt;
DEALLOCATE PREPARE add_user_fk_stmt;
