/**
 * Load tất cả figures của user từ server
 */
function loadFigures() {
    // Hiển thị loading
    $('#loadingState').show();
    $('#errorState').hide();
    $('#emptyState').hide();
    $('#timelineItems').empty();

    $.ajax({
        url: 'figure?action=all',
        method: 'GET',
        dataType: 'json',
        success: function(response) {
            console.log('Figures loaded:', response);
            $('#loadingState').hide();
            
            if (response.success) {
                if (response.figures && response.figures.length > 0) {
                    renderFigures(response.figures);
                } else {
                    $('#emptyState').show();
                }
            } else {
                showError(response.message || 'Không thể tải danh sách nhân vật');
            }
        },
        error: function(xhr, status, error) {
            console.error('Error loading figures:', error);
            $('#loadingState').hide();
            
            let message = 'Lỗi kết nối server';
            if (xhr.status === 401 || xhr.status === 403) {
                message = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
                setTimeout(function() {
                    window.location.href = 'login.jsp';
                }, 2000);
            } else if (xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            }
            
            showError(message);
        }
    });
}

/**
 * Render danh sách figures ra timeline
 */
function renderFigures(figures) {
    const $container = $('#timelineItems');
    $container.empty();

    // Sắp xếp theo năm sinh (những nhân vật không rõ năm sinh -1 sẽ được đưa xuống cuối)
    figures.sort(function(a, b) {
        const yearA = (typeof a.born === 'number' && a.born >= 0) ? a.born : Number.MAX_SAFE_INTEGER;
        const yearB = (typeof b.born === 'number' && b.born >= 0) ? b.born : Number.MAX_SAFE_INTEGER;
        return yearA - yearB;
    });

    let previousYear = null;
    
    figures.forEach(function(figure, index) {
        const isLeft = index % 2 === 0;
        const marginTop = previousYear ? calculateMargin(previousYear, figure.born) : 0;
        
        const figureHtml = createFigureElement(figure, isLeft, marginTop);
        $container.append(figureHtml);
        
        previousYear = figure.born;
    });
}

/**
 * Tính khoảng cách giữa các figures (1 năm = 10px)
 */
function calculateMargin(previousYear, currentYear) {
    const yearDiff = currentYear - previousYear;
    return yearDiff * 10; 
}

/**
 * Tạo HTML element cho một figure
 */
function createFigureElement(figure, isLeft, marginTop) {
    const contentClass = isLeft ? 'timeline-content-left' : 'timeline-content-right';
    // Nếu born hoặc died = -1 => hiển thị "Không rõ"
    let period;
    if (figure.born === -1 || figure.died === -1) {
        period = 'Không rõ';
    } else if (figure.born && figure.died) {
        period = figure.born + ' - ' + figure.died;
    } else if (figure.born) {
        period = String(figure.born);
    } else {
        period = 'Không rõ';
    }

    // If stored imageUrl is an absolute filesystem path (e.g. /Users/..../FinalProject/images/<file>),
    // convert it to the served path `/images/<file>` which is handled by ImageServeServlet.
    let imageUrl = 'images/' + slugify(figure.name) + '.png';
    if (figure.imageUrl && String(figure.imageUrl).trim().length > 0) {
        const stored = String(figure.imageUrl).trim();
        // detect absolute filesystem path by presence of '/FinalProject/' or starting with '/' (Unix) or backslash (Windows)
        if (stored.indexOf('/FinalProject/') !== -1 || stored.indexOf('FinalProject') !== -1 || stored.indexOf('\\') !== -1 || stored.startsWith('/')) {
            // Prefer the global resolver defined in index.jsp which maps filesystem paths -> web URLs
            if (typeof resolveImageUrl === 'function') {
                imageUrl = resolveImageUrl(stored);
            } else {
                // fallback: extract filename and build URL relative to app context
                var parts = stored.split(/\\/g).pop().split('/');
                var fname = parts[parts.length - 1];
                if (fname) {
                    var origin = (typeof APP_ORIGIN !== 'undefined') ? APP_ORIGIN : (window.location.origin || (window.location.protocol + '//' + window.location.host));
                    var context = (typeof APP_CONTEXT !== 'undefined') ? APP_CONTEXT : (function(){
                        var pathParts = window.location.pathname.split('/').filter(function(p){ return p && p.length>0; });
                        return pathParts.length > 0 ? '/' + pathParts[0] : '';
                    })();
                    imageUrl = origin + context + '/images/' + fname;
                }
            }
        } else {
            imageUrl = stored;
        }
    }
    const safeName = escapeHtml(figure.name || 'Không rõ');
    const safeInfo = (figure.shortInfo == null || figure.shortInfo === '') ? 'Không rõ' : escapeHtml(figure.shortInfo);
    const safeHometown = (figure.hometown == null || String(figure.hometown).trim() === '') ? 'Không rõ' : escapeHtml(figure.hometown);

    // Determine year marker position class based on card side: show year on opposite side of the card
    const yearClass = isLeft ? 'year-marker right' : 'year-marker left';
    let html = '<div class="timeline-item" data-id="' + figure.id + '" style="margin-top: ' + marginTop + 'px;">';
    html += '<div class="' + yearClass + '">' + (figure.born === -1 ? 'Không rõ' : escapeHtml(figure.born)) + '</div>';
    html += '<div class="timeline-dot"></div>';
    html += '<div class="' + contentClass + '">';
    html += '<img src="' + imageUrl + '" alt="' + safeName + '" class="person-image" onerror="this.style.display=\'none\'">';
    html += '<h3 class="timeline-title">' + safeName + '</h3>';
    html += '<div class="timeline-date">' + period + '</div>';
    html += '<p class="timeline-description">' + safeInfo + '</p>';
    // Hiển thị quê quán; nếu không có thì ghi 'Không rõ'
    html += '<p style="margin-top: 10px; font-size: 13px; color: #666;">📍 Quê quán: ' + safeHometown + '</p>';
    // Edit image button placed bottom-right of the card (textual, large)
    // Delete button (top-right)
    html += '<button class="delete-figure-btn" data-id="' + figure.id + '" onclick="confirmDeleteFigure(\'' + escapeHtml(String(figure.id)) + '\')" title="Xóa nhân vật">🗑️</button>';
    html += '<button class="edit-image-btn" data-id="' + figure.id + '" onclick="openEditImageModal(\'' + escapeHtml(String(figure.id)) + '\')" title="Chỉnh sửa ảnh">[Sửa ảnh]</button>';
    html += '<button class="edit-info-btn" data-id="' + figure.id + '" onclick="openEditInfoModal(\'' + escapeHtml(String(figure.id)) + '\')" title="Chỉnh sửa thông tin" style="margin-left:8px;">[Chỉnh sửa thông tin]</button>';
    html += '</div>';
    html += '</div>';
    
    return html;
}