package model.dao;

import java.sql.*;
import model.bean.User;
import config.DatabaseManager;

/**
 * Register DAO - xử lý database cho registration
 */
public class RegisterDAO {
    
    private static final String INSERT_USER_SQL = 
        "INSERT INTO users (username, password, fullName) VALUES (?, ?, ?)";
    private static final String SELECT_USER_SQL = 
        "SELECT username FROM users WHERE username = ?";
    private static final String INSERT_USER_FIGURE_SQL = 
        "INSERT INTO user_figures (userName, figureId) VALUES (?, ?)";
    
    private final DatabaseManager dbManager;
    
    public RegisterDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Lưu user mới và tự động thêm 3 nhân vật mặc định
     */
    public boolean saveUser(User user) {
        Connection connection = null;
        try {
            connection = dbManager.getConnection();
            connection.setAutoCommit(false); // Bắt đầu transaction
            
            // 1. Insert user
            try (PreparedStatement stmt = connection.prepareStatement(INSERT_USER_SQL)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword());
                stmt.setString(3, user.getFullName());
                
                if (stmt.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
            }
            
            // 2. Insert 3 user_figures mặc định (figureId: 1, 2, 3)
            try (PreparedStatement stmt = connection.prepareStatement(INSERT_USER_FIGURE_SQL)) {
                for (int figureId = 1; figureId <= 3; figureId++) {
                    stmt.setString(1, user.getUsername());
                    stmt.setInt(2, figureId);
                    stmt.executeUpdate();
                }
            }
            
            connection.commit(); // Commit transaction
            return true;
            
        } catch (SQLException e) {
            System.err.println("Database error in RegisterDAO.saveUser: " + e.getMessage());
            e.printStackTrace();
            
            // Rollback nếu có lỗi
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            return false;
            
        } finally {
            // Restore auto-commit và đóng connection
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    System.err.println("Connection close failed: " + closeEx.getMessage());
                }
            }
        }
    }
    
    /**
     * Kiểm tra user có tồn tại không
     */
    public boolean isUserExists(String username) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_USER_SQL)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("Database error in RegisterDAO.isUserExists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
