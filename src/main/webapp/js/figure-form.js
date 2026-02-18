// Biến lưu URL ảnh đã upload
let uploadedImageUrl = null;

$(document).ready(function() {
    // Khởi tạo Drag & Drop listeners
    initDragAndDrop();

    // Đóng modal khi click bên ngoài
    $(window).on('click', function(event) {
        if (event.target.id === 'addFigureModal') {
            closeAddFigureModal();
        }
    });
});

/**
 * Hiển thị form thêm nhân vật
 */
function showAddFigureForm() {
    $('#addFigureModal').fadeIn(300);
    resetAddFigureForm();
    // Ensure initial state: only name and next button visible
    $('#additionalFields').hide();
    $('#nextBtn').show().prop('disabled', false).text('Tiếp theo');
    $('#submitBtn').hide().prop('disabled', false).text('Thêm nhân vật');
    // Ensure name field is editable at the start
    $('#figureName').prop('disabled', false);
}

/**
 * Đóng modal
 */
function closeAddFigureModal() {
    $('#addFigureModal').fadeOut(300);
    // re-enable name input when closing modal
    $('#figureName').prop('disabled', false);
}

/**
 * Reset form
 */
function resetAddFigureForm() {
    $('#addFigureForm')[0].reset();
    $('#imagePreview').hide();
    $('#modalAlert').hide().removeClass('alert-success alert-error');
    $('#uploadProgress').hide();
    $('#progressBarFill').css('width', '0%');
    uploadedImageUrl = null;
    // reset additional fields visibility
    $('#additionalFields').hide();
    $('#nextBtn').show();
    $('#submitBtn').hide();
    // Ensure name is enabled after reset
    $('#figureName').prop('disabled', false);
}

/**
 * Xử lý khi chọn ảnh
 */
function handleImageSelect(event) {
    const file = event.target.files[0];
    if (file) {
        // Hiển thị preview
        const reader = new FileReader();
        reader.onload = function(e) {
            $('#imagePreview').attr('src', e.target.result).show();
        };
        reader.readAsDataURL(file);

        // Upload ảnh ngay lập tức
        uploadImage(file);
    }
}

/**
 * Upload ảnh lên server
 */
function uploadImage(file) {
    const formData = new FormData();
    formData.append('image', file);

    $('#uploadProgress').show();
    $('#submitBtn').prop('disabled', true);

    $.ajax({
        url: 'upload-image',
        method: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        xhr: function() {
            const xhr = new window.XMLHttpRequest();
            xhr.upload.addEventListener('progress', function(evt) {
                if (evt.lengthComputable) {
                    const percentComplete = (evt.loaded / evt.total) * 100;
                    $('#progressBarFill').css('width', percentComplete + '%');
                }
            }, false);
            return xhr;
        },
        success: function(response) {
            console.log('Upload response:', response);
            $('#uploadProgress').hide();
            $('#submitBtn').prop('disabled', false);
            
            if (response.success) {
                // Keep absolute filesystem path for DB, but use web URL for immediate preview
                uploadedImageUrl = response.imagePath || response.imageUrl;
                if (response.imageUrl) {
                    $('#imagePreview').attr('src', response.imageUrl).show();
                }
                showModalAlert('Ảnh đã được tải lên thành công!', 'success');
            } else {
                showModalAlert(response.message || 'Lỗi khi upload ảnh', 'error');
                $('#imagePreview').hide();
            }
        },
        error: function(xhr, status, error) {
            console.error('Upload error:', error);
            $('#uploadProgress').hide();
            $('#submitBtn').prop('disabled', false);
            $('#imagePreview').hide();
            
            let message = 'Lỗi khi upload ảnh';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            showModalAlert(message, 'error');
        }
    });
}

/**
 * First step: check by name. If figure exists, backend will add the relation and return success.
 * If not exists, backend returns needsDetails=true and we reveal the additional fields for user input.
 */
function checkNameAndProceed() {
    const name = $('#figureName').val();
    if (!name || !name.trim()) { showModalAlert('Vui lòng nhập tên nhân vật', 'error'); return; }

    const trimmed = name.trim();

    $('#nextBtn').prop('disabled', true).text('Đang kiểm tra...');

    $.ajax({
        url: 'figure',
        method: 'POST',
        dataType: 'json',
        data: { action: 'checkByName', figureName: trimmed },
        success: function(res) {
            $('#nextBtn').prop('disabled', false).text('Tiếp theo');
            if (res && res.success) {
                showModalAlert('Đã thêm nhân vật vào danh sách của bạn', 'success');
                setTimeout(function() {
                    closeAddFigureModal();
                    loadFigures();
                }, 800);
            } else if (res && res.needsDetails) {
                // show full form for additional input
                $('#additionalFields').show();
                $('#nextBtn').hide();
                $('#submitBtn').show();
                // disable name field so user doesn't change it between steps
                $('#figureName').prop('disabled', true);
                // move focus to first additional field
                setTimeout(function() { $('#bornYear').focus(); }, 50);
            } else {
                showModalAlert((res && res.message) ? res.message : 'Lỗi khi kiểm tra tên', 'error');
            }
        },
        error: function(xhr) {
            $('#nextBtn').prop('disabled', false).text('Tiếp theo');
            showModalAlert('Lỗi kết nối server khi kiểm tra tên', 'error');
        }
    });
}

/**
 * Init Drag & drop support
 */
function initDragAndDrop() {
    const uploadArea = document.getElementById('imageUploadArea');
    
    if(!uploadArea) return;

    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, preventDefaults, false);
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    ['dragenter', 'dragover'].forEach(eventName => {
        uploadArea.addEventListener(eventName, highlight, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, unhighlight, false);
    });

    function highlight(e) {
        uploadArea.classList.add('dragover');
    }

    function unhighlight(e) {
        uploadArea.classList.remove('dragover');
    }

    uploadArea.addEventListener('drop', handleDrop, false);
    
    function handleDrop(e) {
        const dt = e.dataTransfer;
        const files = dt.files;
        
        if (files.length > 0) {
            const file = files[0];
            if (file.type.startsWith('image/')) {
                $('#imageFile')[0].files = files;
                handleImageSelect({ target: { files: files } });
            } else {
                showModalAlert('Vui lòng chọn file ảnh hợp lệ', 'error');
            }
        }
    }
}

/**
 * Submit form thêm nhân vật
 */
function submitAddFigure() {
    // Validate
    const name = $('#figureName').val().trim();
    const born = $('#bornYear').val().trim();
    const shortInfo = $('#shortInfo').val().trim();

    if (!name) { showModalAlert('Vui lòng nhập tên nhân vật', 'error'); return; }
    if (!born) { showModalAlert('Vui lòng nhập năm sinh', 'error'); return; }
    if (!shortInfo) { showModalAlert('Vui lòng nhập thông tin ngắn gọn', 'error'); return; }

    // Prepare data
    const died = $('#diedYear').val().trim();
    const hometown = $('#hometown').val().trim();
    const formData = {
        figureName: name,
        bornYear: born,
        diedYear: died || '',
        description: shortInfo,
        hometown: hometown || '',
        imageUrl: uploadedImageUrl || ''
    };

    // Disable submit button
    $('#submitBtn').prop('disabled', true).text('Đang xử lý...');

    // Submit via AJAX
    $.ajax({
        url: 'figure',
        method: 'POST',
        data: formData,
        dataType: 'json',
        success: function(response) {
            console.log('Add figure response:', response);
            
            if (response.success) {
                showModalAlert('Thêm nhân vật thành công!', 'success');
                setTimeout(function() {
                    closeAddFigureModal();
                    loadFigures(); // Reload list from timeline.js
                }, 1500);
            } else {
                showModalAlert(response.message || 'Lỗi khi thêm nhân vật', 'error');
                $('#submitBtn').prop('disabled', false).text('Thêm nhân vật');
            }
        },
        error: function(xhr, status, error) {
            console.error('Add figure error:', error);
            let message = 'Lỗi kết nối server';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            
            showModalAlert(message, 'error');
            $('#submitBtn').prop('disabled', false).text('Thêm nhân vật');
        }
    });
}

/**
 * Hiển thị thông báo trong modal
 */
function showModalAlert(message, type) {
    const $alert = $('#modalAlert');
    $alert.removeClass('alert-success alert-error')
          .addClass('alert-' + type)
          .text(message)
          .fadeIn();
    
    if (type === 'success') {
        setTimeout(function() {
            $alert.fadeOut();
        }, 3000);
    }
}

/**
 * Open edit info modal for a specific figure. Fetches current data and populates the form.
 */
function openEditInfoModal(figureId) {
    if (!figureId) return;
    $('#editInfoAlert').hide().text('');
    $('#editInfoFigureId').val(figureId);
    // fetch figure data
    $.ajax({
        url: 'figure',
        method: 'GET',
        dataType: 'json',
        data: { action: 'each', id: figureId },
        success: function(res) {
            if (res && res.success && res.figure) {
                const f = res.figure;
                $('#editFigureName').val(f.name || '');
                $('#editBornYear').val((f.born && f.born !== -1) ? f.born : '');
                $('#editDiedYear').val((f.died && f.died !== -1) ? f.died : '');
                $('#editHometown').val(f.hometown || '');
                $('#editShortInfo').val(f.shortInfo || '');
                $('#editInfoModal').show();
            } else {
                alert((res && res.message) ? res.message : 'Không thể tải thông tin nhân vật');
            }
        },
        error: function() {
            alert('Lỗi khi tải thông tin nhân vật');
        }
    });
}

function closeEditInfoModal() {
    $('#editInfoModal').hide();
}

function submitEditInfo() {
    const figureId = $('#editInfoFigureId').val();
    const name = $('#editFigureName').val().trim();
    const born = $('#editBornYear').val().trim();
    const died = $('#editDiedYear').val().trim();
    const hometown = $('#editHometown').val().trim();
    const description = $('#editShortInfo').val().trim();

    if (!figureId) { $('#editInfoAlert').text('Thiếu ID nhân vật').show(); return; }

    $('#editInfoSubmitBtn').prop('disabled', true).text('Đang lưu...');

    $.ajax({
        url: 'figure',
        method: 'POST',
        dataType: 'json',
        data: {
            action: 'updateInfo',
            figureId: figureId,
            figureName: name,
            bornYear: born,
            diedYear: died,
            hometown: hometown,
            description: description
        },
        success: function(res) {
            $('#editInfoSubmitBtn').prop('disabled', false).text('Lưu thay đổi');
            if (res && res.success) {
                $('#editInfoAlert').removeClass('alert-error').addClass('alert-success').text(res.message || 'Cập nhật thành công').show();
                setTimeout(function() {
                    closeEditInfoModal();
                    loadFigures();
                }, 800);
            } else {
                $('#editInfoAlert').removeClass('alert-success').addClass('alert-error').text(res.message || 'Lỗi khi cập nhật').show();
            }
        },
        error: function() {
            $('#editInfoSubmitBtn').prop('disabled', false).text('Lưu thay đổi');
            $('#editInfoAlert').removeClass('alert-success').addClass('alert-error').text('Lỗi kết nối server').show();
        }
    });
}

/**
 * Confirm and delete a figure for current user (only removes user_figures relation)
 */
function confirmDeleteFigure(figureId) {
    if (!figureId) return;
    var ok = confirm('Bạn có chắc muốn xóa nhân vật này khỏi danh sách của bạn? Hành động này có thể được hoàn tác bằng cách thêm lại.');
    if (!ok) return;

    // send delete request
    $.ajax({
        url: 'figure',
        method: 'POST',
        dataType: 'json',
        data: { action: 'removeUserFigure', figureId: figureId },
        success: function(res) {
            if (res && res.success) {
                alert(res.message || 'Đã xóa nhân vật khỏi danh sách');
                loadFigures();
            } else {
                alert((res && res.message) ? res.message : 'Không thể xóa nhân vật');
            }
        },
        error: function() {
            alert('Lỗi khi kết nối tới server để xóa nhân vật');
        }
    });
}