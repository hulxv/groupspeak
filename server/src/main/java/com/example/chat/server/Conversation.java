package com.example.chat.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class Conversation {
    private String conversationId;
    private String name;
    private int isGroup; // 1=true, 0=false
    private String createdAt;

    private static SQLiteDatabase db;

    public Conversation(String name, int isGroup) {
        this.conversationId = UUID.randomUUID().toString();
        this.name = name;
        this.isGroup = isGroup;
        this.createdAt = LocalDateTime.now().toString();
    }

    Conversation(String conversationId, String name, int isGroup, String createdAt) {
        this.conversationId = conversationId;
        this.name = name;
        this.isGroup = isGroup;
        this.createdAt = createdAt;
    }

    public static void initialize(SQLiteDatabase database) {
        db = database;
    }

    public void save() throws SQLException {
        if (db == null)
            throw new IllegalStateException("Database connection not initialized.");

        String sql = "INSERT INTO CONVERSATIONS(conversation_id, name, is_group, created_at) "
                + "VALUES(?, ?, ?, ?)";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.conversationId);
            pstmt.setString(2, this.name);
            pstmt.setInt(3, this.isGroup);
            pstmt.setString(4, this.createdAt);

            pstmt.executeUpdate();
            System.out.println("DB: Saved new conversation ID: " + this.conversationId);

        } catch (SQLException e) {
            System.err.println("DB Error saving conversation: " + e.getMessage());
            throw e;
        }
    }

    public static Conversation findById(String conversationId) {
        if (db == null)
            return null;
        String sql = "SELECT * FROM CONVERSATIONS WHERE conversation_id = ?";
        Conversation conversation = null;

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, conversationId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                conversation = new Conversation(
                        rs.getString("conversation_id"),
                        rs.getString("name"),
                        rs.getInt("is_group"),
                        rs.getString("created_at"));
            }
        } catch (SQLException e) {
            System.err.println("DB Error finding conversation by ID: " + e.getMessage());
        }
        return conversation;
    }

    public static List<Conversation> findConversationsByUserId(String userId) {
        if (db == null)
            return new ArrayList<>();

        // SQL JOIN to link USERS -> CONVERSATION_PARTICIPANTS -> CONVERSATIONS
        String sql = "SELECT c.* FROM CONVERSATIONS c " +
                "JOIN CONVERSATION_PARTICIPANTS cp ON c.conversation_id = cp.conversation_id " +
                "WHERE cp.user_id = ?";

        List<Conversation> conversations = new ArrayList<>();

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                conversations.add(new Conversation(
                        rs.getString("conversation_id"),
                        rs.getString("name"),
                        rs.getInt("is_group"),
                        rs.getString("created_at")));
            }
        } catch (SQLException e) {
            System.err.println("DB Error finding conversations by user ID: " + e.getMessage());
        }
        return conversations;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getName() {
        return name;
    }

    public boolean isGroup() {
        return isGroup == 1;
    }
}