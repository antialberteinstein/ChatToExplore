package model.dto;

import model.bean.User;

/**
 * Login Result DTO - Kết quả đăng nhập
 */
public class LoginResult {
    private boolean success;
    private String message;
    private User user;
    
    public LoginResult(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public User getUser() { return user; }
    
    public String toJsonString() {
        if (success && user != null) {
            return "{" +
                "\"success\": true, " +
                "\"message\": \"" + message + "\", " +
                "\"username\": \"" + user.getUsername() + "\", " +
                "\"fullName\": \"" + user.getFullName() + "\"" +
                "}";
        } else {
            return "{\"success\": false, \"message\": \"" + message + "\"}";
        }
    }
}