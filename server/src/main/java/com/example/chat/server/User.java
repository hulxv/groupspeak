package com.example.chat.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User {
    private String userId;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private int isOnline;
    private String lastSeen;
    private String createdAt;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static SQLiteDatabase db;

    public User(String userId, String username, String email, String passwordHash, String displayName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = LocalDateTime.now().format(FORMATTER);
        this.isOnline = 0;
        this.lastSeen = this.createdAt;
    }

    private User(String userId, String username, String email, String passwordHash, String displayName,
            String avatarUrl, int isOnline, String lastSeen, String createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.isOnline = isOnline;
        this.lastSeen = lastSeen;
        this.createdAt = createdAt;
    }

    public static void initialize(SQLiteDatabase database) {
        db = database;
    }

    public static User findById(String userId) throws SQLException {
        if (db == null)
            return null;
        String sql = "SELECT * FROM USERS WHERE user_id = ?";
        User user = null;

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("avatar_url"),
                        rs.getInt("is_online"),
                        rs.getString("last_seen"),
                        rs.getString("created_at"));
            }
        } catch (SQLException e) {
            System.err.println("DB Error finding user by ID: " + e.getMessage());
            throw e;
        }
        return user;
    }

    public static User findByUsername(String username) {
        if (db == null)
            return null;
        String sql = "SELECT * FROM USERS WHERE username = ?";
        User user = null;

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("avatar_url"),
                        rs.getInt("is_online"),
                        rs.getString("last_seen"),
                        rs.getString("created_at"));
            }
        } catch (SQLException e) {
            System.err.println("DB Error finding user by username: " + e.getMessage());
        }
        return user;
    }

    public static boolean userExists(String username) {
        if (db == null)
            return false;
        String sql = "SELECT 1 FROM USERS WHERE username = ?";
        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("DB Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    public static void updateOnlineStatus(String userId, boolean isOnline) throws SQLException {
        if (db == null)
            throw new IllegalStateException("Database connection not initialized.");

        String sql = "UPDATE USERS SET is_online = ? WHERE user_id = ?";
        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, isOnline ? 1 : 0);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();

            System.out.println("DB: Updated user " + userId + " online status to: " + (isOnline ? "Online" : "Offline"));
        } catch (SQLException e) {
            System.err.println("DB Error updating user online status: " + e.getMessage());
            throw e;
        }
    }

    public void save() throws SQLException {
        if (db == null)
            throw new IllegalStateException("Database connection not initialized.");

        String sql = "INSERT INTO USERS(user_id, username, email, password_hash, display_name, created_at, is_online, last_seen) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.userId);
            pstmt.setString(2, this.username);
            pstmt.setString(3, this.email);
            pstmt.setString(4, this.passwordHash);
            pstmt.setString(5, this.displayName);
            pstmt.setString(6, this.createdAt);
            pstmt.setInt(7, this.isOnline);
            pstmt.setString(8, this.lastSeen);

            pstmt.executeUpdate();
            System.out.println("DB: Saved new user: " + this.username);

        } catch (SQLException e) {
            System.err.println("DB Error saving new user: " + e.getMessage());
            throw e;
        }
    }

    // Updates a single allowed timestamp column (e.g., last_seen) to the current
    // time
    public void updateTimestamp(String column) throws SQLException {
        if (db == null)
            return;

        String now = LocalDateTime.now().format(FORMATTER);
        String sql = "UPDATE USERS SET " + column + " = ? WHERE user_id = ?";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, now);
            pstmt.setString(2, this.userId);

            pstmt.executeUpdate();

            if ("last_seen".equals(column)) {
                this.lastSeen = now;
            }
        } catch (SQLException e) {
            System.err.println("DB Error updating user timestamp: " + e.getMessage());
            throw e;
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int isOnline() {
        return isOnline;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}