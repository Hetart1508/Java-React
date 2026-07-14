package com.financemanager.controller;

import com.financemanager.model.User;
import com.financemanager.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller/Servlet.
 *
 * Demonstrates:
 * - Signup and login endpoints
 * - HttpSession state stored on the server
 * - Cookies stored in the browser
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }

    private void setAccessControlHeaders(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        resp.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Vary", "Origin");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        PrintWriter out = resp.getWriter();

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/me")) {
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\":\"Not logged in.\"}");
                return;
            }

            try {
                int userId = (Integer) session.getAttribute("userId");
                User user = authService.getUserById(userId);
                if (user == null) {
                    session.invalidate();
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print("{\"error\":\"Session user no longer exists.\"}");
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(toJson(user));
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\":\"Failed to load current user.\"}");
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print("{\"error\":\"Unknown auth endpoint.\"}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(req, resp);
        PrintWriter out = resp.getWriter();
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.equals("/signup")) {
                Map<String, String> body = parseJsonToMap(readRequestBody(req));
                User user = authService.signup(body.get("name"), body.get("email"), body.get("password"));
                startSession(req, resp, user);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                out.print(toJson(user));
                return;
            }

            if (pathInfo != null && pathInfo.equals("/login")) {
                Map<String, String> body = parseJsonToMap(readRequestBody(req));
                User user = authService.login(body.get("email"), body.get("password"));
                startSession(req, resp, user);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print(toJson(user));
                return;
            }

            if (pathInfo != null && pathInfo.equals("/logout")) {
                HttpSession session = req.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                clearReadableCookie(resp);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"message\":\"Logged out successfully.\"}");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Unknown auth endpoint.\"}");

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Authentication database error.\"}");
        }
    }

    private void startSession(HttpServletRequest req, HttpServletResponse resp, User user) {
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setMaxInactiveInterval(30 * 60);

        Cookie readableCookie = new Cookie("finance_user", user.getEmail());
        readableCookie.setMaxAge(30 * 60);
        readableCookie.setPath("/");
        resp.addCookie(readableCookie);
    }

    private void clearReadableCookie(HttpServletResponse resp) {
        Cookie cookie = new Cookie("finance_user", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        resp.addCookie(cookie);
    }

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

    private Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }

        json = json.trim().replaceAll("[{}\"]", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length < 2) {
                continue;
            }
            map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    private String toJson(User user) {
        return String.format(
            "{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}",
            user.getId(), escapeJson(user.getName()), escapeJson(user.getEmail())
        );
    }

    private String escapeJson(String val) {
        if (val == null) {
            return "";
        }
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
