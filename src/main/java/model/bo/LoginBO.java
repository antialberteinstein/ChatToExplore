package model.bo;

import model.bean.User;
import model.dao.LoginDAO;
import model.dto.LoginResult;

/**
 * Login Business Object - xử lý logic đăng nhập
 */
public class LoginBO {
    
    private LoginDAO loginDAO;
    
    public LoginBO() {
        this.loginDAO = new LoginDAO();
    }
    
    /**
     * Xử lý đăng nhập
     */
    public LoginResult processLogin(String username, String password) {
        // Validate input
        if (username == null || password == null || 
            username.trim().isEmpty() || password.trim().isEmpty()) {
            return new LoginResult(false, "Vui lòng nhập đầy đủ tài khoản và mật khẩu", null);
        }
        
        // Tìm user trong database
        User user = loginDAO.findUserByUsername(username);
        
        if (user == null || !user.getPassword().equals(password)) {
            return new LoginResult(false, "Thông tin đăng nhập không đúng", null);
        }
        
        return new LoginResult(true, "Đăng nhập thành công", user);
    }
}
