package model.bo;

import model.bean.User;
import model.dao.UserInfoDAO;

/**
 * User info business object
 */
public class UserInfoBO {

    private UserInfoDAO userInfoDAO;

    public UserInfoBO() {
        this.userInfoDAO = new UserInfoDAO();
    }

    public User getUser(String username) {
        return userInfoDAO.getUserByUsername(username);
    }

    public String updateFullName(String username, String fullName) {
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Không có user\"}";
        }
        boolean ok = userInfoDAO.updateFullName(username.trim(), fullName != null ? fullName.trim() : "");
        if (ok) return "{\"success\": true, \"message\": \"Cập nhật thông tin thành công\"}";
        return "{\"success\": false, \"message\": \"Không thể cập nhật thông tin\"}";
    }

    public String changePassword(String username, String currentPassword, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Không có user\"}";
        }

        User u = userInfoDAO.getUserByUsername(username.trim());
        if (u == null) return "{\"success\": false, \"message\": \"User không tồn tại\"}";

        // Passwords stored in plain text in this app
        if (currentPassword == null || !currentPassword.equals(u.getPassword())) {
            return "{\"success\": false, \"message\": \"Mật khẩu hiện tại không đúng\"}";
        }

        if (newPassword == null || newPassword.length() < 6) {
            return "{\"success\": false, \"message\": \"Mật khẩu mới phải có ít nhất 6 ký tự\"}";
        }

        boolean ok = userInfoDAO.updatePassword(username.trim(), newPassword);
        if (ok) return "{\"success\": true, \"message\": \"Đổi mật khẩu thành công\"}";
        return "{\"success\": false, \"message\": \"Không thể đổi mật khẩu\"}";
    }
}

