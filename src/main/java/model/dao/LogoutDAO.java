package model.dao;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Logout DAO - xử lý session data cho logout
 */
public class LogoutDAO {
    
    // Lưu trữ session data (đơn giản)
    private static final Map<String, String> sessionData = new ConcurrentHashMap<>();
    
    /**
     * Log logout event
     */
    public void logLogout(String sessionInfo) {
        String logEntry = "[LOGOUT]: " + sessionInfo + " at " + new java.util.Date();
        sessionData.put("logout_" + System.currentTimeMillis(), logEntry);
        System.out.println("Logout logged: " + logEntry);
    }
    
    /**
     * Log login event (helper method)
     */
    public void logLogin(String username, String sessionId) {
        String logEntry = "[LOGIN]: " + username + " (session: " + sessionId + ") at " + new java.util.Date();
        sessionData.put("login_" + System.currentTimeMillis(), logEntry);
        System.out.println("Login logged: " + logEntry);
    }
    
    /**
     * Lấy session logs
     */
    public java.util.List<String> getSessionLogs() {
        return new java.util.ArrayList<>(sessionData.values());
    }
}
