<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="model.bean.User" %>
<%
    User user = (User) request.getAttribute("user");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Thông tin cá nhân</title>
    <link rel="stylesheet" href="css/base.css">
    <link rel="stylesheet" href="css/auth.css">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        .profile-container {
            max-width: 820px;
            margin: 28px auto;
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 8px 30px rgba(17,24,39,0.06);
            padding: 28px 34px;
            font-family: system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial;
        }

        .profile-header {
            display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:18px;
        }

        .profile-title { font-size:20px; font-weight:700; color:#0f172a; }

        label { display:block; margin-top:12px; color:#334155; font-weight:600; }
        input[type="text"], input[type="password"], textarea { width:100%; padding:10px 12px; border:1px solid #e6edf3; border-radius:8px; margin-top:6px; font-size:14px; }
        textarea { min-height:100px; resize:vertical }

        .btn-row { display:flex; gap:12px; margin-top:16px; }
        .btn-primary { background: linear-gradient(90deg,#6366f1,#8b5cf6); color:#fff; border:none; padding:10px 14px; border-radius:8px; cursor:pointer; font-weight:700; }
        .btn-secondary { background:#f3f4f6; color:#0f172a; border:none; padding:10px 14px; border-radius:8px; cursor:pointer; }
        .btn-danger { background:#ef4444; color:#fff; border:none; padding:10px 14px; border-radius:8px; cursor:pointer; }

        .section { margin-top:16px; padding-top:8px; border-top:1px dashed #eef2ff; }

        .back-link { text-decoration:none; color:#334155; font-weight:600; background:transparent; border:1px solid #e6edf3; padding:8px 12px; border-radius:8px; }

        #message { margin-top:12px; font-weight:700; }
    </style>
</head>
<body>
<div class="profile-container">
    <div class="profile-header">
        <div class="profile-title">Thông tin tài khoản</div>
        <div>
            <button class="back-link" onclick="history.back()">← Quay lại</button>
        </div>
    </div>

    <form id="updateInfoForm">
        <label>Username</label>
        <input type="text" name="username" value="<%= user != null ? user.getUsername() : "" %>" disabled />

        <label>Họ và tên</label>
        <input type="text" id="fullName" name="fullName" value="<%= user != null ? user.getFullName() : "" %>" />

        <div class="btn-row">
            <button type="button" id="saveInfo" class="btn-primary">Lưu thông tin</button>
            <button type="button" class="btn-secondary" onclick="location.href='index.jsp'">Trang chính</button>
        </div>
    </form>

    <div class="section">
        <h3 style="margin:0 0 10px 0;">Đổi mật khẩu</h3>
        <form id="changePasswordForm">
            <label>Mật khẩu hiện tại</label>
            <input type="password" id="currentPassword" name="currentPassword" />

            <label>Mật khẩu mới</label>
            <input type="password" id="newPassword" name="newPassword" />

            <label>Xác nhận mật khẩu mới</label>
            <input type="password" id="confirmPassword" name="confirmPassword" />

            <div class="btn-row">
                <button type="button" id="changePassword" class="btn-danger">Đổi mật khẩu</button>
                <button type="button" class="btn-secondary" onclick="$('#changePasswordForm')[0].reset()">Hủy</button>
            </div>
        </form>
    </div>

    <div id="message"></div>
</div>

<script>
    $(function(){
        $('#saveInfo').click(function(){
            var fullName = $('#fullName').val();
            $.post('user-info', {action: 'updateInfo', fullName: fullName}, function(resp){
                try {
                    var j = typeof resp === 'string' ? JSON.parse(resp) : resp;
                    if (j.success) {
                        $('#message').css('color','green').text(j.message);
                    } else {
                        $('#message').css('color','red').text(j.message);
                    }
                } catch(e){
                    $('#message').css('color','red').text('Lỗi máy chủ');
                }
            });
        });

        $('#changePassword').click(function(){
            var current = $('#currentPassword').val();
            var next = $('#newPassword').val();
            var confirm = $('#confirmPassword').val();
            if (next !== confirm) {
                $('#message').css('color','red').text('Xác nhận mật khẩu không khớp');
                return;
            }
            $.post('user-info', {action: 'changePassword', currentPassword: current, newPassword: next}, function(resp){
                try {
                    var j = typeof resp === 'string' ? JSON.parse(resp) : resp;
                    if (j.success) {
                        $('#message').css('color','green').text(j.message);
                        $('#changePasswordForm')[0].reset();
                    } else {
                        $('#message').css('color','red').text(j.message);
                    }
                } catch(e){
                    $('#message').css('color','red').text('Lỗi máy chủ');
                }
            });
        });
    });
</script>

</body>
</html>
