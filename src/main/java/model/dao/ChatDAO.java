package model.dao;

import config.DatabaseManager;
import model.dto.ChatRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat DAO - xử lý lưu trữ tin nhắn chat vào database
 */
public class ChatDAO {
    
    /**
     * Lưu tin nhắn chat của user vào database
     */
    public void saveMessage(String username, String question, String response) {
        String deleteSql = "DELETE FROM chat_messages WHERE username = ? AND is_last_user_message = TRUE";
        String sqlUser = "INSERT INTO chat_messages (username, role, content, is_last_user_message) VALUES (?, 'user', ?, FALSE)";
        String sqlAssistant = "INSERT INTO chat_messages (username, role, content, is_last_user_message) VALUES (?, 'assistant', ?, FALSE)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Xóa hàng có is_last_user_message = TRUE
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, username);
                deleteStmt.executeUpdate();
            }
            
            // Lưu câu hỏi (user)
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUser)) {
                pstmt.setString(1, username);
                pstmt.setString(2, question);
                pstmt.executeUpdate();
            }
            
            // Lưu câu trả lời (assistant)
            try (PreparedStatement pstmt = conn.prepareStatement(sqlAssistant)) {
                pstmt.setString(1, username);
                pstmt.setString(2, response);
                pstmt.executeUpdate();
            }
            
            System.out.println("Chat saved to database for user " + username);
            
        } catch (SQLException e) {
            System.err.println("Error saving chat message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveLastMessage(String username, String content) {
        String role = "user";
        String sql = "INSERT INTO chat_messages (username, role, content, is_last_user_message) VALUES (?, ?, ?, TRUE)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, role);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
            
            System.out.println("Chat message saved to database for user " + username);
            
        } catch (SQLException e) {
            System.err.println("Error saving chat message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getLastMessage(String username) {
        String sql = "SELECT content FROM chat_messages WHERE username = ? AND is_last_user_message = TRUE ORDER BY created_at DESC LIMIT 1";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("content");
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving last chat message: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Lấy lịch sử chat của user từ database
     * Nếu có nhiều hơn hoặc bằng hai content có cùng role nằm sát nhau thì gộp lại thành một String (cách nhau bởi \n)
     */
    public List<ChatRequest.Message> getChatHistory(String username) {
        List<ChatRequest.Message> messages = new ArrayList<>();
        String sql = "SELECT role, content FROM chat_messages WHERE username = ? AND is_last_user_message = FALSE ORDER BY created_at ASC";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            String currentRole = null;
            StringBuilder currentContent = new StringBuilder();
            List<String> groupedQuestions = new ArrayList<>();
            List<String> groupedResponses = new ArrayList<>();
            
            while (rs.next()) {
                String role = rs.getString("role");
                String content = rs.getString("content");
                
                if (currentRole == null) {
                    currentRole = role;
                    currentContent.append(content);
                } else if (currentRole.equals(role)) {
                    currentContent.append("\n").append(content);
                } else {
                    // Gộp xong một nhóm, lưu lại
                    if ("user".equals(currentRole)) {
                        groupedQuestions.add(currentContent.toString());
                    } else if ("assistant".equals(currentRole)) {
                        groupedResponses.add(currentContent.toString());
                    }
                    // Reset cho nhóm tiếp theo
                    currentRole = role;
                    currentContent = new StringBuilder(content);
                }
            }
            // Lưu nhóm cuối cùng
            if (currentRole != null) {
                if ("user".equals(currentRole)) {
                    groupedQuestions.add(currentContent.toString());
                } else if ("assistant".equals(currentRole)) {
                    groupedResponses.add(currentContent.toString());
                }
            }
            
            // Ghép question với response theo thứ tự
            int size = Math.min(groupedQuestions.size(), groupedResponses.size());
            for (int i = 0; i < size; i++) {
                messages.add(new ChatRequest.Message(groupedQuestions.get(i), groupedResponses.get(i)));
            }
            
            System.out.println("Loaded " + messages.size() + " chat messages for user " + username);
            
        } catch (SQLException e) {
            System.err.println("Error loading chat history: " + e.getMessage());
            e.printStackTrace();
        }
        
        return messages;
    }
    
    /**
     * Xóa lịch sử chat của user từ database
     */
    public void clearChatHistory(String username) {
        String sql = "DELETE FROM chat_messages WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            int deleted = pstmt.executeUpdate();
            
            System.out.println("Cleared " + deleted + " chat messages for user " + username);
            
        } catch (SQLException e) {
            System.err.println("Error clearing chat history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Chat message class (để tương thích với frontend)
     */
    public static class ChatMessage {
        public String sender;
        public String message;
        
        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }
    }
}
