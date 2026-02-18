package com.chat;

import java.io.DataOutputStream;
import java.io.IOException;

public class ChatResult {
    private String username;
    private String userMessage;
    private int status; // 1: success, 0: pending, -1: error
    private String response;

    private boolean figureFlag = false;

    // Nếu có thể, sẽ trả ChatResult kèm FigureDTO
    private FigureDTO figureDTO = null;

    public void setFigureDTO(FigureDTO figureDTO) {
        this.figureDTO = figureDTO;
    }

    public FigureDTO getFigureDTO() {
        return figureDTO;
    }

    public boolean isFigureFlag() {
        return figureFlag;
    }

    public void setFigureFlag(boolean figureFlag) {
        this.figureFlag = figureFlag;
    }

    public String getUsername() {
        return username;
    }

    public String getUserMessage() {
        return userMessage;
    }
    
    public int getStatus() {
        return status;
    }

    public String getResponse() {
        return response;
    }

    public static ChatResult success(ChatRequest request, String response) {
        ChatResult result = new ChatResult();
        result.username = request.username;
        result.userMessage = request.userMessageAsKey;
        result.status = 1;
        result.response = response;
        return result;
    }

    public static ChatResult pending(ChatRequest request) {
        ChatResult result = new ChatResult();
        result.username = request.username;
        result.userMessage = request.userMessageAsKey;
        result.status = 0;
        result.response = "";
        return result;
    }

    public static ChatResult error(ChatRequest request, String errorMessage) {
        ChatResult result = new ChatResult();
        result.username = request.username;
        result.userMessage = request.userMessageAsKey;
        result.status = -1;
        result.response = errorMessage;
        return result;
    }

    public void sendBy(DataOutputStream out) throws IOException {
        out.writeUTF(username);
        out.writeUTF(userMessage);
        out.writeInt(status);
        out.writeUTF(response);
        out.writeBoolean(figureFlag);

        if (figureDTO != null) {
            // Gửi thêm FigureDTO nếu có
            figureDTO.isEmpty = false;
            figureDTO.sendBy(out);
        } else {
            // Gửi FigureDTO rỗng nếu không có
            FigureDTO emptyDTO = new FigureDTO();
            emptyDTO.isEmpty = true;
            emptyDTO.sendBy(out);
        }
        out.flush();
    }
}
