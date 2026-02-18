<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Nếu đã đăng nhập thì redirect về index
    String username = (String) session.getAttribute("username");
    if (username != null) {
        response.sendRedirect("index.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login & Register</title>
    <link rel="stylesheet" href="css/base.css">
    <link rel="stylesheet" href="css/navbar.css">
    <link rel="stylesheet" href="css/components.css">
    <link rel="stylesheet" href="css/auth.css">
</head>
<body>
    <div class="auth-container">
        <div class="auth-header">
            <h1>Chào mừng</h1>
            <p>Đăng nhập để xem Timeline Lịch sử</p>
        </div>

        <div class="auth-toggle">
            <button type="button" class="toggle-btn active" id="loginToggle">Đăng nhập</button>
            <button type="button" class="toggle-btn" id="registerToggle">Đăng ký</button>
        </div>

        <div id="messageContainer"></div>

        <div class="loading" id="loading">
            <div class="spinner"></div>
            <p>Đang xử lý...</p>
        </div>

        <!-- Login Form -->
        <form id="loginForm" method="post" action="login">
            <div class="form-group">
                <label for="loginUsername">Tên đăng nhập:</label>
                <input type="text" id="loginUsername" name="username" required>
            </div>
            <div class="form-group">
                <label for="loginPassword">Mật khẩu:</label>
                <input type="password" id="loginPassword" name="password" required>
            </div>
            <button type="submit" class="auth-btn" id="loginBtn">Đăng nhập</button>
        </form>

        <!-- Register Form -->
        <form id="registerForm" class="hidden" method="post" action="register">
            <div class="form-group">
                <label for="registerFullName">Họ và tên:</label>
                <input type="text" id="registerFullName" name="fullName" required>
            </div>
            <div class="form-group">
                <label for="registerUsername">Tên đăng nhập:</label>
                <input type="text" id="registerUsername" name="username" required>
            </div>
            <div class="form-group">
                <label for="registerPassword">Mật khẩu:</label>
                <input type="password" id="registerPassword" name="password" required>
            </div>
            <div class="form-group">
                <label for="confirmPassword">Xác nhận mật khẩu:</label>
                <input type="password" id="confirmPassword" name="confirmPassword" required>
            </div>
            <button type="submit" class="auth-btn" id="registerBtn">Đăng ký</button>
        </form>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="js/utils.js"></script>
    <script src="js/auth.js"></script>
</body>
</html>