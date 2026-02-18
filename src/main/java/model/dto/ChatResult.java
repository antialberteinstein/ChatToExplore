package model.dto;

import java.io.DataInputStream;
import java.io.IOException;

public class ChatResult {
    private String username;
    private String userMessage;
    private int status; // 1: success, 0: pending, -1: error, -2: not found
    private String response;

    private boolean figureFlag;

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

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public static ChatResult receiveFrom(java.io.DataInputStream in) throws java.io.IOException {
        try {
            ChatResult r = new ChatResult();
            r.username = in.readUTF();
            r.userMessage = in.readUTF();
            r.status = in.readInt();
            r.response = in.readUTF();
            r.figureFlag = in.readBoolean();

            // ALWAYS read a FigureDTO (server always sends one)
            FigureDTO fig = FigureDTO.receiveFrom(in);
            r.figureDTO = fig;

            return r;
        } catch (java.io.EOFException eof) {
            throw new java.io.IOException("EOF while reading ChatResult", eof);
        }
    }
}