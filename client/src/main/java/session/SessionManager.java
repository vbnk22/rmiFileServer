package session;

import enums.UserRole;

public class SessionManager {

    private static String sessionId;
    private static UserRole role;

    public static String getSessionId() {
        return sessionId;
    }

    public static void setSessionId(String sessionId) {
        SessionManager.sessionId = sessionId;
    }

    public static UserRole getRole() {
        return role;
    }

    public static void setRole(UserRole role) {
        SessionManager.role = role;
    }

    public static void clear() {
        sessionId = null;
        role = null;
    }
}
