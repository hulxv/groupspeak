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
            Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\\"(.*?)\\\"");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return unescape(m.group(1));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
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

    public static String buildAddParticipantRequest(String conversationId, String participantId) {
        return "{\"type\":\"add_participant\",\"conversationId\":\"" + escape(conversationId)
                + "\",\"participantId\":\"" + escape(participantId) + "\"}";
    }

    public static String buildRemoveParticipantRequest(String conversationId, String participantId) {
        return "{\"type\":\"remove_participant\",\"conversationId\":\"" + escape(conversationId)
                + "\",\"participantId\":\"" + escape(participantId) + "\"}";
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
        resp.success = "true".equals(extractJsonString(json, "success"));
        resp.userId = extractJsonString(json, "userId");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static class LoginResponse extends Response {
        public String userId;
        public String sessionToken;
    }

    public static LoginResponse parseLoginResponse(String json) {
        LoginResponse resp = new LoginResponse();
        resp.success = "true".equals(extractJsonString(json, "success"));
        resp.userId = extractJsonString(json, "userId");
        resp.sessionToken = extractJsonString(json, "sessionToken");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parseLogoutResponse(String json) {
        Response resp = new Response();
        resp.success = "true".equals(extractJsonString(json, "success"));
        return resp;
    }

    public static class ConversationsResponse extends Response {
        public String conversations; // JSON array as string, parse separately if needed
    }

    public static ConversationsResponse parseConversationsResponse(String json) {
        ConversationsResponse resp = new ConversationsResponse();
        resp.success = "true".equals(extractJsonString(json, "success"));
        // Extract conversations array - simplistic, assumes no nested quotes
        try {
            Pattern p = Pattern.compile("\"conversations\"\\s*:\\s*(\\[.*?\\])");
            Matcher m = p.matcher(json);
            if (m.find()) {
                resp.conversations = m.group(1);
            }
        } catch (Exception e) {
        }
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static class CreateConversationResponse extends Response {
        public String conversationId;
    }

    public static CreateConversationResponse parseCreateConversationResponse(String json) {
        CreateConversationResponse resp = new CreateConversationResponse();
        resp.success = "true".equals(extractJsonString(json, "success"));
        resp.conversationId = extractJsonString(json, "conversationId");
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parseAddParticipantResponse(String json) {
        Response resp = new Response();
        resp.success = "true".equals(extractJsonString(json, "success"));
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parseRemoveParticipantResponse(String json) {
        Response resp = new Response();
        resp.success = "true".equals(extractJsonString(json, "success"));
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parseMessageResponse(String json) {
        Response resp = new Response();
        resp.success = "true".equals(extractJsonString(json, "success"));
        resp.message = extractJsonString(json, "message");
        return resp;
    }

    public static Response parsePingResponse(String json) {
        Response resp = new Response();
        resp.success = json.contains("\"type\":\"mekey\"");
        return resp;
    }

    public static Response parseErrorResponse(String json) {
        Response resp = new Response();
        resp.success = false;
        resp.code = extractJsonString(json, "code");
        resp.message = extractJsonString(json, "message");
        return resp;
    }
}
