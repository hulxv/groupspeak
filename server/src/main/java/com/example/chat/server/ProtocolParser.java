package com.example.chat.server;

/**
 * Protocol parser for newline-delimited JSON commands.
 * 
 * Provides utilities for:
 * - Extracting JSON field values
 * - Sending error and raw JSON responses
 * - Escaping/unescaping JSON strings
 */
public class ProtocolParser {

    /**
     * Send an error response with error code and message.
     */
    static void sendError(String code, String message, Framing framing) {
        sendRaw("{\"type\":\"error\",\"code\":\"" + escape(code) + "\",\"message\":\""
                + escape(message) + "\"}", framing);
    }

    /**
     * Send an error response using PrintWriter (legacy support).
     */
    static void sendError(String code, String message, java.io.PrintWriter out) {
        sendRaw("{\"type\":\"error\",\"code\":\"" + escape(code) + "\",\"message\":\""
                + escape(message) + "\"}", out);
    }

    /**
     * Send raw JSON frame through Framing handler.
     */
    static synchronized void sendRaw(String json, Framing framing) {
        if (framing != null) {
            framing.writeFrame(json);
        }
    }

    /**
     * Send raw JSON frame using PrintWriter (legacy support).
     */
    static synchronized void sendRaw(String json, java.io.PrintWriter out) {
        if (out != null) {
            out.println(json);
            out.flush();
        }
    }

    /**
     * Extract a string value from a JSON object by key.
     * Uses regex pattern matching for simple JSON parsing.
     * 
     * @param json JSON string to parse
     * @param key  Field name to extract
     * @return The unescaped string value, or null if not found
     */
    static String extractJsonString(String json, String key) {
        if (json == null || key == null) return null;
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\\\"(.*?)\\\"");
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return unescape(m.group(1));
            }
        } catch (Exception e) {
            // ignore parse errors
        }
        return null;
    }

    /**
     * Escape a string for JSON format.
     * Handles backslashes, quotes, and newlines.
     */
    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * Unescape a JSON string back to normal format.
     */
    static String unescape(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
    }

}
