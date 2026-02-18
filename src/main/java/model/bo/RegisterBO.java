package model.bo;

import model.bean.User;
import model.dao.RegisterDAO;

/**
 * Register Business Object - xử lý logic đăng ký
 */
public class RegisterBO {
    
    private RegisterDAO registerDAO;
    
    public RegisterBO() {
        this.registerDAO = new RegisterDAO();
    }
    
    /**
     * Xử lý đăng ký
     */
    public String processRegister(String username, String password, String fullName) {
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Vui lòng nhập tên đăng nhập\"}";
        }
        
        if (password == null || password.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Vui lòng nhập mật khẩu\"}";
        }
        
        if (fullName == null || fullName.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Vui lòng nhập họ và tên\"}";
        }
        
        // Kiểm tra username length
        if (username.trim().length() < 3) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập phải có ít nhất 3 ký tự\"}";
        }
        
        // Kiểm tra username format (chỉ chấp nhận chữ cái, số và _)
        if (!username.trim().matches("^[a-zA-Z0-9_]+$")) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới\"}";
        }
        
        if (password.length() < 6) {
            return "{\"success\": false, \"message\": \"Mật khẩu phải có ít nhất 6 ký tự\"}";
        }
        
        // Kiểm tra user đã tồn tại
        if (registerDAO.isUserExists(username.trim())) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác\"}";
        }
        
        // Tạo user mới
        User newUser = new User(username.trim(), password, fullName.trim());
        
        // Lưu vào database
        if (registerDAO.saveUser(newUser)) {
            return "{\"success\": true, \"message\": \"Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ\"}";
        } else {
            return "{\"success\": false, \"message\": \"Lỗi hệ thống. Vui lòng thử lại sau\"}";
        }
    }
}
