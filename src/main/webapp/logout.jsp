<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Invalidate session and show a brief logged-out page, then redirect
    session.invalidate();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng xuất</title>
    <link rel="stylesheet" href="css/base.css">
    <link rel="stylesheet" href="css/navbar.css">
    <link rel="stylesheet" href="css/components.css">
    <link rel="stylesheet" href="css/auth.css">
    <style>
        /* small override to center logout box on any layout */
        body { align-items: center; }
    </style>
</head>
<body>
    <div class="auth-container" style="text-align:center;">
        <div class="auth-header">
            <h1>Đã đăng xuất</h1>
            <p>Bạn đã đăng xuất thành công. Chuyển hướng về trang đăng nhập...</p>
        </div>

        <div id="messageContainer"></div>

        <div class="loading" id="loading">
            <div class="spinner"></div>
            <p>Đang chuyển hướng...</p>
        </div>

        <div style="margin-top:16px;">
            <a class="auth-btn" href="login.jsp">Đăng nhập lại</a>
        </div>
    </div>

    <script>
        // Redirect back to login after a short delay so user sees the message
        setTimeout(function() { window.location = 'login.jsp'; }, 1500);
    </script>
</body>
</html>