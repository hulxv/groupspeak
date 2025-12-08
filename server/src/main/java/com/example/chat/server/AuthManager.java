package com.example.chat.server;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

public class AuthManager {

    public AuthManager() {
    }

    public AuthResult registerUser(String username, String password, String displayName, String email) {
        if (User.userExists(username)) {
            return new AuthResult(false, "User already exists", null, null, null, null);
        }

        try {
            String hashedPassword = hashPassword(password);
            String newUserId = UUID.randomUUID().toString();

            User newUser = new User(newUserId, username, email, hashedPassword, displayName);
            newUser.save();

            return new AuthResult(true, "SUCCESS", newUser.getUserId(), null, displayName, email);
        } catch (SQLException e) {
            System.err.println("Error during registration: " + e.getMessage());
            return new AuthResult(false, "Database error during registration.", null, null, null, null);
        }
    }

    public AuthResult authenticate(String username, String password, String deviceInfo) throws SQLException {
        User userRecord = User.findByUsername(username);

        if (userRecord == null) {
            return new AuthResult(false, "Invalid credentials", null, null, null, null);
        }

        if (verifyPassword(password, userRecord.getPasswordHash())) {
            String sessionToken = generateRandomToken();

            UserSession newSession = new UserSession(userRecord.getUserId(), sessionToken);
            newSession.save();

            userRecord.updateTimestamp("last_seen");
            User.updateOnlineStatus(userRecord.getUserId(), true);

            // Broadcast online status
            MessagingManager.broadcastUserStatus(userRecord.getUserId(), true);

            // Return success with all user details including email and displayName
            return new AuthResult(
                true, 
                "SUCCESS", 
                userRecord.getUserId(), 
                sessionToken, 
                userRecord.getDisplayName(), 
                userRecord.getEmail()
            );
        }

        return new AuthResult(false, "Invalid credentials", null, null, null, null);
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private Boolean verifyPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class AuthResult {
        public final boolean success;
        public final String message;
        public final String userId;
        public final String sessionToken;
        public final String displayName;
        public final String email;

        public AuthResult(boolean success, String message, String userId, String sessionToken, String displayName, String email) {
            this.success = success;
            this.message = message;
            this.userId = userId;
            this.sessionToken = sessionToken;
            this.displayName = displayName;
            this.email = email;
        }
    }
}

