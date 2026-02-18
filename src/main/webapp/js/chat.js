let chatHistory = [];
let pollingIntervals = {};

/**
 * Toggle chat window
 */
function toggleChat() {
    $('#chatWindow').toggleClass('active');
    if ($('#chatWindow').hasClass('active')) {
        $('#chatInput').focus();
        loadChatHistory();
    }
}

/**
 * Load chat history from server
 */
function loadChatHistory() {
    $.ajax({
        url: 'chat?action=loadHistory',
        type: 'GET',
        dataType: 'json',
        success: function(response) {
            if (response.success && response.history) {
                chatHistory = [];
                $('#chatMessages').empty();
                
                response.history.forEach(msg => {
                    const sender = msg.role === 'user' ? 'user' : 'bot';
                    addChatMessage(msg.content, sender);
                    chatHistory.push({sender: sender, message: msg.content});
                });
                
                // Nếu tin nhắn cuối là user (lastMessage), bắt đầu polling lại
                if (response.history.length > 0 && response.history[response.history.length - 1].role === 'user') {
                    const lastUserMessage = response.history[response.history.length - 1].content;
                    $('#chatInput').prop('disabled', true);
                    $('#chatSend').prop('disabled', true);
                    
                    // Add typing indicator
                    const messageId = 'msg-' + Date.now();
                    const typingHtml = `<div class="chat-message bot" id="${messageId}">
                        <div class="message-avatar">🤖</div>
                        <div class="typing-indicator active">
                            <div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>
                        </div></div>`;
                    $('#chatMessages').append(typingHtml);
                    scrollChatToBottom();
                    
                    // Start polling
                    startPolling(lastUserMessage, messageId);
                }
                scrollChatToBottom();
            }
        },
        error: function(xhr, status, error) {
            console.error('Failed to load chat history:', error);
        }
    });
}

/**
 * Handle Enter key in chat input
 */
function handleChatKeypress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendChatMessage();
    }
}

/**
 * Send chat message
 */
function sendChatMessage() {
    let message = $('#chatInput').val().trim();
    if (!message) return;

    $('#chatInput').prop('disabled', true);
    $('#chatSend').prop('disabled', true);

    addChatMessage(message, 'user');
    $('#chatInput').val('');
    chatHistory.push({sender: 'user', message: message});

    const messageId = 'msg-' + Date.now();
    const typingHtml = `<div class="chat-message bot" id="${messageId}">
        <div class="message-avatar">🤖</div>
        <div class="typing-indicator active">
            <div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>
        </div></div>`;
    $('#chatMessages').append(typingHtml);
    scrollChatToBottom();

    $.ajax({
        url: 'chat?action=submit',
        type: 'GET',
        data: { message: message },
        contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                startPolling(message, messageId);
            } else {
                $('#' + messageId).remove();
                addChatMessage('Lỗi: ' + (response.message || 'Queue đầy'), 'bot');
                enableChatInput();
            }
        },
        error: function(xhr, status, error) {
            console.error('Submit error:', error);
            $('#' + messageId).remove();
            addChatMessage('Lỗi kết nối. Vui lòng thử lại sau.', 'bot');
            enableChatInput();
        }
    });
}

/**
 * Start polling for chat result
 */
function startPolling(userMessage, messageId) {
    let pollCount = 0;
    const maxPolls = 120; // 60 seconds
    
    const interval = setInterval(function() {
        pollCount++;
        $.ajax({
            url: 'chat?action=poll',
            type: 'GET',
            data: { message: userMessage },
            dataType: 'json',
            success: function(response) {
                if (response.success && response.result) {
                    const result = response.result;
                    if (result.status === 1) { // Success
                        clearInterval(interval);
                        $('#' + messageId).remove();
                        addChatMessage(result.response, 'bot');
                        chatHistory.push({sender: 'bot', message: result.response});
                        saveResult(userMessage, result.response);
                        enableChatInput();
                    } else if (result.status === -1) { // Error
                        clearInterval(interval);
                        $('#' + messageId).remove();
                        addChatMessage('Lỗi: ' + result.response, 'bot');
                        enableChatInput();
                    } else if (result.status === -2) { // Not found - keep polling
                        if (pollCount >= maxPolls) {
                            clearInterval(interval);
                            $('#' + messageId).remove();
                            addChatMessage('Timeout - không nhận được phản hồi', 'bot');
                            enableChatInput();
                        }
                    }
                } else {
                    if (pollCount >= maxPolls) {
                        clearInterval(interval);
                        $('#' + messageId).remove();
                        addChatMessage('Lỗi kết nối', 'bot');
                        enableChatInput();
                    }
                }
            },
            error: function(xhr, status, error) {
                console.error('Poll error:', error);
                if (pollCount >= maxPolls) {
                    clearInterval(interval);
                    $('#' + messageId).remove();
                    addChatMessage('Lỗi kết nối. Vui lòng thử lại sau.', 'bot');
                    enableChatInput();
                }
            }
        });
    }, 500);
    pollingIntervals[userMessage] = interval;
}

/**
 * Save successful result to DAO
 */
function saveResult(question, answer) {
    $.ajax({
        url: 'chat?action=save',
        type: 'GET',
        data: { question: question, answer: answer },
        dataType: 'json',
        error: function(xhr, status, error) { console.error('Save error:', error); }
    });
}

/**
 * Enable chat input
 */
function enableChatInput() {
    $('#chatInput').prop('disabled', false).focus();
    $('#chatSend').prop('disabled', false);
}

/**
 * Add message to chat
 */
function addChatMessage(message, sender) {
    const isUser = sender === 'user';
    const avatar = isUser ? '👤' : '🤖';
    let content;
    
    if (isUser) {
        content = escapeHtml(message);
    } else {
        marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false });
        content = marked.parse(message);
    }
    
    const messageHtml = `<div class="chat-message ${sender}">
        <div class="message-avatar">${avatar}</div>
        <div class="message-bubble">${content}</div>
    </div>`;
    $('#chatMessages').append(messageHtml);
    scrollChatToBottom();
}

/**
 * Scroll chat to bottom
 */
function scrollChatToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    if(chatMessages) chatMessages.scrollTop = chatMessages.scrollHeight;
}