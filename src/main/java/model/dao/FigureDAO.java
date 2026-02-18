package model.dao;

import java.sql.*;
import config.DatabaseManager;

/**
 * Figure DAO - xử lý database cho nhân vật lịch sử
 */
public class FigureDAO {
    
    private static final String SELECT_FIGURE_BY_ID = 
        "SELECT id, name, shortInfo, image_url, born, died, hometown FROM figures WHERE id = ?";
        
    private static final String SELECT_FIGURES_BY_USER = 
        "SELECT f.id, f.name, f.shortInfo, f.image_url, f.born, f.died, f.hometown " +
        "FROM figures f INNER JOIN user_figures uf ON f.id = uf.figureId " +
        "WHERE uf.userName = ?";
    
    private static final String INSERT_FIGURE = 
        "INSERT INTO figures (name, shortInfo, image_url, born, died, hometown) VALUES (?, ?, ?, ?, ?, ?)";
        
    private static final String INSERT_USER_FIGURE = 
        "INSERT INTO user_figures (userName, figureId) VALUES (?, ?)";
    private static final String DELETE_USER_FIGURE =
        "DELETE FROM user_figures WHERE userName = ? AND figureId = ?";
        
    private static final String SELECT_FIGURE_BY_NAME = 
        "SELECT id FROM figures WHERE name = ?";
    
    private static final String UPDATE_FIGURE_IMAGE =
        "UPDATE figures SET image_url = ? WHERE id = ?";

    private static final String UPDATE_FIGURE_INFO =
        "UPDATE figures SET name = ?, shortInfo = ?, born = ?, died = ?, hometown = ? WHERE id = ?";
    
    private final DatabaseManager dbManager;
    
    public FigureDAO() {
        this.dbManager = DatabaseManager.getInstance();
        // Khởi tạo demo data nếu database trống
        initializeDemoData();
    }
    
    /**
     * Khởi tạo demo data nếu chưa có
     */
    private void initializeDemoData() {
        try {
            if (getFigureCount() == 0) {
                insertDemoFigures();
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize demo data: " + e.getMessage());
        }
    }
    
    /**
     * Đếm số lượng figures trong database
     */
    private int getFigureCount() {
        try (Connection connection = dbManager.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM figures")) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting figures: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Thêm demo figures vào database
     */
    private void insertDemoFigures() {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_FIGURE)) {
            
            // Trần Hưng Đạo
            stmt.setString(1, "Trần Hưng Đạo");
            stmt.setString(2, "Danh tướng chống Mông Cổ, ba lần đánh bại quân xâm lược");
            stmt.setString(3, "");
            stmt.setInt(4, 1228);
            stmt.setInt(5, 1300);
            stmt.setString(6, "Nam Định");
            stmt.executeUpdate();
            
            // Lý Thái Tổ  
            stmt.setString(1, "Lý Thái Tổ");
            stmt.setString(2, "Hoàng đế sáng lập nhà Lý, dời đô về Thăng Long");
            stmt.setString(3, "");
            stmt.setInt(4, 974);
            stmt.setInt(5, 1028);
            stmt.setString(6, "Cổ Pháp");
            stmt.executeUpdate();
            
            // Nguyễn Trãi
            stmt.setString(1, "Nguyễn Trãi");
            stmt.setString(2, "Nhà chính trị, quân sự và văn học lớn thời Lê sơ");
            stmt.setString(3, "");
            stmt.setInt(4, 1380);
            stmt.setInt(5, 1442);
            stmt.setString(6, "Hải Dương");
            stmt.executeUpdate();
            
            System.out.println("Demo figures inserted successfully!");
            
        } catch (SQLException e) {
            System.err.println("Error inserting demo figures: " + e.getMessage());
        }
    }
    
    /**
     * Lấy figure theo ID
     */
    public String getFigureById(String figureId) {
        if (figureId == null || figureId.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"ID nhân vật không được để trống\"}";
        }

        long id;
        try {
            id = Long.parseLong(figureId.trim());
        } catch (NumberFormatException e) {
            return "{\"success\": false, \"message\": \"ID nhân vật không hợp lệ\"}";
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_FIGURE_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = "{" +
                        "\"id\": \"" + rs.getLong("id") + "\", " +
                        "\"name\": \"" + escapeJson(rs.getString("name")) + "\", " +
                        "\"shortInfo\": \"" + escapeJson(rs.getString("shortInfo")) + "\", " +
                        "\"imageUrl\": \"" + escapeJson(rs.getString("image_url")) + "\", " +
                        "\"born\": " + rs.getInt("born") + ", " +
                        "\"died\": " + (rs.getObject("died") != null ? rs.getInt("died") : "null") + ", " +
                        "\"hometown\": \"" + escapeJson(rs.getString("hometown")) + "\"" +
                        "}";
                    return "{\"success\": true, \"figure\": " + json + "}";
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting figure by ID: " + e.getMessage());
            e.printStackTrace();
            return "{\"success\": false, \"message\": \"Lỗi khi truy vấn nhân vật\"}";
        }

        return "{\"success\": false, \"message\": \"Không tìm thấy nhân vật\"}";
    }
    
    /**
     * Lấy figures theo username (thông qua UserFigure)
     */
    public String getFiguresByUsername(String username) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_FIGURES_BY_USER)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                StringBuilder json = new StringBuilder("{\"success\": true, \"figures\": [");
                boolean first = true;
                
                while (rs.next()) {
                    if (!first) json.append(", ");
                    first = false;
                    
                    json.append("{")
                        .append("\"id\": \"").append(rs.getLong("id")).append("\", ")
                        .append("\"name\": \"").append(escapeJson(rs.getString("name"))).append("\", ")
                        .append("\"shortInfo\": \"").append(escapeJson(rs.getString("shortInfo"))).append("\", ")
                        .append("\"imageUrl\": \"").append(escapeJson(rs.getString("image_url"))).append("\", ")
                        .append("\"born\": ").append(rs.getInt("born")).append(", ")
                        .append("\"died\": ").append(rs.getObject("died") != null ? rs.getInt("died") : "null").append(", ")
                        .append("\"hometown\": \"").append(escapeJson(rs.getString("hometown"))).append("\"")
                        .append("}");
                }
                
                json.append("]}");
                return json.toString();
            }
        } catch (SQLException e) {
            System.err.println("Error getting figures by username: " + e.getMessage());
            e.printStackTrace();
            return "{\"success\": false, \"message\": \"Lỗi khi truy vấn danh sách nhân vật\"}";
        }
    }
    
    /**
     * Tạo UserFigure mới
     * Nếu Figure chưa tồn tại -> Tạo Figure mới -> Thêm UserFigure
     * Nếu Figure đã tồn tại -> Chỉ thêm UserFigure
     */
    public String createUserFigure(String username, String figureName, Integer born, Integer died, String description, String imageUrl, String hometown) {
        try {
            // Kiểm tra xem figure đã có chưa
            Long figureId = findFigureIdByName(figureName);
            
            // Nếu chưa có, tạo figure mới
            if (figureId == null) {
                figureId = createNewFigure(figureName, born, died, description, imageUrl, hometown);
                if (figureId == null) {
                    return "{\"success\": false, \"message\": \"Không thể tạo nhân vật mới\"}";
                }
            }
            
            // Thêm vào user_figures
            if (createUserFigureRelation(username, figureId)) {
                return "{\"success\": true, \"message\": \"Thêm nhân vật thành công\", \"figureId\": \"" + figureId + "\"}";
            } else {
                return "{\"success\": false, \"message\": \"Không thể thêm nhân vật vào danh sách của bạn\"}";
            }
            
        } catch (Exception e) {
            System.err.println("Error creating user figure: " + e.getMessage());
            e.printStackTrace();
            return "{\"success\": false, \"message\": \"Lỗi hệ thống: " + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /**
     * Backwards-compatible overload that accepts a period string like "1228-1300".
     */
    public String createUserFigure(String username, String figureName, String period, String description) {
        Integer born = null;
        Integer died = null;
        if (period != null) {
            String[] parts = period.split("-");
            try {
                if (parts.length > 0 && parts[0].trim().length() > 0) born = Integer.parseInt(parts[0].trim());
                if (parts.length > 1 && parts[1].trim().length() > 0) died = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return createUserFigure(username, figureName, born, died, description, null, null);
    }
    
    /**
     * Tìm figure ID theo tên
     */
    private Long findFigureIdByName(String figureName) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_FIGURE_BY_NAME)) {
            
            stmt.setString(1, figureName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding figure by name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Public wrapper to retrieve a figure ID by its exact name.
     * Returns null when not found or on error.
     */
    public Long getFigureIdByName(String figureName) {
        return findFigureIdByName(figureName);
    }
    
    /**
     * Tạo figure mới
     */
    private Long createNewFigure(String figureName, String period, String description) {
        return createNewFigure(figureName, null, null, description, null, null);
    }

    /**
     * Overloaded create new figure that accepts imageUrl and hometown
     */
    private Long createNewFigure(String figureName, Integer born, Integer died, String description, String imageUrl, String hometown) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_FIGURE, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, figureName);
            stmt.setString(2, description != null ? description : "");
            stmt.setString(3, imageUrl != null ? imageUrl : "");

            if (born != null) {
                stmt.setInt(4, born);
            } else {
                stmt.setInt(4, -1);
            }

            if (died != null) {
                stmt.setInt(5, died);
            } else {
                stmt.setInt(5, -1);
            }

            stmt.setString(6, hometown != null ? hometown : "");

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating new figure: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Tạo quan hệ user-figure
     */
    private boolean createUserFigureRelation(String username, Long figureId) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_USER_FIGURE)) {
            
            stmt.setString(1, username);
            stmt.setLong(2, figureId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                // User đã có figure này rồi
                return true;
            }
            System.err.println("Error creating user-figure relation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa quan hệ user-figure (không xóa record figure)
     */
    public boolean deleteUserFigureRelation(String username, Long figureId) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(DELETE_USER_FIGURE)) {

            stmt.setString(1, username);
            stmt.setLong(2, figureId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user-figure relation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật image_url cho một figure
     */
    public boolean updateFigureImage(long figureId, String imageUrl) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(UPDATE_FIGURE_IMAGE)) {

            stmt.setString(1, imageUrl != null ? imageUrl : "");
            stmt.setLong(2, figureId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating figure image: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thông tin cơ bản của một figure
     */
    public boolean updateFigureInfo(long figureId, String name, String shortInfo, Integer born, Integer died, String hometown) {
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(UPDATE_FIGURE_INFO)) {

            stmt.setString(1, name != null ? name : "");
            stmt.setString(2, shortInfo != null ? shortInfo : "");

            if (born != null) stmt.setInt(3, born); else stmt.setInt(3, -1);
            if (died != null) stmt.setInt(4, died); else stmt.setInt(4, -1);

            stmt.setString(5, hometown != null ? hometown : "");
            stmt.setLong(6, figureId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating figure info: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Escape JSON string để tránh lỗi format
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
