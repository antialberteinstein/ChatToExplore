package com.chat;

import de.kherud.llama.*;

import java.io.IOException;

/**
 * Xử lý chat với Llama model
 */
public class ChatProcessor {
    
    private final LlamaModel model;
    
    public ChatProcessor(ServerConfig config) throws IOException {
        System.out.println("Đang khởi tạo Llama model...");
        
        ModelParameters modelParams = new ModelParameters()
                .setModel(config.getModelPath())
                .setGpuLayers(config.getGpuLayers());
        
        this.model = new LlamaModel(modelParams);
        
        System.out.println("Model đã sẵn sàng!");
    }
    
    /**
     * Xử lý chat request và trả về response
     */
    public String processChat(ChatRequest request) {
        try {
            // Debug: Kiểm tra encoding
            System.out.println("[DEBUG] User message bytes: " + request.userMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            System.out.println("[DEBUG] System prompt bytes: " + request.systemPrompt.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            
            // Sử dụng format() method của ChatRequest
            String prompt = request.format();
            
            StringBuilder response = new StringBuilder();
            InferenceParameters inferParams = new InferenceParameters(prompt)
                    .setTemperature(request.temperature)
                    .setPenalizeNl(true)
                    .setStopStrings("<end_of_turn>", "<start_of_turn>");
            
            System.out.println("Processing message (length=" + request.userMessage.length() + ") from client");
            
            for (LlamaOutput output : model.generate(inferParams)) {
                response.append(output);
            }
            
            String result = response.toString().trim();
            System.out.println("Response: " + result);
            return result;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }
}
