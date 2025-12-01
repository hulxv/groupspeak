package com.example.chat.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class ConversationParticipant {
    private String participantId;
    private String conversationId;
    private String userId;
    private String joinedAt;

    private static SQLiteDatabase db;

    public ConversationParticipant(String conversationId, String userId) {
        this.participantId = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.userId = userId;
        this.joinedAt = LocalDateTime.now().toString();
    }

    private ConversationParticipant(String participantId, String conversationId, String userId, String joinedAt) {
        this.participantId = participantId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.joinedAt = joinedAt;
    }

    public static void initialize(SQLiteDatabase database) {
        db = database;
    }

    public void save() throws SQLException {
        if (db == null)
            throw new IllegalStateException("Database connection not initialized.");

        String sql = "INSERT INTO CONVERSATION_PARTICIPANTS(participant_id, conversation_id, user_id, joined_at) "
                + "VALUES(?, ?, ?, ?)";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.participantId);
            pstmt.setString(2, this.conversationId);
            pstmt.setString(3, this.userId);
            pstmt.setString(4, this.joinedAt);

            pstmt.executeUpdate();
            System.out.println("DB: Added user " + this.userId + " to conversation " + this.conversationId);

        } catch (SQLException e) {
            System.err.println("DB Error saving participant: " + e.getMessage());
            throw e;
        }
    }

    public static boolean isParticipant(String conversationId, String userId) {
        if (db == null)
            return false;
        String sql = "SELECT 1 FROM CONVERSATION_PARTICIPANTS WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, conversationId);
            pstmt.setString(2, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("DB Error checking participant status: " + e.getMessage());
            return false;
        }
    }

    public static List<ConversationParticipant> findByConversationId(String conversationId) {
        if (db == null)
            return new ArrayList<>();

        String sql = "SELECT * FROM CONVERSATION_PARTICIPANTS WHERE conversation_id = ?";
        List<ConversationParticipant> participants = new ArrayList<>();

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, conversationId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                participants.add(new ConversationParticipant(
                        rs.getString("participant_id"),
                        rs.getString("conversation_id"),
                        rs.getString("user_id"),
                        rs.getString("joined_at")));
            }
        } catch (SQLException e) {
            System.err.println("DB Error finding participants by conversation: " + e.getMessage());
        }
        return participants;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getUserId() {
        return userId;
    }
}