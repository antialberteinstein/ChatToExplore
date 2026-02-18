package controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

import model.bo.LogoutBO;

/**
 * Logout Servlet - xử lý đăng xuất
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    
    private LogoutBO logoutBO;
    
    public LogoutServlet() {
        this.logoutBO = new LogoutBO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        handleLogout(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        handleLogout(request, response);
    }
    
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        boolean sessionExisted = false;
        
        if (session != null) {
            sessionExisted = true;
            session.invalidate();
        }
        
        String jsonResult = "{\"success\": true, \"message\": \"" + 
            (sessionExisted ? "Logout successful" : "No active session") + 
            "\"}";
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.print(jsonResult);
        out.flush();
    }
    
    /**
     * Xử lý đăng xuất
     */
    public String handleLogout(String sessionId) {
        return logoutBO.processLogout(sessionId);
    }
}
