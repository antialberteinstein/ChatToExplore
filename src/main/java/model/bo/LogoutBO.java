package model.bo;

import model.dao.LogoutDAO;

/**
 * Logout Business Object - xử lý logic đăng xuất
 */
public class LogoutBO {
    
    private LogoutDAO logoutDAO;
    
    public LogoutBO() {
        this.logoutDAO = new LogoutDAO();
    }
    
    /**
     * Xử lý đăng xuất (chủ yếu logging)
     */
    public String processLogout(String sessionInfo) {
        // Log logout event
        logoutDAO.logLogout(sessionInfo != null ? sessionInfo : "unknown");
        
        return "{\"success\": true, \"message\": \"Logout successful\"}";
    }
}
