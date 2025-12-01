package com.example.chat.server;

import java.net.Socket;
import java.sql.SQLException;
import java.util.List;


public class ClientHandler implements Runnable {

    private java.net.Socket socket;
    private Framing framing;
    private volatile boolean running = true;

    private String userId = null;
    private String sessionToken = null;

    public ClientHandler(java.net.Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            framing = new Framing(socket.getInputStream(), socket.getOutputStream());

            String frame;
            while(running && (frame = framing.readFrame()) != null) {
                if(frame.isEmpty()) {
                    System.out.println("3omda");
                    continue;
                }

                String type = ProtocolParser.extractJsonString(frame, "type");
                if(type == null) {
                    ProtocolParser.sendError("invalid_protocol", "Missing 'type' field", framing);
                    continue;
                }

                switch(type) {
                    case "login":
                        handleLogin(frame);
                        break;

                    case "logout":
                        handleLogout(frame);
                        break;

                    case "7ekey":
                        ProtocolParser.sendRaw("{\"type\":\"mekey\"}", framing);
                        break;

                    case "register":
                        handleRegister(frame);
                        break;

                    case "get_conversations":
                        handleGetConversations(frame);
                        break;

                    case "create_conversation":
                        handleCreateConversation(frame);
                        break;

                    case "add_participant":
                        handleAddParticipant(frame);
                        break;

                    case "remove_participant":
                        handleRemoveParticipant(frame);
                        break;

                    case "send_dm":
                        handleSendDmMessage(frame);
                        break;

                    case "send_group":
                        handleSendGroupMessage(frame);
                        break;

                    case "exit":
                        handleExit(frame);
                        break;

                    default:
                        ProtocolParser.sendError("unknown_command", "Unknown command type: " + type, framing);
                        break;
                }
            }
            running = false;
            Server.removeClient(this);
        }
        catch(Exception e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        }
        finally {
            cleanup();
        }
        
    }

    private void handleLogin(String frame) {
        String username = ProtocolParser.extractJsonString(frame, "username");
        String password = ProtocolParser.extractJsonString(frame, "password");
        String device = ProtocolParser.extractJsonString(frame, "device");

        if(username == null || password == null) {
            ProtocolParser.sendError("invalid_args", "'username' and 'password' required", framing);
            return;
        }

        AuthManager auth = new AuthManager();
        try {
            AuthManager.AuthResult res = auth.authenticate(username, password, device);
            if(res.success) {
                this.userId = res.userId;
                this.sessionToken = res.sessionToken;
                
                // Register this client connection with the messaging manager
                MessagingManager.registerClient(res.userId, this);
                
                ProtocolParser.sendRaw("{\"type\":\"login_response\", \"success\":true, \"userId\":\"" + ProtocolParser.escape(res.userId)
                        + "\",\"sessionToken\":\"" + ProtocolParser.escape(res.sessionToken) + "\"}", framing);
            }

            else {
                ProtocolParser.sendRaw("{\"type\":\"login_response\",\"success\":false,\"message\":\"" + ProtocolParser.escape(res.message) + "\"}", framing);
            }
        }
        catch (java.sql.SQLException e) {
            ProtocolParser.sendError("server_error", "Authentication failed: " + e.getMessage(), framing);
        }
    }

    private void handleLogout(String frame) {
        String username = ProtocolParser.extractJsonString(frame, "username");
        if(username == null && this.sessionToken == null) {
            ProtocolParser.sendError("invalid_args", "username required", framing);
            return;
        }

        try {
            this.sessionToken = UserSession.findTokenByUsername(username).getSessionToken();
        } catch (SQLException e) {

        }
        // String toEnd = username != null ? username : this.sessionToken;
        SessionManager sm = new SessionManager();
        boolean ok = sm.endSessionByUsername(username);
        ProtocolParser.sendRaw("{\"type\":\"logout_response\",\"success\":" + ok + "}", framing);
        running = false;
    }

    private void handleRegister(String frame) {
        String username = ProtocolParser.extractJsonString(frame, "username");
        String password = ProtocolParser.extractJsonString(frame, "password");
        String displayName = ProtocolParser.extractJsonString(frame, "displayName");
        String email = ProtocolParser.extractJsonString(frame, "email");

        if(username == null || password == null || displayName == null) {
            ProtocolParser.sendError("invalid_args", "'username', 'password', and 'displayName' required", framing);
            return;
        }

        AuthManager auth = new AuthManager();
        AuthManager.AuthResult res = auth.registerUser(username, password, displayName, email != null ? email : "");

        if(res.success) {
            ProtocolParser.sendRaw("{\"type\":\"register_response\",\"success\":true,\"userId\":\"" + ProtocolParser.escape(res.userId) + "\"}", framing);
        } else {
            ProtocolParser.sendRaw("{\"type\":\"register_response\",\"success\":false,\"message\":\"" + ProtocolParser.escape(res.message) + "\"}", framing);
        }
    }

    private void handleGetConversations(String frame) {
        if(userId == null) {
            ProtocolParser.sendError("not_authenticated", "Must be logged in to get conversations", framing);
            return;
        }

        try {
            List<Conversation> conversations = ConversationManager.getConversationsForUser(userId);
            StringBuilder json = new StringBuilder("{\"type\":\"conversations_response\",\"conversations\":[");
            for(int i = 0; i < conversations.size(); i++) {
                Conversation c = conversations.get(i);
                json.append("{\"id\":\"").append(ProtocolParser.escape(c.getConversationId()))
                    .append("\",\"name\":\"").append(ProtocolParser.escape(c.getName()))
                    .append("\",\"isGroup\":").append(c.isGroup());
                if(i < conversations.size() - 1) json.append(",");
            }
            json.append("]}");
            ProtocolParser.sendRaw(json.toString(), framing);
        } catch (Exception e) {
            ProtocolParser.sendError("server_error", "Failed to get conversations: " + e.getMessage(), framing);
        }
    }

    private void handleCreateConversation(String frame) {
        if(userId == null) {
            ProtocolParser.sendError("not_authenticated", "Must be logged in to create conversation", framing);
            return;
        }

        String otherUsername = ProtocolParser.extractJsonString(frame, "otherUsername");
        String name = ProtocolParser.extractJsonString(frame, "name");
        String participantsJson = ProtocolParser.extractJsonString(frame, "participants");

        try {
            Conversation conversation;
            if(otherUsername != null) {
                // 1-on-1 conversation - find user by username
                User otherUser = User.findByUsername(otherUsername);
                if(otherUser == null) {
                    ProtocolParser.sendError("invalid_args", "User not found: " + otherUsername, framing);
                    return;
                }
                conversation = ConversationManager.createOneOnOneConversation(userId, otherUser.getUserId());
            } else if(name != null && participantsJson != null) {
                // Group conversation - assume participants is comma-separated usernames
                String[] parts = participantsJson.split(",");
                List<String> participants = new java.util.ArrayList<>();
                for(String p : parts) {
                    User u = User.findByUsername(p.trim());
                    if(u == null) {
                        ProtocolParser.sendError("invalid_args", "User not found: " + p.trim(), framing);
                        return;
                    }
                    participants.add(u.getUserId());
                }
                participants.add(0, userId); // Add creator
                conversation = ConversationManager.createGroupConversation(name, participants);
            } else {
                ProtocolParser.sendError("invalid_args", "Provide 'otherUsername' for 1-on-1 or 'name' and 'participants' for group", framing);
                return;
            }

            ProtocolParser.sendRaw("{\"type\":\"create_conversation_response\",\"success\":true,\"conversationId\":\""
                + ProtocolParser.escape(conversation.getConversationId()) + "\"}", framing);
        } catch (Exception e) {
            ProtocolParser.sendError("server_error", "Failed to create conversation: " + e.getMessage(), framing);
        }
    }

    private void handleAddParticipant(String frame) {
        if(userId == null) {
            ProtocolParser.sendError("not_authenticated", "Must be logged in to add participant", framing);
            return;
        }

        String conversationId = ProtocolParser.extractJsonString(frame, "conversationId");
        String userId = ProtocolParser.extractJsonString(frame, "userId");

        if(conversationId == null || userId == null) {
            ProtocolParser.sendError("invalid_args", "'conversationId' and 'participantId' required", framing);
            return;
        }

        try {
            ConversationManager.addParticipant(conversationId, userId);
            ProtocolParser.sendRaw("{\"type\":\"add_participant_response\",\"success\":true}", framing);
        } catch (Exception e) {
            ProtocolParser.sendError("server_error", "Failed to add participant: " + e.getMessage(), framing);
        }
    }

    private void handleRemoveParticipant(String frame) {
        if(userId == null) {
            ProtocolParser.sendError("not_authenticated", "Must be logged in to remove participant", framing);
            return;
        }

        String conversationId = ProtocolParser.extractJsonString(frame, "conversationId");
        String userId = ProtocolParser.extractJsonString(frame, "userId");

        if(conversationId == null || userId == null) {
            ProtocolParser.sendError("invalid_args", "'conversationId' and 'userId' required", framing);
            return;
        }

        try {
            ConversationManager.removeParticipant(conversationId, userId);
            ProtocolParser.sendRaw("{\"type\":\"remove_participant_response\",\"success\":true}", framing);
        } catch (Exception e) {
            ProtocolParser.sendError("server_error", "Failed to remove participant: " + e.getMessage(), framing);
        }
    }

    private void handleSendDmMessage(String frame) {
        String conversationId = ProtocolParser.extractJsonString(frame, "conversationId");
        String senderId = ProtocolParser.extractJsonString(frame, "senderId");
        String content = ProtocolParser.extractJsonString(frame, "content");
        String recipientUserId = ProtocolParser.extractJsonString(frame, "recipientId");

        if(conversationId == null || senderId == null || content == null || recipientUserId == null) {
            ProtocolParser.sendError("invalid_args", "'conversationId', 'senderId', 'content' and 'recipientUserId' required", framing);
            return;
        }

        boolean ok = MessagingManager.sendDirectMessage(conversationId, senderId, content, recipientUserId);

        if(ok) {
            ProtocolParser.sendRaw(MessagingManager.buildMessageJson(senderId, content, conversationId), framing);
        } else {
            ProtocolParser.sendRaw("{\"type\":\"message_response\",\"success\":failed,\"message\":\"" + ProtocolParser.escape(content) + "\"}", framing);
        }
    }

    private void handleSendGroupMessage(String frame) {
        String conversationId = ProtocolParser.extractJsonString(frame, "conversationId");
        String senderId = ProtocolParser.extractJsonString(frame, "senderId");
        String content = ProtocolParser.extractJsonString(frame, "content");

        if(conversationId == null || senderId == null || content == null) {
            ProtocolParser.sendError("invalid_args", "'conversationId', 'senderId', and 'content' required", framing);
            return;
        }

        int deliveredCount = MessagingManager.sendGroupMessage(conversationId, senderId, content);

        if(deliveredCount != 0) {
            ProtocolParser.sendRaw(MessagingManager.buildMessageJson(senderId, content, conversationId), framing);
        } else {
            ProtocolParser.sendRaw("{\"type\":\"message_response\",\"success\":failed,\"message\":\"" + ProtocolParser.escape(content) + "\"}", framing);
        }
    }

    private void handleExit(String frame) {
        Server.removeClient(this);
        ProtocolParser.sendRaw("{\"type\":\"exit_response\",\"success\":true}", framing);
        running = false;
    }

    private void cleanup() {
        try {
            if(sessionToken != null) {
                new SessionManager().endSession(sessionToken);
            }
            
            // Unregister from messaging manager
            if(userId != null) {
                MessagingManager.unregisterClient(userId, this);
            }
        }
        catch (Exception e) {

        }

        try {
            if(framing != null) framing.close();
        } catch (Exception e) {}

        try {
            if(socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {}
    }

    /**
     * Send a message to this client.
     * Called by MessagingManager when routing messages to recipients.
     */
    public synchronized void sendMessage(String jsonMessage) throws Exception {
        if(framing == null || !running) {
            throw new Exception("Client not connected");
        }
        framing.writeFrame(jsonMessage);
    }

    public Framing getFraming() {
        return this.framing;
    }

    public Socket getSocket() {
        return this.socket;
    }
}