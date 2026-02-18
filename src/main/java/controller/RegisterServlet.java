package controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import model.bo.RegisterBO;

/**
 * Register Servlet - xử lý đăng ký
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    
    private RegisterBO registerBO;
    
    public RegisterServlet() {
        this.registerBO = new RegisterBO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Chuyển hướng đến trang register
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String fullName = request.getParameter("fullName");
        
        String jsonResult = handleRegister(username, password, fullName);
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.print(jsonResult);
        out.flush();
    }
    
    /**
     * Xử lý đăng ký
     */
    public String handleRegister(String username, String password, String fullName) {
        return registerBO.processRegister(username, password, fullName);
    }
}
