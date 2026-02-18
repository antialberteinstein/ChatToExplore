// auth.js - handles login/register UI and AJAX actions for auth views
if (typeof jQuery === 'undefined') {
    console && console.warn('jQuery not loaded before auth.js');
}

$(document).ready(function() {
    // Toggle between login and register forms
    $('#loginToggle').click(function() {
        $('#loginToggle').addClass('active');
        $('#registerToggle').removeClass('active');
        $('#loginForm').removeClass('hidden');
        $('#registerForm').addClass('hidden');
        clearMessages();
    });

    $('#registerToggle').click(function() {
        $('#registerToggle').addClass('active');
        $('#loginToggle').removeClass('active');
        $('#registerForm').removeClass('hidden');
        $('#loginForm').addClass('hidden');
        clearMessages();
    });

    // Handle login form submission
    $('#loginForm').submit(function(e) {
        e.preventDefault();

        const username = $('#loginUsername').val().trim();
        const password = $('#loginPassword').val().trim();

        if (!username || !password) {
            showMessage('Vui lòng nhập đầy đủ thông tin', 'error');
            return;
        }

        performLogin(username, password);
    });

    // Handle register form submission
    $('#registerForm').submit(function(e) {
        e.preventDefault();

        const fullName = $('#registerFullName').val().trim();
        const username = $('#registerUsername').val().trim();
        const password = $('#registerPassword').val().trim();
        const confirmPassword = $('#confirmPassword').val().trim();

        if (!fullName) {
            showMessage('Vui lòng nhập họ và tên', 'error');
            $('#registerFullName').focus();
            return;
        }

        if (!username) {
            showMessage('Vui lòng nhập tên đăng nhập', 'error');
            $('#registerUsername').focus();
            return;
        }

        if (username.length < 3) {
            showMessage('Tên đăng nhập phải có ít nhất 3 ký tự', 'error');
            $('#registerUsername').focus().select();
            return;
        }

        if (!/^[a-zA-Z0-9_]+$/.test(username)) {
            showMessage('Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới', 'error');
            $('#registerUsername').focus().select();
            return;
        }

        if (!password) {
            showMessage('Vui lòng nhập mật khẩu', 'error');
            $('#registerPassword').focus();
            return;
        }

        if (password.length < 6) {
            showMessage('Mật khẩu phải có ít nhất 6 ký tự', 'error');
            $('#registerPassword').focus().select();
            return;
        }

        if (!confirmPassword) {
            showMessage('Vui lòng xác nhận mật khẩu', 'error');
            $('#confirmPassword').focus();
            return;
        }

        if (password !== confirmPassword) {
            showMessage('Mật khẩu xác nhận không khớp', 'error');
            $('#confirmPassword').focus().select();
            return;
        }

        performRegister(fullName, username, password);
    });
});

function performLogin(username, password) {
    showLoading();

    $.ajax({
        url: 'login',
        method: 'POST',
        data: {
            username: username,
            password: password
        },
        success: function(response) {
            hideLoading();
            // Try parse JSON if returned as string
            let res = response;
            try { if (typeof response === 'string') res = JSON.parse(response); } catch (e) {}

            if (res && (res.redirect || res.success)) {
                window.location = res.redirect || 'index.jsp';
                return;
            }

            // Fallback: if server returned plain HTML or message
            showMessage(res && res.message ? res.message : 'Đăng nhập thất bại', 'error');
        },
        error: function(xhr) {
            hideLoading();
            const text = xhr && xhr.responseText ? xhr.responseText : 'Lỗi máy chủ';
            showMessage(text, 'error');
        }
    });
}

function performRegister(fullName, username, password) {
    showLoading();

    $.ajax({
        url: 'register',
        method: 'POST',
        data: {
            fullName: fullName,
            username: username,
            password: password
        },
        success: function(response) {
            hideLoading();
            let res = response;
            try { if (typeof response === 'string') res = JSON.parse(response); } catch (e) {}

            if (res && res.success) {
                showMessage(res.message || 'Đăng ký thành công. Vui lòng đăng nhập.', 'success');
                // switch to login tab
                $('#loginToggle').addClass('active');
                $('#registerToggle').removeClass('active');
                $('#loginForm').removeClass('hidden');
                $('#registerForm').addClass('hidden');
                return;
            }

            showMessage(res && res.message ? res.message : 'Đăng ký thất bại', 'error');
        },
        error: function(xhr) {
            hideLoading();
            const text = xhr && xhr.responseText ? xhr.responseText : 'Lỗi máy chủ';
            showMessage(text, 'error');
        }
    });
}

function showMessage(message, type) {
    const messageHtml = '<div class="message ' + type + '">' + message + '</div>';
    $('#messageContainer').html(messageHtml).show();
    setTimeout(function() {
        // keep visible; optionally scroll
    }, 100);
}

function clearMessages() {
    $('#messageContainer').empty();
}

function showLoading() {
    $('#loading').show();
    $('#loginBtn, #registerBtn').prop('disabled', true);
}

function hideLoading() {
    $('#loading').hide();
    $('#loginBtn, #registerBtn').prop('disabled', false);
}
