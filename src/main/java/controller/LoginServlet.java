package controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

import model.bean.User;
import model.bo.LoginBO;
import model.dto.LoginResult;

/**
 * Login Servlet - xử lý đăng nhập
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    
    private LoginBO loginBO;
    
    public LoginServlet() {
        this.loginBO = new LoginBO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Chuyển hướng đến trang login
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        LoginResult result = loginBO.processLogin(username, password);
        
        // Nếu login thành công, tạo session
        if (result.isSuccess() && result.getUser() != null) {
            HttpSession session = request.getSession(true);
            User user = result.getUser();
            session.setAttribute("user", user);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("fullName", user.getFullName());
            session.setMaxInactiveInterval(30 * 60); // 30 phút
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.print(result.toJsonString());
        out.flush();
    }
    
    /**
     * Xử lý đăng nhập
     */
    public String handleLogin(String username, String password) {
        LoginResult result = loginBO.processLogin(username, password);
        return result.toJsonString();
    }
    

}
