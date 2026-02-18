package controller;

import model.bo.UserInfoBO;
import model.bean.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/user-info")
public class UserInfoServlet extends HttpServlet {

    private UserInfoBO userInfoBO;

    public UserInfoServlet() {
        this.userInfoBO = new UserInfoBO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String username = (String) session.getAttribute("username");
        User u = userInfoBO.getUser(username);
        request.setAttribute("user", u);
        request.getRequestDispatcher("/user-info.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        if (session == null || session.getAttribute("username") == null) {
            out.print("{\"success\": false, \"message\": \"Vui lòng đăng nhập\"}");
            out.flush();
            return;
        }

        String username = (String) session.getAttribute("username");
        String action = request.getParameter("action");

        if ("updateInfo".equals(action)) {
            String fullName = request.getParameter("fullName");
            String result = userInfoBO.updateFullName(username, fullName);
            out.print(result);
            out.flush();
            return;
        } else if ("changePassword".equals(action)) {
            String current = request.getParameter("currentPassword");
            String next = request.getParameter("newPassword");
            String result = userInfoBO.changePassword(username, current, next);
            out.print(result);
            out.flush();
            return;
        }

        out.print("{\"success\": false, \"message\": \"Hành động không hợp lệ\"}");
        out.flush();
    }
}
 