package com.financemanager.service;

import com.financemanager.dao.UserDAO;
import com.financemanager.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Base64;

/**
 * Service layer for authentication.
 *
 * This is where signup/login validation and password hashing live. The Servlet
 * should know HTTP; the Service should know business rules.
 */
public class AuthService {
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 65536;
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final UserDAO userDAO;
    private final SecureRandom secureRandom;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.secureRandom = new SecureRandom();
    }

    public User signup(String name, String email, String password) throws SQLException {
        validateName(name);
        validateEmail(email);
        validatePassword(password);

        try {
            return userDAO.create(name.trim(), normalizeEmail(email), hashPassword(password));
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
    }

    public User login(String email, String password) throws SQLException {
        validateEmail(email);
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        UserDAO.UserRecord record = userDAO.findByEmail(normalizeEmail(email));
        if (record == null || !verifyPassword(password, record.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return record.getUser();
    }

    public User getUserById(int id) throws SQLException {
        return userDAO.findById(id);
    }

    private void validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Name must be at least 2 characters.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !normalizeEmail(email).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("A valid email is required.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyPassword(String password, String storedValue) {
        if (storedValue == null || !storedValue.contains(":")) {
            return false;
        }

        String[] parts = storedValue.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = pbkdf2(password.toCharArray(), salt);

        if (actualHash.length != expectedHash.length) {
            return false;
        }

        int diff = 0;
        for (int i = 0; i < actualHash.length; i++) {
            diff |= actualHash[i] ^ expectedHash[i];
        }
        return diff == 0;
    }

    private byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available.", e);
        }
    }
}
