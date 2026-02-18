package model.dao;

import config.DatabaseManager;
import model.bean.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserInfoDAO {

    private static final String SELECT_USER = "SELECT username, password, fullName FROM users WHERE username = ?";
    private static final String UPDATE_FULLNAME = "UPDATE users SET fullName = ? WHERE username = ?";
    private static final String UPDATE_PASSWORD = "UPDATE users SET password = ? WHERE username = ?";

    private final DatabaseManager dbManager;

    public UserInfoDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public User getUserByUsername(String username) {
        if (username == null) return null;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_USER)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setUsername(rs.getString("username"));
                    u.setPassword(rs.getString("password"));
                    u.setFullName(rs.getString("fullName"));
                    return u;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in UserInfoDAO.getUserByUsername: " + e.getMessage());
        }
        return null;
    }

    public boolean updateFullName(String username, String fullName) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_FULLNAME)) {

            stmt.setString(1, fullName != null ? fullName : "");
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error in UserInfoDAO.updateFullName: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(String username, String newPassword) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_PASSWORD)) {

            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error in UserInfoDAO.updatePassword: " + e.getMessage());
            return false;
        }
    }
}

