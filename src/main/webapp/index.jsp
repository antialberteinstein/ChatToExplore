<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Kiểm tra session - bắt buộc phải đăng nhập
    String username = (String) session.getAttribute("username");
    String fullName = (String) session.getAttribute("fullName");
    if (username == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ChatToExplore</title>
    
    <link rel="stylesheet" href="css/base.css">
    <link rel="stylesheet" href="css/navbar.css">
    <link rel="stylesheet" href="css/components.css">
    <link rel="stylesheet" href="css/timeline.css">
    <link rel="stylesheet" href="css/modal-form.css">
    <link rel="stylesheet" href="css/chat.css">
    <style>
        /* Styles for edit image button placement */
        .timeline-item { position: relative; }
        .timeline-content-left, .timeline-content-right { position: relative; overflow: visible; }
        .edit-image-btn { position: absolute; right: 12px; bottom: 12px; background: #007bff; color: white; border: 0; padding:10px 14px; border-radius:8px; cursor:pointer; box-shadow:0 4px 10px rgba(0,0,0,0.15); font-weight:700; font-size:14px; z-index: 30; }
        .edit-image-btn:hover { background:#0056b3; }
        .edit-image-btn:focus { outline: none; box-shadow:0 0 0 3px rgba(0,123,255,0.2); }
        .edit-info-btn { position: absolute; right: 96px; bottom: 12px; background: #28a745; color: white; border: 0; padding:9px 12px; border-radius:8px; cursor:pointer; box-shadow:0 4px 10px rgba(0,0,0,0.12); font-weight:700; font-size:13px; z-index: 29; }
        .edit-info-btn:hover { background:#1e7e34; }
        .edit-info-btn:focus { outline: none; box-shadow:0 0 0 3px rgba(40,167,69,0.15); }
        .delete-figure-btn { position: absolute; right: 12px; top: 12px; background: #dc3545; color: white; border: 0; padding:6px 8px; border-radius:8px; cursor:pointer; box-shadow:0 4px 8px rgba(0,0,0,0.12); font-weight:700; font-size:14px; z-index: 35; }
        .delete-figure-btn:hover { background:#b02a37; }
        .delete-figure-btn:focus { outline: none; box-shadow:0 0 0 3px rgba(220,53,69,0.12); }
        /* ensure button doesn't overlap timeline dot */
        .timeline-dot { z-index: 20; }
    </style>
</head>
<body>
    <div class="navbar">
        <div class="nav-container">
            <div class="nav-brand">ChatToExplore</div>
            <div class="nav-user">
                <a href="user-info" class="profile-link">Thông tin cá nhân</a>
                <a href="logout.jsp" class="logout-btn">Đăng xuất</a>
            </div>
        </div>
    </div>

    <div class="main-content">
        <div class="welcome-section">
            <p>Khám phá cuộc đời của những danh nhân vĩ đại trong lịch sử dân tộc</p>
        </div>
        
        <div class="timeline-container">
            <div class="timeline-line"></div>

            <div id="loadingState" class="loading">
                <div class="spinner"></div>
                <p>Đang tải danh sách nhân vật...</p>
            </div>

            <div id="errorState" class="error-message" style="display: none;">
                <h3>⚠️ Có lỗi xảy ra</h3>
                <p id="errorMessage"></p>
                <button onclick="loadFigures()" style="margin-top: 15px; padding: 10px 20px; background: white; color: #dc3545; border: none; border-radius: 5px; cursor: pointer; font-weight: bold;">Thử lại</button>
            </div>

            <div id="emptyState" class="empty-state" style="display: none;">
                <h3>📝 Chưa có nhân vật nào</h3>
                <p>Bạn chưa thêm nhân vật lịch sử nào. Hãy thêm nhân vật đầu tiên của bạn!</p>
            </div>

            <div class="timeline-items" id="timelineItems">
                </div>
        </div>
    </div>

    <button class="add-figure-btn" onclick="showAddFigureForm()" title="Thêm nhân vật mới">+</button>

    <button class="chat-button" onclick="toggleChat()" title="Chat với AI">💬</button>

    <div id="chatWindow" class="chat-window">
        <div class="chat-header">
            <h3>🤖 Trợ lý lịch sử</h3>
            <button class="chat-close" onclick="toggleChat()">×</button>
        </div>
        <div class="chat-messages" id="chatMessages">
            <div class="chat-message bot">
                <div class="message-avatar">🤖</div>
                <div class="message-bubble">
                    Xin chào! Tôi là trợ lý AI chuyên về lịch sử Việt Nam. Bạn có muốn hỏi gì không?
                </div>
            </div>
        </div>
        <div class="chat-input-container">
            <input type="text" class="chat-input" id="chatInput" placeholder="Nhập câu hỏi..." onkeypress="handleChatKeypress(event)">
            <button class="chat-send" id="chatSend" onclick="sendChatMessage()">➤</button>
        </div>
    </div>

    <div id="addFigureModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Thêm nhân vật mới</h2>
                <span class="close" onclick="closeAddFigureModal()">&times;</span>
            </div>
            <div class="modal-body">
                <div id="modalAlert" class="alert"></div>
                
                <form id="addFigureForm" enctype="multipart/form-data">
                    <div class="form-group">
                        <label>Tên nhân vật <span class="required">*</span></label>
                        <input type="text" id="figureName" name="figureName" required placeholder="Ví dụ: Trần Hưng Đạo">
                    </div>

                    <!-- Additional fields hidden initially; shown only when figure name is not found -->
                    <div id="additionalFields" style="display:none;">
                        <div class="form-row">
                            <div class="form-group">
                                <label>Năm sinh <span class="required">*</span></label>
                                <input type="number" id="bornYear" name="bornYear" placeholder="Ví dụ: 1228">
                            </div>
                            <div class="form-group">
                                <label>Năm mất</label>
                                <input type="number" id="diedYear" name="diedYear" placeholder="Ví dụ: 1300">
                            </div>
                        </div>

                        <div class="form-group">
                            <label>Quê quán</label>
                            <input type="text" id="hometown" name="hometown" placeholder="Ví dụ: Nam Định">
                        </div>

                        <div class="form-group">
                            <label>Thông tin ngắn gọn <span class="required">*</span></label>
                            <textarea id="shortInfo" name="description" placeholder="Mô tả ngắn gọn về nhân vật..."></textarea>
                        </div>

                        <div class="form-group">
                            <label>Ảnh nhân vật</label>
                            <div class="image-upload-area" id="imageUploadArea" onclick="document.getElementById('imageFile').click()">
                                <div class="upload-icon">📷</div>
                                <p><strong>Nhấp để chọn ảnh</strong> hoặc kéo thả ảnh vào đây</p>
                                <p style="font-size: 12px; color: #666; margin-top: 5px;">JPG, PNG, GIF, WEBP (tối đa 10MB)</p>
                                <img id="imagePreview" class="image-preview" alt="Preview">
                                <input type="file" id="imageFile" name="image" accept="image/*" style="display: none;" onchange="handleImageSelect(event)">
                            </div>
                            <div class="upload-progress" id="uploadProgress">
                                <div class="progress-bar">
                                    <div class="progress-bar-fill" id="progressBarFill"></div>
                                </div>
                                <p style="text-align: center; font-size: 12px; margin-top: 5px;">Đang tải lên...</p>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeAddFigureModal()">Hủy</button>
                <button type="button" class="btn btn-primary" id="nextBtn" onclick="checkNameAndProceed()">Tiếp theo</button>
                <button type="button" class="btn btn-primary" id="submitBtn" onclick="submitAddFigure()" style="display:none;">Thêm nhân vật</button>
            </div>
        </div>
    </div>

    <!-- Edit Image Modal -->
    <div id="editImageModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Chỉnh sửa ảnh nhân vật</h2>
                <span class="close" onclick="closeEditImageModal()">&times;</span>
            </div>
            <div class="modal-body">
                <div id="editModalAlert" class="alert"></div>
                <form id="editImageForm" enctype="multipart/form-data">
                    <input type="hidden" id="editFigureId" name="figureId">
                    <div class="form-group">
                        <label>Chọn ảnh mới</label>
                        <div class="image-upload-area" onclick="document.getElementById('editImageFile').click()">
                            <div class="upload-icon">📷</div>
                            <p><strong>Nhấp để chọn ảnh</strong> hoặc kéo thả vào đây</p>
                            <img id="editImagePreview" class="image-preview" alt="Preview">
                            <input type="file" id="editImageFile" name="image" accept="image/*" style="display: none;" onchange="handleEditImageSelect(event)">
                        </div>
                        <div class="upload-progress" id="editUploadProgress" style="display:none;">
                            <div class="progress-bar">
                                <div class="progress-bar-fill" id="editProgressBarFill"></div>
                            </div>
                            <p style="text-align: center; font-size: 12px; margin-top: 5px;">Đang tải lên...</p>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeEditImageModal()">Hủy</button>
                <button type="button" class="btn btn-primary" id="editSubmitBtn" onclick="submitEditImage()">Cập nhật ảnh</button>
            </div>
        </div>
    </div>

    <!-- Edit Info Modal -->
    <div id="editInfoModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Chỉnh sửa thông tin nhân vật</h2>
                <span class="close" onclick="closeEditInfoModal()">&times;</span>
            </div>
            <div class="modal-body">
                <div id="editInfoAlert" class="alert"></div>
                <form id="editInfoForm">
                    <input type="hidden" id="editInfoFigureId" name="figureId">
                    <div class="form-group">
                        <label>Tên nhân vật</label>
                        <input type="text" id="editFigureName" name="figureName" placeholder="Tên nhân vật">
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label>Năm sinh</label>
                            <input type="number" id="editBornYear" name="bornYear" placeholder="Ví dụ: 1228">
                        </div>
                        <div class="form-group">
                            <label>Năm mất</label>
                            <input type="number" id="editDiedYear" name="diedYear" placeholder="Ví dụ: 1300">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Quê quán</label>
                        <input type="text" id="editHometown" name="hometown" placeholder="Ví dụ: Nam Định">
                    </div>
                    <div class="form-group">
                        <label>Thông tin ngắn gọn</label>
                        <textarea id="editShortInfo" name="description" placeholder="Mô tả ngắn gọn..."></textarea>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeEditInfoModal()">Hủy</button>
                <button type="button" class="btn btn-primary" id="editInfoSubmitBtn" onclick="submitEditInfo()">Lưu thay đổi</button>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>

    <script src="js/utils.js"></script>         <script src="js/timeline.js"></script>      <script src="js/figure-form.js"></script>   <script src="js/chat.js"></script>          <script>
    // App context helpers used by client scripts
    var APP_ORIGIN = window.location.origin;
    var APP_CONTEXT = '<%= request.getContextPath() %>';
    /**
     * Resolve an absolute filesystem image path (e.g. /Users/nhat/FinalProject/images/xxx.jpg)
     * to a web-accessible URL under the webapp `/images/<filename>` path.
     * If the input already looks like a URL, it is returned unchanged.
     */
    function resolveImageUrl(stored) {
        if (!stored) return stored;
        try {
            var s = String(stored).trim();
            // If it's already a web URL (http, https) or a relative path, return as-is
            if (/^https?:\/\//i.test(s) || /^\//.test(s) && s.indexOf('/images/') === 0) {
                return s;
            }

            // Detect common absolute filesystem patterns and extract filename
            if (s.indexOf('/FinalProject/') !== -1 || s.indexOf('FinalProject') !== -1 || /^[A-Za-z]:\\/.test(s) || s.startsWith('/')) {
                var fname = s.replace(/.*[\\\/]/, '');
                if (fname) {
                    return APP_ORIGIN + APP_CONTEXT + '/images/' + fname;
                }
            }
        } catch (e) {
            console.error('resolveImageUrl error', e);
        }
        return stored;
    }
        // Entry point with safety: call loadFigures if available and catch errors
        $(document).ready(function() {
            try {
                if (typeof loadFigures === 'function') {
                    loadFigures();
                } else {
                    console.error('loadFigures is not defined');
                    $('#loadingState').hide();
                    $('#errorState').show();
                    $('#errorMessage').text('Lỗi client: không thể tải giao diện');
                }
            } catch (e) {
                console.error('Error during loadFigures:', e);
                $('#loadingState').hide();
                $('#errorState').show();
                $('#errorMessage').text('Lỗi client: ' + (e && e.message ? e.message : 'Không xác định'));
            }

            // Safety timeout: if still loading after 7s, show generic error to avoid infinite spinner
            setTimeout(function() {
                if ($('#loadingState').is(':visible')) {
                    $('#loadingState').hide();
                    $('#errorState').show();
                    $('#errorMessage').text('Máy chủ không phản hồi. Vui lòng thử lại sau.');
                }
            }, 7000);
        });

        // Styles moved to <head> style block to avoid runtime string injection issues

        function openEditImageModal(figureId) {
            document.getElementById('editFigureId').value = figureId;
            document.getElementById('editImagePreview').style.display = 'none';
            document.getElementById('editImageFile').value = '';
            document.getElementById('editModalAlert').innerHTML = '';
            document.getElementById('editImageModal').style.display = 'block';
        }

        function closeEditImageModal() {
            document.getElementById('editImageModal').style.display = 'none';
        }

        function handleEditImageSelect(event) {
            var file = event.target.files[0];
            if (!file) return;
            var reader = new FileReader();
            reader.onload = function(e) {
                var img = document.getElementById('editImagePreview');
                img.src = e.target.result;
                img.style.display = 'block';
            };
            reader.readAsDataURL(file);
        }

        function submitEditImage() {
            var figureId = document.getElementById('editFigureId').value;
            var fileInput = document.getElementById('editImageFile');
            if (!figureId) {
                document.getElementById('editModalAlert').innerText = 'Thiếu ID nhân vật';
                return;
            }

            if (!fileInput.files || !fileInput.files[0]) {
                document.getElementById('editModalAlert').innerText = 'Vui lòng chọn ảnh trước khi cập nhật';
                return;
            }

            // First upload to /upload-image
            var fd = new FormData();
            fd.append('image', fileInput.files[0]);

            var xhr = new XMLHttpRequest();
            xhr.open('POST', 'upload-image', true);
            xhr.upload.onprogress = function(e) {
                if (e.lengthComputable) {
                    var pct = Math.round((e.loaded / e.total) * 100);
                    document.getElementById('editUploadProgress').style.display = 'block';
                    document.getElementById('editProgressBarFill').style.width = pct + '%';
                }
            };
            xhr.onload = function() {
                try {
                    var res = JSON.parse(xhr.responseText);
                    if (res.success && (res.imageUrl || res.imagePath)) {
                        // Now tell figure servlet to update DB with the absolute filesystem path
                        // (server-side path is returned as `imagePath`); use web URL for preview only
                        var fd2 = new FormData();
                        fd2.append('action', 'updateImage');
                        fd2.append('figureId', figureId);
                        // Prefer the absolute filesystem path for DB storage
                        fd2.append('imageUrl', res.imagePath || res.imageUrl);

                        var xhr2 = new XMLHttpRequest();
                        xhr2.open('POST', 'figure', true);
                        xhr2.onload = function() {
                            try {
                                var r2 = JSON.parse(xhr2.responseText);
                                if (r2.success) {
                                    closeEditImageModal();
                                    loadFigures();
                                } else {
                                    document.getElementById('editModalAlert').innerText = r2.message || 'Lỗi khi cập nhật ảnh';
                                }
                            } catch (e) {
                                document.getElementById('editModalAlert').innerText = 'Lỗi server khi cập nhật ảnh';
                            }
                            document.getElementById('editUploadProgress').style.display = 'none';
                            document.getElementById('editProgressBarFill').style.width = '0%';
                        };
                        xhr2.onerror = function() {
                            document.getElementById('editModalAlert').innerText = 'Lỗi khi kết nối tới server (cập nhật)';
                        };
                        xhr2.send(fd2);
                    } else {
                        document.getElementById('editModalAlert').innerText = res.message || 'Lỗi khi upload ảnh';
                        document.getElementById('editUploadProgress').style.display = 'none';
                        document.getElementById('editProgressBarFill').style.width = '0%';
                    }
                } catch (e) {
                    document.getElementById('editModalAlert').innerText = 'Lỗi server khi upload ảnh';
                    document.getElementById('editUploadProgress').style.display = 'none';
                    document.getElementById('editProgressBarFill').style.width = '0%';
                }
            };
            xhr.onerror = function() {
                document.getElementById('editModalAlert').innerText = 'Lỗi khi kết nối tới server (upload)';
            };
            xhr.send(fd);
        }
    </script>
</body>
</html>