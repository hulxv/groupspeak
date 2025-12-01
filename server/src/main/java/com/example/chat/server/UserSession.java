package com.example.chat.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class UserSession {
    private String sessionId;
    private String userId;
    private String sessionToken;
    private String deviceInfo;
    private String createdAt;
    private String expiresAt;
    private String lastActivity;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static SQLiteDatabase db;

    public UserSession() {}

    public UserSession(String userId, String sessionToken) {
        this.sessionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.createdAt = LocalDateTime.now().format(FORMATTER);
        this.expiresAt = LocalDateTime.now().plusDays(1).format(FORMATTER);
        this.lastActivity = this.createdAt;
        this.deviceInfo = "Unknown";
    }

    private UserSession(String sessionId, String userId, String sessionToken, String deviceInfo,
            String createdAt, String expiresAt, String lastActivity) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.deviceInfo = deviceInfo;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastActivity = lastActivity;
    }

    public static void initialize(SQLiteDatabase database) {
        db = database;
    }

    public static UserSession findByToken(String sessionToken) throws SQLException {
        if (db == null)
            return null;
        String sql = "SELECT * FROM USER_SESSIONS WHERE session_token = ?";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sessionToken);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new UserSession(
                        rs.getString("session_id"),
                        rs.getString("user_id"),
                        rs.getString("session_token"),
                        rs.getString("device_info"),
                        rs.getString("created_at"),
                        rs.getString("expires_at"),
                        rs.getString("last_activity"));
            }
            return null;
        } catch (SQLException e) {
            System.err.println("DB Error finding session by token: " + e.getMessage());
            throw e;
        }
    }

<<<<<<< HEAD
=======
    public static UserSession findTokenByUsername(String username) throws SQLException {
        if (db == null)
            return null;
        String sql = "SELECT USER_SESSIONS.* FROM USERS JOIN USER_SESSIONS ON USER_SESSIONS.user_id = USERS.user_id WHERE username = ?";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new UserSession(
                        rs.getString("session_id"),
                        rs.getString("user_id"),
                        rs.getString("session_token"),
                        rs.getString("device_info"),
                        rs.getString("created_at"),
                        rs.getString("expires_at"),
                        rs.getString("last_activity"));
            }
            return null;
        } catch (SQLException e) {
            System.err.println("DB Error finding session by token: " + e.getMessage());
            throw e;
        }
    }

>>>>>>> 75e807f967c1fbdffe781ffec99d1eb2c60dc43b
    public void save() throws SQLException {
        if (db == null) {
            System.err.println("DB Error: Database connection not initialized for UserSession.");
            return;
        }

        String sql = "INSERT INTO USER_SESSIONS(session_id, user_id, session_token, device_info, created_at, expires_at, last_activity) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.sessionId);
            pstmt.setString(2, this.userId);
            pstmt.setString(3, this.sessionToken);
            pstmt.setString(4, this.deviceInfo);
            pstmt.setString(5, this.createdAt);
            pstmt.setString(6, this.expiresAt);
            pstmt.setString(7, this.lastActivity);

            pstmt.executeUpdate();
            System.out.println("DB: Saved new session for user ID: " + this.userId);
        } catch (SQLException e) {
            System.err.println("DB Error saving session: " + e.getMessage());
            throw e;
        }
    }

    public void delete() throws SQLException {
        if (db == null)
            throw new IllegalStateException("Database connection not initialized.");

        String sql = "DELETE FROM USER_SESSIONS WHERE session_token = ?";
        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.sessionToken);
            pstmt.executeUpdate();
            System.out.println("DB: Deleted session: " + this.sessionToken);
        } catch (SQLException e) {
            System.err.println("DB Error deleting session: " + e.getMessage());
            throw e;
        }
    }

    // Updates a single allowed timestamp column (e.g., last_activity) to the
    // current time
    public void updateTimestamp(String column) throws SQLException {
        if (db == null)
            return;

        String now = LocalDateTime.now().format(FORMATTER);
        String sql = "UPDATE USER_SESSIONS SET " + column + " = ? WHERE session_token = ?";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, now);
            pstmt.setString(2, this.sessionToken);

            pstmt.executeUpdate();

            if ("last_activity".equals(column)) {
                this.lastActivity = now;
            } else if ("expires_at".equals(column)) {
                this.expiresAt = now;
            }
        } catch (SQLException e) {
            System.err.println("DB Error updating session timestamp: " + e.getMessage());
            throw e;
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public static DateTimeFormatter getFormatter() {
        return FORMATTER;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }
}