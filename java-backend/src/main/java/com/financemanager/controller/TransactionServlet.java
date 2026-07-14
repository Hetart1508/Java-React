package com.financemanager.controller;

import com.financemanager.model.Transaction;
import com.financemanager.service.TransactionService;

// Note: In an enterprise setting, you would use a JSON library like Google Gson or Jackson.
// To keep this Core Java file clean, compilable, and highly readable, we use pseudo-JSON methods
// with simple manual string parsing to demonstrate the mechanics of reading/writing HTTP streams.
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller/Servlet Layer: TransactionServlet
 * 
 * In standard Java EE (Enterprise Edition) without Spring Boot, we map URLs to Servlets.
 * This servlet handles CRUD operations on financial transactions by overriding standard HTTP methods.
 * 
 * Handles transaction CRUD requests.
 */
@WebServlet("/api/transactions/*") // Maps all requests starting with /api/transactions
public class TransactionServlet extends HttpServlet {

    private TransactionService transactionService;

    // Initialize the servlet and create instance of service layer
    @Override
    public void init() throws ServletException {
        this.transactionService = new TransactionService();
    }

    /**
     * Set CORS headers to allow React (running on another port like 3000/5173) 
     * to safely query our Java REST API.
     */
    private void setAccessControlHeaders(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        resp.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Vary", "Origin");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
    }

    /**
     * Handles HTTP OPTIONS pre-flight requests (common in React cross-origin Axios calls)
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * READ - GET Requests
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        PrintWriter out = resp.getWriter();
        Integer userId = requireLoggedInUser(req, resp, out);
        if (userId == null) {
            return;
        }
        
        try {
            String pathInfo = req.getPathInfo(); // E.g., "/12" or "/summary" or null

            // Scenario A: GET /api/transactions/summary -> Finance Dashboard Card statistics
            if (pathInfo != null && pathInfo.equals("/summary")) {
                Map<String, Double> summary = transactionService.getFinanceSummary(userId);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(toJson(summary));
                return;
            }

            // Scenario B: GET /api/transactions/{id} -> Single transaction lookup
            if (pathInfo != null && !pathInfo.equals("/")) {
                // Parse ID from URL path (e.g., "/12" -> "12" -> 12)
                int id = Integer.parseInt(pathInfo.substring(1));
                Transaction transaction = transactionService.getTransactionById(id, userId);
                
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(toJson(transaction));
                return;
            }

            String type = req.getParameter("type");
            String category = req.getParameter("category");
            String date = req.getParameter("date");
            String sortBy = getStringParam(req, "sortBy", "date");
            String sortDir = getStringParam(req, "sortDir", "desc");
            int page = getIntParam(req, "page", 1);
            int pageSize = getIntParam(req, "pageSize", 10);

            List<Transaction> transactions = transactionService.getAllTransactions(userId, type, category, date, sortBy, sortDir, page, pageSize);
            int total = transactionService.countTransactions(userId, type, category, date);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            out.print(paginatedTransactionsToJson(transactions, total, page, pageSize));

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid transaction ID format.\"}");
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error occurred while fetching transactions: " + e.getMessage() + "\"}");
        }
    }

    /**
     * CREATE - POST Requests
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        PrintWriter out = resp.getWriter();
        Integer userId = requireLoggedInUser(req, resp, out);
        if (userId == null) {
            return;
        }

        try {
            // Read the JSON request body string from the input stream
            String body = readRequestBody(req);
            Transaction newTransaction = parseJsonToTransaction(body);

            // Send to service layer for validation and persistence
            Transaction saved = transactionService.addTransaction(newTransaction, userId);

            // Return 201 Created status and the newly created object (including its DB generated ID)
            resp.setStatus(HttpServletResponse.SC_CREATED);
            out.print(toJson(saved));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to create transaction: " + e.getMessage() + "\"}");
        }
    }

    /**
     * UPDATE - PUT & PATCH Requests
     * Classic Java Servlets do not have a dedicated doPatch() method.
     * Often, PATCH is processed inside service() or mapped inside doPut() or doPost() using method override headers.
     * Here, we show how we handle PUT (complete rewrite) and route PATCH.
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        PrintWriter out = resp.getWriter();
        Integer userId = requireLoggedInUser(req, resp, out);
        if (userId == null) {
            return;
        }

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Transaction ID is required for update.\"}");
                return;
            }

            int id = Integer.parseInt(pathInfo.substring(1));
            String body = readRequestBody(req);
            
            // PUT replaces everything
            Transaction updatedTransaction = parseJsonToTransaction(body);
            updatedTransaction.setId(id);

            boolean success = transactionService.updateTransaction(updatedTransaction, userId);
            if (success) {
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(toJson(updatedTransaction));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Transaction to update not found.\"}");
            }

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error during update: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Standard Java Servlets forward PATCH requests through the service() method
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp); // Delegates GET, POST, PUT, DELETE, OPTIONS, etc.
        }
    }

    /**
     * PARTIAL UPDATE - PATCH Requests
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        PrintWriter out = resp.getWriter();
        Integer userId = requireLoggedInUser(req, resp, out);
        if (userId == null) {
            return;
        }

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Transaction ID is required for partial update.\"}");
                return;
            }

            int id = Integer.parseInt(pathInfo.substring(1));
            String body = readRequestBody(req);
            
            // Parse payload into a generic Key-Value map for partial updates
            Map<String, Object> fieldsToUpdate = parseJsonToMap(body);

            boolean success = transactionService.patchTransaction(id, userId, fieldsToUpdate);
            if (success) {
                Transaction updated = transactionService.getTransactionById(id, userId);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(toJson(updated));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Transaction to update not found.\"}");
            }

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error during partial update: " + e.getMessage() + "\"}");
        }
    }

    /**
     * DELETE - DELETE Requests
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        PrintWriter out = resp.getWriter();
        Integer userId = requireLoggedInUser(req, resp, out);
        if (userId == null) {
            return;
        }

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Transaction ID is required for deletion.\"}");
                return;
            }

            int id = Integer.parseInt(pathInfo.substring(1));
            boolean success = transactionService.deleteTransaction(id, userId);
            
            if (success) {
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"message\": \"Transaction deleted successfully.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Transaction to delete not found.\"}");
            }

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to delete transaction: " + e.getMessage() + "\"}");
        }
    }

    // ==========================================
    // JSON Parsing & Writing Helpers
    // In a real project, we would use Jackson: mapper.readValue(body, Transaction.class)
    // To explain JSON mechanics without libraries, these are simple educational equivalents.
    // ==========================================

    private String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private Integer requireLoggedInUser(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Please log in first.\"}");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    private int getIntParam(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getStringParam(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private Transaction parseJsonToTransaction(String json) {
        // Simple manual JSON string parsing logic for demonstration (educational purpose)
        Transaction t = new Transaction();
        json = json.trim().replaceAll("[{}\"]", ""); // Remove curly braces and quotes
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length < 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();

            switch (key) {
                case "title": t.setTitle(val); break;
                case "amount": t.setAmount(Double.parseDouble(val)); break;
                case "type": t.setType(val); break;
                case "category": t.setCategory(val); break;
                case "date": t.setDate(val); break;
                case "description": t.setDescription(val); break;
            }
        }
        return t;
    }

    private Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> map = new java.util.HashMap<>();
        json = json.trim().replaceAll("[{}\"]", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length < 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();
            
            // Save type accordingly
            if (key.equals("amount")) {
                map.put(key, Double.parseDouble(val));
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    private String toJson(Transaction t) {
        return String.format(
            "{\"id\":%d,\"title\":\"%s\",\"amount\":%.2f,\"type\":\"%s\",\"category\":\"%s\",\"date\":\"%s\",\"description\":\"%s\"}",
            t.getId(), escapeJson(t.getTitle()), t.getAmount(), t.getType(), escapeJson(t.getCategory()), t.getDate(), escapeJson(t.getDescription())
        );
    }

    private String transactionsToJson(List<Transaction> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJson(list.get(i)));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String paginatedTransactionsToJson(List<Transaction> list, int total, int page, int pageSize) {
        return String.format(
            "{\"items\":%s,\"total\":%d,\"page\":%d,\"pageSize\":%d}",
            transactionsToJson(list),
            total,
            page,
            pageSize
        );
    }

    private String toJson(Map<String, Double> map) {
        return String.format(
            "{\"totalIncome\":%.2f,\"totalExpense\":%.2f,\"currentBalance\":%.2f}",
            map.getOrDefault("totalIncome", 0.0),
            map.getOrDefault("totalExpense", 0.0),
            map.getOrDefault("currentBalance", 0.0)
        );
    }

    private String escapeJson(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
