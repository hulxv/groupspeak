package com.example.chat.server;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class SessionManager {

    public String validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return null;
        }

        try {
            UserSession sessionRecord = UserSession.findByToken(sessionToken);

            if (sessionRecord == null) {
                return null;
            }

            // Convert LocalDateTime (from UserSession) to Instant for comparison,
            // using the static formatter provided by UserSession.
            Instant expiresAtInstant = LocalDateTime.parse(sessionRecord.getExpiresAt(), UserSession.getFormatter())
                    .atZone(ZoneId.systemDefault()).toInstant();

            if (expiresAtInstant.isBefore(Instant.now())) {
                sessionRecord.delete();
                return null;
            }
            sessionRecord.updateTimestamp("last_activity");
            return sessionRecord.getUserId();

        } catch (SQLException e) {
            System.err.println("SessionManager: Database error during session validation: " + e.getMessage());
            return null;
        }
    }

  
    public boolean endSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return false;
        }

        try {
            UserSession sessionRecord = UserSession.findByToken(sessionToken);

            if (sessionRecord == null) {
                return true;
            }

            String userId = sessionRecord.getUserId();
            sessionRecord.delete();

            return this.updateOnlineStatus(userId, false);

        } catch (SQLException e) {
            System.err.println("SessionManager: Database error during session termination: " + e.getMessage());
            return false;
        }
    }

    public boolean updateOnlineStatus(String userId, boolean isOnline) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }

        try {
            User.updateOnlineStatus(userId, isOnline);

            if (!isOnline) {
                User userRecord = User.findById(userId);
                if (userRecord != null) {
                    userRecord.updateTimestamp("last_seen");
                }
            }
            return true;

        } catch (SQLException e) {
            System.err.println(
                    "SessionManager: Database error updating online status for user " + userId + ": " + e.getMessage());
            return false;
        }
    }
}