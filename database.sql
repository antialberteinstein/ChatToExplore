-- Tạo database (nếu chưa có)
CREATE DATABASE IF NOT EXISTS finalproject;

-- Sử dụng database
USE finalproject;

-- Tạo bảng users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullName VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tạo bảng figures (danh nhân)
CREATE TABLE IF NOT EXISTS figures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    shortInfo TEXT,
    image_url VARCHAR(500),
    born INT NOT NULL,
    died INT,
    hometown VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tạo bảng user_figures (quan hệ nhiều-nhiều giữa users và figures)
CREATE TABLE IF NOT EXISTS user_figures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userName VARCHAR(50) NOT NULL,
    figureId BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (userName) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (figureId) REFERENCES figures(id) ON DELETE CASCADE,
    
    -- Unique constraint để tránh trùng lặp
    UNIQUE KEY unique_user_figure (userName, figureId)
);

-- Tạo bảng chat_messages (lưu lịch sử chat)
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    role ENUM('user', 'assistant') NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_last_user_message BOOLEAN DEFAULT FALSE,
    
    -- Foreign key
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- Index để tăng performance khi query theo user
    INDEX idx_username (username),
    INDEX idx_created_at (created_at)
);

-- Tạo bảng flags
CREATE TABLE IF NOT EXISTS flags (
    username VARCHAR(50) PRIMARY KEY,
    figure_flag BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tạo indexes để tăng performance (sẽ bỏ qua nếu đã tồn tại)
CREATE INDEX idx_figures_born ON figures(born);
CREATE INDEX idx_figures_died ON figures(died);
CREATE INDEX idx_figures_name ON figures(name);
CREATE INDEX idx_user_figures_username ON user_figures(userName);
CREATE INDEX idx_user_figures_figureid ON user_figures(figureId);

-- Hiển thị cấu trúc các bảng
DESCRIBE users;
DESCRIBE figures;
DESCRIBE user_figures;
DESCRIBE chat_messages;