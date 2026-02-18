package com.chat;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Chat request data class
 */
public class ChatRequest {
    public String username;
    public String systemPrompt;
    public Conversation conversation;

    public String userMessageAsKey;
    public String userMessage;
    public float temperature;

    public boolean figureFlag;
    
    public ChatRequest(String username, String systemPrompt, Conversation conversation, String userMessageAsKey, String userMessage, float temperature) {
        this.username = username;
        this.systemPrompt = systemPrompt;
        this.conversation = conversation;
        this.userMessageAsKey = userMessageAsKey;
        this.userMessage = userMessage;
        this.temperature = temperature;
        this.figureFlag = false;
    }

    public static String formatUserPrompt(String userQuestion) {
        StringBuilder b = new StringBuilder();
        b.append("<start_of_turn>user\n");
        b.append(userQuestion);
        b.append("<end_of_turn>\n");
        return b.toString();
    }

    public static String formatUserPrompt(String systemPrompt, String userQuestion) {
        StringBuilder b = new StringBuilder();
        b.append("<start_of_turn>user\n");
        b.append(systemPrompt + "\n\n");
        b.append(userQuestion);
        b.append("<end_of_turn>\n");
        return b.toString();
    }

    public static String formatBotResponse(String botResponse) {
        StringBuilder b = new StringBuilder();
        b.append("<start_of_turn>model\n");
        b.append(botResponse);
        b.append("<end_of_turn>\n");
        return b.toString();
    }

    // Turn này là turn cuối của bot và để trống cho Llama điền vào
    public static String formatBotResponse() {
        StringBuilder b = new StringBuilder();
        b.append("<start_of_turn>model\n");
        return b.toString();
    }

    public String format() {
        StringBuilder b = new StringBuilder();

        int messageCount = conversation.messages.size();

        // Nếu không có tin nhắn nào trong conversation history
        if (messageCount == 0) {
            // Chỉ bao gồm system prompt và tin nhắn user hiện tại
            b.append(formatUserPrompt(this.systemPrompt, this.userMessage));
            b.append(formatBotResponse()); // Chờ bot trả lời
            return b.toString();
        }

        for (int i = 0; i < messageCount; i++) {
            Message msg = conversation.messages.get(i);
            if (i == 0) {
                // Nếu là tin nhắn đầu tiên thì thêm system prompt vào
                b.append(formatUserPrompt(this.systemPrompt, msg.question));
            } else {
                // Các tin nhắn sau thì không cần system prompt
                b.append(formatUserPrompt(msg.question));
            }
            b.append(formatBotResponse(msg.response));
        }
        // Thêm tin nhắn user cuối cùng (chưa có response)
        b.append(formatUserPrompt(this.userMessage));
        b.append(formatBotResponse()); // Chờ bot trả lời
        return b.toString();
    }

    public static ChatRequest receiveBy(DataInputStream in) throws IOException {
        // Đọc username
        String username = in.readUTF();
        
        // Đọc system prompt
        String systemPrompt = in.readUTF();
        
        // Đọc số lượng messages trong conversation
        int messageCount = in.readInt();
        Conversation conversation = new Conversation();
        
        for (int i = 0; i < messageCount; i++) {
            String question = in.readUTF();
            String response = in.readUTF();
            conversation.messages.add(new Message(question, response));
        }
        
        // Đọc user message as key
        String userMessageAsKey = in.readUTF();

        // Đọc user message hiện tại
        String userMessage = in.readUTF();
        
        // Đọc temperature
        float temperature = in.readFloat();

        // Đọc figure flag
        boolean figureFlag = in.readBoolean();
        
        ChatRequest request = new ChatRequest(username, systemPrompt, conversation, userMessageAsKey, userMessage, temperature);

        request.figureFlag = figureFlag;

        return request;
    }

    /**
     * Mỗi message là một cặp question-response giữa user và bot
     */
    public static class Message {
        public String question;
        public String response;

        public Message(String question, String response) {
            this.question = question;
            this.response = response;
        }
    }

    /**
     * Conversation chứa lịch sử chat
     */
    public static class Conversation {
        // Không bao gồm system prompt.
        // Không bao gồm câu hỏi cuối cùng của user (đang chờ bot trả lời)
        public ArrayList<Message> messages;

        public Conversation() {
            this.messages = new ArrayList<>();
        }
    }
}
