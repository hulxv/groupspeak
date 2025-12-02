package com.example.chat.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ConversationManager {

    private static SQLiteDatabase db;

    public static void initialize(SQLiteDatabase database) {
        db = database;
    }

    /**
     * Fetch all conversations for a given user.
     */
    public static List<Conversation> getConversationsForUser(String userId) {
        return Conversation.findConversationsByUserId(userId);
    }

    /**
     * Create a 1-on-1 conversation between two users.
     * If a 1-on-1 conversation already exists between them, return it.
     */
    public static Conversation createOneOnOneConversation(String userId1, String userId2) throws SQLException {
        // Check if users exist
        User user1 = User.findById(userId1);
        User user2 = User.findById(userId2);
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("One or both users do not exist.");
        }

        // Check if a 1-on-1 conversation already exists
        Conversation existing = findExistingOneOnOne(userId1, userId2);
        if (existing != null) {
            return existing;
        }

        // Create new conversation
        String name = user1.getDisplayName() + " & " + user2.getDisplayName();
        Conversation conversation = new Conversation(name, 0); // 0 for not group
        conversation.save();

        // Add participants
        ConversationParticipant p1 = new ConversationParticipant(conversation.getConversationId(), userId1);
        p1.save();
        ConversationParticipant p2 = new ConversationParticipant(conversation.getConversationId(), userId2);
        p2.save();

        return conversation;
    }

    /**
     * Create a group conversation with a name and list of user IDs.
     */
    public static Conversation createGroupConversation(String name, List<String> userIds) throws SQLException {
        if (userIds == null || userIds.size() < 2) {
            throw new IllegalArgumentException("Group conversation must have at least 2 participants.");
        }

        // Check if all users exist
        for (String userId : userIds) {
            if (User.findById(userId) == null) {
                throw new IllegalArgumentException("User does not exist: " + userId);
            }
        }

        // Create conversation
        Conversation conversation = new Conversation(name, 1); // 1 for group
        conversation.save();

        // Add participants
        for (String userId : userIds) {
            ConversationParticipant participant = new ConversationParticipant(conversation.getConversationId(), userId);
            participant.save();
        }

        return conversation;
    }

    /**
     * Add a participant to a group conversation.
     */
    public static void addParticipant(String conversationId, String userId) throws SQLException {
        Conversation conversation = Conversation.findById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation does not exist.");
        }
        if (!conversation.isGroup()) {
            throw new IllegalArgumentException("Cannot add participants to 1-on-1 conversations.");
        }
        if (User.findById(userId) == null) {
            throw new IllegalArgumentException("User does not exist.");
        }
        if (ConversationParticipant.isParticipant(conversationId, userId)) {
            throw new IllegalArgumentException("User is already a participant.");
        }

        ConversationParticipant participant = new ConversationParticipant(conversationId, userId);
        participant.save();
    }

    /**
     * Remove a participant from a group conversation.
     */
    public static void removeParticipant(String conversationId, String userId) throws SQLException {
        Conversation conversation = Conversation.findById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation does not exist.");
        }
        if (!conversation.isGroup()) {
            throw new IllegalArgumentException("Cannot remove participants from 1-on-1 conversations.");
        }
        if (!ConversationParticipant.isParticipant(conversationId, userId)) {
            throw new IllegalArgumentException("User is not a participant.");
        }

        // Check if this is the last participant
        List<ConversationParticipant> participants = ConversationParticipant.findByConversationId(conversationId);
        if (participants.size() <= 1) {
            throw new IllegalArgumentException("Cannot remove the last participant from a group conversation.");
        }

        // Remove participant
        removeParticipantFromDB(conversationId, userId);
    }

    /**
     * Helper method to find existing 1-on-1 conversation between two users.
     */
    private static Conversation findExistingOneOnOne(String userId1, String userId2) {
        if (db == null) return null;

        // Query for conversations where both users are participants and is_group=0
        String sql = "SELECT c.* FROM CONVERSATIONS c " +
                "JOIN CONVERSATION_PARTICIPANTS cp1 ON c.conversation_id = cp1.conversation_id " +
                "JOIN CONVERSATION_PARTICIPANTS cp2 ON c.conversation_id = cp2.conversation_id " +
                "WHERE c.is_group = 0 AND cp1.user_id = ? AND cp2.user_id = ?";

        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId1);
            pstmt.setString(2, userId2);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Conversation(
                        rs.getString("conversation_id"),
                        rs.getString("name"),
                        rs.getInt("is_group"),
                        rs.getString("created_at"));
            }
        } catch (SQLException e) {
            System.err.println("DB Error finding existing 1-on-1 conversation: " + e.getMessage());
        }
        return null;
    }

    /**
     * Helper method to remove a participant from the database.
     */
    private static void removeParticipantFromDB(String conversationId, String userId) throws SQLException {
        if (db == null) throw new IllegalStateException("Database not initialized.");

        String sql = "DELETE FROM CONVERSATION_PARTICIPANTS WHERE conversation_id = ? AND user_id = ?";
        try (Connection conn = db.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, conversationId);
            pstmt.setString(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Participant not found.");
            }
            System.out.println("DB: Removed user " + userId + " from conversation " + conversationId);
        } catch (SQLException e) {
            System.err.println("DB Error removing participant: " + e.getMessage());
            throw e;
        }
    }
}
