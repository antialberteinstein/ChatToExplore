package model.dto;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Chat request DTO for sending to TCP server
 */
public class ChatRequest {
    public String username;
    public String systemPrompt;
    public Conversation conversation;

    // Biến này dùng làm
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

    /**
     * Gửi ChatRequest qua DataOutputStream (binary protocol)
     */
    public void sendBy(DataOutputStream out) throws IOException {
        // Gửi username
        out.writeUTF(username);
        
        // Gửi system prompt
        out.writeUTF(systemPrompt);
        
        // Gửi số lượng messages trong conversation
        int messageCount = conversation.messages.size();
        out.writeInt(messageCount);
        
        // Gửi từng message (question + response)
        for (Message msg : conversation.messages) {
            out.writeUTF(msg.question);
            out.writeUTF(msg.response);
        }
        
        // Gửi user message as key
        out.writeUTF(userMessageAsKey);

        // Gửi user message hiện tại
        out.writeUTF(userMessage);
        
        // Gửi temperature
        out.writeFloat(temperature);

        // Gửi figure flag
        out.writeBoolean(figureFlag);
        
        out.flush();
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
