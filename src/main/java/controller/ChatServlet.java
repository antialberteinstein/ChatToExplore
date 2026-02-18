package controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.google.gson.Gson;
import model.bo.ChatBO;
import model.dto.ChatRequest;
import model.dto.ChatResult;
import java.util.ArrayList;

/**
 * Chat Servlet - xử lý chat với bot thông qua TCP server
 * Endpoints:
 * - /chat?action=loadHistory: Lấy lịch sử chat
 * - /chat?action=submit: Gửi câu hỏi vào queue
 * - /chat?action=poll: Lấy kết quả (polling)
 */
@WebServlet("/chat")
public class ChatServlet extends HttpServlet {
    
    private ChatBO chatBO;
    private Gson gson;
    
    public ChatServlet() {
        this.chatBO = new ChatBO();
        this.gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Set UTF-8 encoding
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            sendJsonResponse(response, "{\"success\": false, \"message\": \"Unauthorized\"}");
            return;
        }
        
        String currentUser = (String) session.getAttribute("username");
        String action = request.getParameter("action");
        
        if (action == null) {
            sendJsonResponse(response, "{\"success\": false, \"message\": \"Missing action parameter\"}");
            return;
        }
        
        switch (action) {
            case "loadHistory":
                handleLoadHistory(currentUser, response);
                break;
            case "submit":
                handleSubmit(currentUser, request, response);
                break;
            case "poll":
                handlePoll(currentUser, request, response);
                break;
            case "save":
                handleSave(currentUser, request, response);
                break;
            default:
                sendJsonResponse(response, "{\"success\": false, \"message\": \"Unknown action\"}");
        }
    }
    
    /**
     * Load lịch sử chat từ ChatDAO
     */
    private void handleLoadHistory(String username, HttpServletResponse response) throws IOException {
        List<ChatRequest.Message> history = chatBO.loadHistory(username);
        String lastMessage = chatBO.getLastMessage(username);

        // Transform ChatRequest.Message (question/response pairs) into a flat
        // list of role/content objects expected by the frontend.
        // Frontend expects items like: { role: 'user'|'assistant', content: '...' }
        List<RoleMessage> out = new ArrayList<>();
        for (ChatRequest.Message m : history) {
            if (m.question != null) out.add(new RoleMessage("user", m.question));
            if (m.response != null) out.add(new RoleMessage("assistant", m.response));
        }
        if (lastMessage != null) {
            out.add(new RoleMessage("user", lastMessage));
        }

        String json = gson.toJson(new HistoryResponse(true, out));
        sendJsonResponse(response, json);
    }
    
    /**
     * Submit câu hỏi vào queue (process command)
     */
    private void handleSubmit(String username, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String message = request.getParameter("message");
        
        if (message == null || message.trim().isEmpty()) {
            sendJsonResponse(response, "{\"success\": false, \"message\": \"Message is required\"}");
            return;
        }
        
        System.out.println("[ChatServlet] Submitting message from user " + username + ": " + message);
        
        boolean queued = chatBO.submitQuestion(username, message);
        
        if (queued) {
            sendJsonResponse(response, "{\"success\": true, \"message\": \"Đang chờ xử lý\"}");
        } else {
            sendJsonResponse(response, "{\"success\": false, \"message\": \"Không thể kết nối đến server, vui lòng thử lại sau\"}");
        }
    }
    
    /**
     * Poll kết quả (load command)
     * Frontend sẽ render markdown bằng marked.js
     */
    private void handlePoll(String username, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String message = request.getParameter("message");
        
        if (message == null || message.trim().isEmpty()) {
            sendJsonResponse(response, "{\"success\": false, \"message\": \"Message is required\"}");
            return;
        }
        
        ChatResult result = chatBO.pollResult(username, message);
        
        if (result != null) {
            String json = gson.toJson(new PollResponse(true, result));
            sendJsonResponse(response, json);
        } else {
            sendJsonResponse(response, "{\"success\": false, \"message\": \"Lỗi kết nối đến server\"}");
        }
    }
    
    /**
     * Lưu kết quả thành công vào ChatDAO
     */
    private void handleSave(String username, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String question = request.getParameter("question");
        String answer = request.getParameter("answer");
        
        if (question == null || answer == null) {
            sendJsonResponse(response, "{\"success\": false, \"message\": \"Missing parameters\"}");
            return;
        }
        
        chatBO.saveResult(username, question, answer);
        sendJsonResponse(response, "{\"success\": true, \"message\": \"Saved\"}");
    }
    
    private void sendJsonResponse(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
    
    // Response classes
    static class HistoryResponse {
        boolean success;
        Object history;

        HistoryResponse(boolean success, Object history) {
            this.success = success;
            this.history = history;
        }
    }

    static class RoleMessage {
        public String role;
        public String content;

        public RoleMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    
    static class PollResponse {
        boolean success;
        ChatResult result;
        
        PollResponse(boolean success, ChatResult result) {
            this.success = success;
            this.result = result;
        }
    }
}
