package model.dao;

import java.sql.*;
import model.bean.User;
import config.DatabaseManager;

/**
 * Login DAO - xử lý database cho login
 */
public class LoginDAO {
    
    private static final String SELECT_USER_SQL = 
        "SELECT username, password, fullName FROM users WHERE username = ?";
    
    private final DatabaseManager dbManager;
    
    public LoginDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Tìm user theo username
     */
    public User findUserByUsername(String username) {
        try (Connection connection = dbManager.getConnection()) {
            if (connection == null) {
                System.err.println("Cannot connect to database");
                return null;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(SELECT_USER_SQL)) {
                stmt.setString(1, username);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("fullName")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error in LoginDAO: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
}
