package com.chat;

import java.util.HashMap;

public class ChatResultKeeper {

    // Lưu trữ kết quả chat theo cặp (username + userMessage) để truy xuất nhanh
    private final HashMap<String, ChatResult> resultMap;

    public ChatResultKeeper() {
        this.resultMap = new HashMap<>();
    }

    public synchronized void storeResult(ChatResult result) {
        String key = generateKey(result.getUsername(), result.getUserMessage());
        resultMap.put(key, result);
    }

    public synchronized ChatResult getResult(String username, String userMessage) {
        String key = generateKey(username, userMessage);
        return resultMap.get(key);
    }

    private String generateKey(String username, String userMessage) {
        return username + ">>" + userMessage;
    }
}
