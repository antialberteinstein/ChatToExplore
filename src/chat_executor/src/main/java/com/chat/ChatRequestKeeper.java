package com.chat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Quản lý hàng đợi ChatRequest
 * Producer: "process" command đưa request vào queue
 * Consumer: QueueProcessor lấy request ra khỏi queue và xử lý
 */
public class ChatRequestKeeper {
    
    private final BlockingQueue<ChatRequest> queue;
    
    public ChatRequestKeeper(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    /**
     * Thêm ChatRequest vào queue (non-blocking)
     * @return true nếu thành công, false nếu queue đầy
     */
    public boolean enqueue(ChatRequest request) {
        return queue.offer(request);
    }
    
    /**
     * Lấy ChatRequest từ queue (blocking)
     * @return ChatRequest hoặc null nếu bị interrupt
     */
    public ChatRequest dequeue() throws InterruptedException {
        return queue.take();
    }
    
    /**
     * Lấy ChatRequest từ queue (non-blocking)
     * @return ChatRequest hoặc null nếu queue rỗng
     */
    public ChatRequest poll() {
        return queue.poll();
    }
    
    /**
     * Kiểm tra số lượng request trong queue
     */
    public int size() {
        return queue.size();
    }
    
    /**
     * Kiểm tra queue có rỗng không
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
