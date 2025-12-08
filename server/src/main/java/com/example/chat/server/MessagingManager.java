package com.example.chat.server;

import java.sql.SQLException;
import java.util.*;

/**
 * MessagingManager (MessageRouter) handles sending and routing messages.
 * 
 * Responsibilities:
 * - Save messages to database
 * - Route messages to specific users (1-on-1 chat)
 * - Push messages to multiple recipients (group chats)
 * - Track online/offline clients
 * - Queue offline messages for delivery when user comes online (optional)
 */
public class MessagingManager {

    // Map of userId to connected ClientHandler(s)
    // A user may have multiple connections (different devices)
    private static Map<String, List<ClientHandler>> userConnections = Collections.synchronizedMap(new HashMap<>());

    /**
     * Register a connected client with a userId.
     * A user can have multiple concurrent connections.
     */
    public static void registerClient(String userId, ClientHandler clientHandler) {
        userConnections.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(clientHandler);
        System.out.println("MessagingManager: User " + userId + " connected. Total connections: " + userConnections.get(userId).size());
    }

    /**
     * Unregister a client when it disconnects.
     */
    public static void unregisterClient(String userId, ClientHandler clientHandler) {
        if (userConnections.containsKey(userId)) {
            List<ClientHandler> handlers = userConnections.get(userId);
            handlers.remove(clientHandler);
            if (handlers.isEmpty()) {
                userConnections.remove(userId);
                System.out.println("MessagingManager: User " + userId + " disconnected (all connections closed)");
            } else {
                System.out.println("MessagingManager: User " + userId + " connection closed. Remaining: " 
                    + handlers.size());
            }
        }
    }

    /**
     * Check if a user is currently online (has at least one active connection).
     */
    public static boolean isUserOnline(String userId) {
        return userConnections.containsKey(userId) && !userConnections.get(userId).isEmpty();
    }

    /**
     * Get all currently online users.
     */
    public static List<String> getOnlineUsers() {
        return new ArrayList<>(userConnections.keySet());
    }

    /**
     * Send a message to a specific user in a conversation.
     * 
     * @param conversationId  The conversation ID
     * @param senderId        The sender's user ID
     * @param content         The message content (may be encrypted)
     * @param recipientUserId The recipient's user ID
     * @return true if message was delivered to at least one online client, false otherwise
     */
    public static boolean sendDirectMessage(String conversationId, String senderId, String content, 
                                            String recipientUserId) {
        // Save message to database first
        try {
            Message message = new Message(conversationId, senderId, content);
            message.save();
        } catch (SQLException e) {
            System.err.println("MessagingManager: Error saving message: " + e.getMessage());
            return false;
        }

        // Try to deliver to online recipients
        List<ClientHandler> recipientHandlers = userConnections.get(recipientUserId);
        if (recipientHandlers == null || recipientHandlers.isEmpty()) {
            System.out.println("MessagingManager: User " + recipientUserId 
                + " is offline. Message queued (or will be fetched on next login)");
            return false;
        }

        // Send to all connected clients of the recipient
        String messageJson = buildMessageJson(senderId, content, conversationId);
        boolean deliveredToAny = false;
        for (ClientHandler handler : new ArrayList<>(recipientHandlers)) {
            try {
                handler.sendMessage(messageJson);
                deliveredToAny = true;
            } catch (Exception e) {
                System.err.println("MessagingManager: Failed to deliver to client: " + e.getMessage());
            }
        }

        return deliveredToAny;
    }

    /**
     * Send a message to all participants in a group conversation.
     * Excludes the sender from the recipients.
     * 
     * @param conversationId  The group conversation ID
     * @param senderId        The sender's user ID
     * @param content         The message content
     * @return Number of recipients the message was delivered to
     */
    public static int sendGroupMessage(String conversationId, String senderId, String content) {
        // Save message to database first
        try {
            Message message = new Message(conversationId, senderId, content);
            message.save();
        } catch (SQLException e) {
            System.err.println("MessagingManager: Error saving group message: " + e.getMessage());
            return 0;
        }

        // Get all participants in the conversation
        Conversation conversation = Conversation.findById(conversationId);
        if (conversation == null) {
            System.err.println("MessagingManager: Conversation not found: " + conversationId);
            return 0;
        }

        List<ConversationParticipant> participants = ConversationParticipant.findByConversationId(conversationId);
        if (participants == null || participants.isEmpty()) {
            System.err.println("MessagingManager: No participants found for conversation: " + conversationId);
            return 0;
        }

        // Send to all participants except sender
        String messageJson = buildMessageJson(senderId, content, conversationId);
        int deliveredCount = 0;

        for (ConversationParticipant participant : participants) {
            String recipientId = participant.getUserId();
            
            // Don't send to sender
            if (recipientId.equals(senderId)) {
                continue;
            }

            // Send to all connected clients of this participant
            List<ClientHandler> handlers = userConnections.get(recipientId);
            if (handlers != null && !handlers.isEmpty()) {
                for (ClientHandler handler : new ArrayList<>(handlers)) {
                    try {
                        handler.sendMessage(messageJson);
                        deliveredCount++;
                    } catch (Exception e) {
                        System.err.println("MessagingManager: Failed to deliver to " + recipientId + ": " 
                            + e.getMessage());
                    }
                }
            } else {
                System.out.println("MessagingManager: User " + recipientId + " is offline. Will receive on next login.");
            }
        }

        System.out.println("MessagingManager: Group message sent to " + deliveredCount + " online recipients");
        return deliveredCount;
    }

    /**
     * Build a JSON message frame for transmission.
     */
    public static String buildMessageJson(String senderId, String content, String conversationId) {
        return "{\"type\":\"message\",\"senderId\":\"" + ProtocolParser.escape(senderId) 
            + "\",\"content\":\"" + ProtocolParser.escape(content) 
            + "\",\"conversationId\":\"" + ProtocolParser.escape(conversationId) + "\"}";
    }

    /**
     * Broadcast a user's online status change to all connected clients.
     */
    public static void broadcastUserStatus(String userId, boolean isOnline) {
        String json = "{\"type\":\"status\",\"userId\":\"" + ProtocolParser.escape(userId) + "\",\"isOnline\":" + isOnline + "}";
        
        synchronized(userConnections) {
            for (List<ClientHandler> handlers : userConnections.values()) {
                for (ClientHandler h : handlers) {
                    try {
                        h.sendMessage(json);
                    } catch (Exception e) {
                        System.err.println("MessagingManager: Failed to broadcast status: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Notify participants of a new conversation.
     */
    public static void notifyNewConversation(Conversation conversation, List<String> participantIds) {
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"new_conversation\",");
        json.append("\"id\":\"").append(ProtocolParser.escape(conversation.getConversationId())).append("\",");
        json.append("\"name\":\"").append(ProtocolParser.escape(conversation.getName())).append("\",");
        json.append("\"isGroup\":").append(conversation.isGroup()).append(",");
        
        json.append("\"participants\":[");
        for(int i=0; i<participantIds.size(); i++) {
             json.append("\"").append(ProtocolParser.escape(participantIds.get(i))).append("\"");
             if(i < participantIds.size()-1) json.append(",");
        }
        json.append("]}");
        
        String message = json.toString();

        for (String pid : participantIds) {
            List<ClientHandler> handlers = userConnections.get(pid);
            if (handlers != null) {
                for (ClientHandler h : handlers) {
                    try {
                        h.sendMessage(message);
                    } catch (Exception e) {
                        System.err.println("Failed to notify user " + pid + " of new conversation: " + e.getMessage());
                    }
                }
            }
        }
    }

    public static void notifyReloadConversations(String userId) {
        String message = "{\"type\":\"reload_conversations\",\"userId\":\"" + ProtocolParser.escape(userId) + "\"}";

        // Find the client(s) for the target user and send the notification
        List<ClientHandler> handlers = userConnections.get(userId);
        if (handlers != null && !handlers.isEmpty()) {
            // Iterate over a copy to avoid ConcurrentModificationException
            for (ClientHandler h : new ArrayList<>(handlers)) {
                try {
                    h.sendMessage(message);
                    System.out.println("MessagingManager: Sent reload_conversations notification to " + userId);
                } catch (Exception e) {
                    System.err.println("MessagingManager: Failed to notify user " + userId 
                        + " to reload conversations: " + e.getMessage());
                }
            }
        }
    }
}
