package config;

import java.sql.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * DatabaseManager - Lazy Singleton quản lý kết nối database
 * Tự động tạo database và tables từ file database.sql nếu chưa tồn tại
 */
public class DatabaseManager {
    
    // Singleton instance
    private static volatile DatabaseManager instance;
    
    // Database configuration
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "finalproject";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "02022005";
    
    // JDBC URLs
    private static final String JDBC_URL_WITHOUT_DB = 
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/?useSSL=false&serverTimezone=UTC";
    private static final String JDBC_URL_WITH_DB = 
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?useSSL=false&serverTimezone=UTC";
    
    // Flag để kiểm tra đã khởi tạo database chưa
    private boolean databaseInitialized = false;
    
    /**
     * Private constructor cho Singleton pattern
     */
    private DatabaseManager() {
        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Khởi tạo database
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }
    
    /**
     * Lazy Singleton - Thread-safe
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Lấy Connection đến database
     */
    public Connection getConnection() throws SQLException {
        if (!databaseInitialized) {
            initializeDatabase();
        }
        return DriverManager.getConnection(JDBC_URL_WITH_DB, DB_USERNAME, DB_PASSWORD);
    }
    
    /**
     * Khởi tạo database và tables từ file database.sql
     */
    private synchronized void initializeDatabase() {
        if (databaseInitialized) {
            return;
        }
        
        try {
            // 1. Tạo database nếu chưa có
            createDatabaseIfNotExists();
            
            // 2. Thực thi script database.sql
            executeSqlScript();
            
            // 3. Thêm dữ liệu mặc định
            insertDefaultFigures();
            
            databaseInitialized = true;
            System.out.println("Database initialized successfully!");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tạo database nếu chưa tồn tại
     */
    private void createDatabaseIfNotExists() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                JDBC_URL_WITHOUT_DB, DB_USERNAME, DB_PASSWORD)) {
            
            String createDbSql = "CREATE DATABASE IF NOT EXISTS " + DB_NAME;
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createDbSql);
                System.out.println("Database '" + DB_NAME + "' ensured to exist.");
            }
        }
    }
    
    /**
     * Thực thi file database.sql
     */
    private void executeSqlScript() {
        String sqlFilePath = Paths.get(System.getProperty("user.dir"), "database.sql").toString();
        
        try (Connection connection = DriverManager.getConnection(
                JDBC_URL_WITH_DB, DB_USERNAME, DB_PASSWORD);
             BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
            
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Bỏ qua comments và dòng trống
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                sqlBuilder.append(line).append(" ");
                
                // Thực thi khi gặp dấu ;
                if (line.endsWith(";")) {
                    String sql = sqlBuilder.toString().trim();
                    if (!sql.isEmpty()) {
                        executeSingleStatement(connection, sql);
                    }
                    sqlBuilder = new StringBuilder();
                }
            }
            
            // Thực thi statement cuối nếu có
            String finalSql = sqlBuilder.toString().trim();
            if (!finalSql.isEmpty()) {
                executeSingleStatement(connection, finalSql);
            }
            
            System.out.println("SQL script executed successfully!");
            
        } catch (IOException e) {
            System.err.println("Cannot read database.sql file: " + e.getMessage());
            System.out.println("Trying to create basic tables manually...");
            createBasicTablesManually();
        } catch (SQLException e) {
            System.err.println("SQL execution error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Thực thi một SQL statement
     */
    private void executeSingleStatement(Connection connection, String sql) {
        try (Statement stmt = connection.createStatement()) {
            if (sql.trim().toUpperCase().startsWith("SELECT") || 
                sql.trim().toUpperCase().startsWith("DESCRIBE") ||
                sql.trim().toUpperCase().startsWith("SHOW")) {
                // Không thực thi SELECT statements trong script khởi tạo
                return;
            }
            
            stmt.executeUpdate(sql);
            System.out.println("Executed: " + sql.substring(0, Math.min(50, sql.length())) + "...");
            
        } catch (SQLException e) {
            // Bỏ qua lỗi IF NOT EXISTS, table đã tồn tại, hoặc index trùng lặp
            String errorMsg = e.getMessage().toLowerCase();
            if (!errorMsg.contains("already exists") && 
                !errorMsg.contains("duplicate key name") &&
                !errorMsg.contains("duplicate entry")) {
                System.err.println("Error executing SQL: " + sql);
                System.err.println("Error: " + e.getMessage());
            } else {
                // Log nhẹ cho các lỗi có thể bỏ qua
                System.out.println("Skipped (already exists): " + sql.substring(0, Math.min(30, sql.length())) + "...");
            }
        }
    }
    
    /**
     * Tạo basic tables manually nếu không đọc được file database.sql
     */
    private void createBasicTablesManually() {
        try (Connection connection = DriverManager.getConnection(
                JDBC_URL_WITH_DB, DB_USERNAME, DB_PASSWORD)) {
            
            // Tạo table users
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) UNIQUE NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "fullName VARCHAR(100) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
            
            // Tạo table figures
            String createFiguresTable = "CREATE TABLE IF NOT EXISTS figures (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "shortInfo TEXT, " +
                "image_url VARCHAR(500), " +
                "born INT NOT NULL, " +
                "died INT, " +
                "hometown VARCHAR(100), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
            
            // Tạo table user_figures
            String createUserFiguresTable = "CREATE TABLE IF NOT EXISTS user_figures (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "userName VARCHAR(50) NOT NULL, " +
                "figureId BIGINT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (userName) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (figureId) REFERENCES figures(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_user_figure (userName, figureId)" +
                ")";
            
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createUsersTable);
                stmt.executeUpdate(createFiguresTable);
                stmt.executeUpdate(createUserFiguresTable);
                System.out.println("Basic tables created manually.");
            }
            
        } catch (SQLException e) {
            System.err.println("Failed to create basic tables: " + e.getMessage());
        }
    }
    
    /**
     * Thêm các nhân vật mặc định vào database
     */
    private void insertDefaultFigures() {
        try (Connection connection = DriverManager.getConnection(
                JDBC_URL_WITH_DB, DB_USERNAME, DB_PASSWORD)) {
            
            // Kiểm tra xem đã có figures chưa
            String checkSql = "SELECT COUNT(*) FROM figures";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                
                if (rs.next() && rs.getInt(1) > 0) {
                    // Đã có dữ liệu, không insert nữa
                    System.out.println("Figures already exist, skipping default data.");
                    return;
                }
            }
            
            // Insert các nhân vật mặc định
            String insertSql = "INSERT INTO figures (name, shortInfo, image_url, born, died, hometown) VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                // Trần Thủ Độ
                pstmt.setString(1, "Trần Thủ Độ");
                pstmt.setString(2, "Công thần khai quốc triều Trần, đưa Trần Thái Tông lên ngôi");
                pstmt.setString(3, "images/tran_thu_do.png");
                pstmt.setInt(4, 1194);
                pstmt.setInt(5, 1264);
                pstmt.setString(6, "Hưng Yên");
                pstmt.executeUpdate();
                
                // Trần Hưng Đạo
                pstmt.setString(1, "Trần Hưng Đạo");
                pstmt.setString(2, "Đại tướng triều Trần, anh hùng dân tộc, ba lần đánh thắng quân Nguyên-Mông xâm lược");
                pstmt.setString(3, "images/tran_hung_dao.png");
                pstmt.setInt(4, 1228);
                pstmt.setInt(5, 1300);
                pstmt.setString(6, "Nam Định");
                pstmt.executeUpdate();
                
                // Hồ Chí Minh
                pstmt.setString(1, "Hồ Chí Minh");
                pstmt.setString(2, "Chủ tịch nước Việt Nam Dân chủ Cộng hòa, lãnh tụ của cách mạng Việt Nam, anh hùng giải phóng dân tộc");
                pstmt.setString(3, "images/ho_chi_minh.png");
                pstmt.setInt(4, 1890);
                pstmt.setInt(5, 1969);
                pstmt.setString(6, "Nghệ An");
                pstmt.executeUpdate();
                
                System.out.println("Default figures inserted successfully!");
            }
            
        } catch (SQLException e) {
            System.err.println("Failed to insert default figures: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Kiểm tra kết nối database
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Đóng tất cả kết nối (cleanup khi shutdown)
     */
    public void shutdown() {
        // Implement nếu cần thiết
        System.out.println("DatabaseManager shutdown completed.");
    }
}
