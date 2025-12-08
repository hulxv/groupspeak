package org.openjfx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Protocol handler for client-side JSON command building and response parsing.

public class ProtocolHandler {

    // Escape a string for JSON format.
    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // Unescape a JSON string back to normal format.
    private static String unescape(String s) {
        if (s == null)
            return null;
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
    }

    // Extract a string value from a JSON object by key.
    private static String extractJsonString(String json, String key) {
        if (json == null || key == null)
            return null;
        try {
            // Matches "key" : "value" allowing for whitespace and escaped quotes
            Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return unescape(m.group(1));
            }
        } catch (Exception e) {
            System.out.println("Error extracting string for key " + key + ": " + e);
        }
        return null;
    }

    // Extract a boolean value from a JSON object by key.
    private static boolean extractJsonBoolean(String json, String key) {
        if (json == null || key == null)
            return false;
        try {
            // Matches "key" : true or "key" : false
            Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(1));
            }
        } catch (Exception e) {
            System.out.println("Error extracting boolean for key " + key + ": " + e);
        }
        return false;
    }

    // Request builders
    public static String buildRegisterRequest(String username, String password, String displayName, String email) {
        String json = "{\"type\":\"register\",\"username\":\"" + escape(username) + "\",\"password\":\""
                + escape(password) + "\",\"displayName\":\"" + escape(displayName) + "\"";
        if (email != null) {
            json += ",\"email\":\"" + escape(email) + "\"";
        }
        json += "}";
        return json;
    }

    public static String buildLoginRequest(String username, String password, String device) {
        String json = "{\"type\":\"login\",\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(password)
                + "\"";
        if (device != null) {
            json += ",\"device\":\"" + escape(device) + "\"";
        }
        json += "}";
        return json;
    }

    public static String buildLogoutRequest(String username) {
        return "{\"type\":\"logout\",\"username\":\"" + escape(username) + "\"}";
    }

    public static String buildGetUsersRequest() {
        return "{\"type\":\"get_users\"}";
    }

    public static String buildGetMessagesRequest(String conversationId) {
        return "{\"type\":\"get_messages\",\"conversationId\":\"" + escape(conversationId) + "\"}";
    }

    public static String buildGetConversationsRequest() {
        return "{\"type\":\"get_conversations\"}";
    }

    public static String buildCreateConversationRequest(String otherUsername, String name, String participants) {
        String json = "{\"type\":\"create_conversation\"";
        if (otherUsername != null) {
            json += ",\"otherUsername\":\"" + escape(otherUsername) + "\"";
        } else if (name != null && participants != null) {
            json += ",\"name\":\"" + escape(name) + "\",\"participants\":\"" + escape(participants) + "\"";
        }
        json += "}";
        return json;
    }

    public static String buildAddParticipantRequest(String conversationId, String userId) {
        return "{\"type\":\"add_participant\",\"conversationId\":\"" + escape(conversationId)
                + "\",\"userId\":\"" + escape(userId) + "\"}";
    }

    public static String buildRemoveParticipantRequest(String conversationId, String userId) {
        return "{\"type\":\"remove_participant\",\"conversationId\":\"" + escape(conversationId)
                + "\",\"userId\":\"" + escape(userId) + "\"}";
    }

    public static String buildSendDmRequest(String conversationId, String senderId, String content,
            String recipientId) {
        return "{\"type\":\"send_dm\",\"conversationId\":\"" + escape(conversationId) + "\",\"senderId\":\""
                + escape(senderId) + "\",\"content\":\"" + escape(content) + "\",\"recipientId\":\""
                + escape(recipientId) + "\"}";
    }

    public static String buildSendGroupRequest(String conversationId, String senderId, String content) {
        return "{\"type\":\"send_group\",\"conversationId\":\"" + escape(conversationId) + "\",\"senderId\":\""
                + escape(senderId) + "\",\"content\":\"" + escape(content) + "\"}";
    }

    public static String buildReloadConversationsRequest(String userId) {
        return "{\"type\":\"reload_conversations\",\"userId\":\"" + escape(userId) + "\"}";
    }

    public static String buildPingRequest() {
        return "{\"type\":\"7ekey\"}";
    }
    // Response parsers
    public static class Response {
        public boolean success;
        public String message;
        public String code;
    }

    public static class RegisterResponse extends Response {
        public String userId;
    }

    public static RegisterResponse parseRegisterResponse(String json) {
        RegisterResponse resp = new RegisterResponse();
        resp.success = extractJsonBoolean(json, "success");
        resp.userId = extractJsonString(json, "userId");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static class LoginResponse extends Response {
        public String userId;
        public String sessionToken;
        public String displayName;
        public String email;
    }

    public static LoginResponse parseLoginResponse(String json) {
        LoginResponse resp = new LoginResponse();
        resp.success = extractJsonBoolean(json, "success");
        resp.userId = extractJsonString(json, "userId");
        resp.sessionToken = extractJsonString(json, "sessionToken");
        resp.message = extractJsonString(json, "message");
        resp.displayName = extractJsonString(json, "displayName");
        resp.email = extractJsonString(json, "email");
        return resp;
    }

    public static Response parseLogoutResponse(String json) {
        Response resp = new Response();
        resp.success = extractJsonBoolean(json, "success");
        return resp;
    }

    public static class UsersResponse extends Response {
        public String users; // JSON array as string
    }

    public static UsersResponse parseUsersResponse(String json) {
        UsersResponse resp = new UsersResponse();
        resp.success = extractJsonBoolean(json, "success");
        try {
            // Non-greedy match for array content
            Pattern p = Pattern.compile("\"users\"\\s*:\\s*(\\[.*\\])");
            Matcher m = p.matcher(json);
            if (m.find()) {
                resp.users = m.group(1);
            }
        } catch (Exception e) {
             System.out.println("Error extracting users array: " + e);
        }
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static class MessagesResponse extends Response {
        public String messages; // JSON array string
    }

    public static MessagesResponse parseMessagesResponse(String json) {
        MessagesResponse resp = new MessagesResponse();
        resp.success = extractJsonBoolean(json, "success");
        try {
            Pattern p = Pattern.compile("\"messages\"\\s*:\\s*(\\[.*\\])");
            Matcher m = p.matcher(json);
            if (m.find()) {
                resp.messages = m.group(1);
            }
        } catch (Exception e) {
             System.out.println("Error extracting messages array: " + e);
        }
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static class ConversationsResponse extends Response {
        public String conversations; // JSON array as string, parse separately if needed
    }

    public static ConversationsResponse parseConversationsResponse(String json) {
        ConversationsResponse resp = new ConversationsResponse();
        resp.success = extractJsonBoolean(json, "success");
        // Extract conversations array - looks for "conversations": [...]
        try {
            // Non-greedy match for array content
            Pattern p = Pattern.compile("\"conversations\"\\s*:\\s*(\\[.*\\])");
            Matcher m = p.matcher(json);
            if (m.find()) {
                resp.conversations = m.group(1);
            }
        } catch (Exception e) {
             System.out.println("Error extracting conversations array: " + e);
        }
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static class CreateConversationResponse extends Response {
        public String conversationId;
    }

    public static CreateConversationResponse parseCreateConversationResponse(String json) {
        CreateConversationResponse resp = new CreateConversationResponse();
        resp.success = extractJsonBoolean(json, "success");
        resp.conversationId = extractJsonString(json, "conversationId");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parseAddParticipantResponse(String json) {
        Response resp = new Response();
        resp.success = extractJsonBoolean(json, "success");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parseRemoveParticipantResponse(String json) {
        Response resp = new Response();
        resp.success = extractJsonBoolean(json, "success");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parseMessageResponse(String json) {
        Response resp = new Response();
        resp.success = extractJsonBoolean(json, "success");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parsePingResponse(String json) {
        Response resp = new Response();
        resp.success = json != null && json.contains("\"type\":\"mekey\"");
        return resp;
    }

    public static Response parseErrorResponse(String json) {
        Response resp = new Response();
        resp.success = false;
        resp.code = extractJsonString(json, "code");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static class StatusUpdate {
        public String userId;
        public boolean isOnline;
    }

    public static StatusUpdate parseStatusUpdate(String json) {
        StatusUpdate status = new StatusUpdate();
        status.userId = extractJsonString(json, "userId");
        status.isOnline = extractJsonBoolean(json, "isOnline");
        return status;
    }
    
    public static class MessageEvent {
        public String senderId;
        public String content;
        public String conversationId;
    }
    
    public static MessageEvent parseMessageEvent(String json) {
        MessageEvent evt = new MessageEvent();
        evt.senderId = extractJsonString(json, "senderId");
        evt.content = extractJsonString(json, "content");
        evt.conversationId = extractJsonString(json, "conversationId");
        return evt;
    }

    public static class NewConversationEvent {
        public String id;
        public String name;
        public boolean isGroup;
        public java.util.List<String> participantIds;
    }

    public static NewConversationEvent parseNewConversationEvent(String json) {
        NewConversationEvent evt = new NewConversationEvent();
        evt.id = extractJsonString(json, "id");
        evt.name = extractJsonString(json, "name");
        evt.isGroup = extractJsonBoolean(json, "isGroup");
        
        evt.participantIds = new java.util.ArrayList<>();
        try {
            Pattern p = Pattern.compile("\"participants\"\\s*:\\s*\\[(.*?)\\]");
            Matcher m = p.matcher(json);
            if (m.find()) {
                String[] ids = m.group(1).split(",");
                for (String pid : ids) {
                    String cleanId = pid.trim().replace("\"", "");
                    if (!cleanId.isEmpty()) {
                        evt.participantIds.add(cleanId);
                    }
                }
            }
        } catch (Exception e) {}
        
        return evt;
    }
}
