package com.chat;

/**
 * QueueProcessor - Thread tự động xử lý các ChatRequest trong queue
 * Lấy request từ ChatRequestKeeper, xử lý bằng AgentProcessor, lưu kết quả vào ChatResultKeeper
 */
public class QueueProcessor extends Thread {
    
    private final ChatRequestKeeper chatRequestKeeper;
    private final AgentProcessor agentProcessor;
    private final ChatResultKeeper chatResultKeeper;
    private volatile boolean running = true;

    public QueueProcessor(ChatRequestKeeper chatRequestKeeper, AgentProcessor agentProcessor, ChatResultKeeper chatResultKeeper) {
        super("QueueProcessor-Thread");
        this.chatRequestKeeper = chatRequestKeeper;
        this.agentProcessor = agentProcessor;
        this.chatResultKeeper = chatResultKeeper;
        this.setDaemon(false);
    }
    
    @Override
    public void run() {
        System.out.println("[QueueProcessor] Thread started");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Lấy request từ queue (non-blocking)
                ChatRequest request = chatRequestKeeper.poll();
                
                // Nếu queue rỗng, sleep một chút và tiếp tục
                if (request == null) {
                    Thread.sleep(100); // Sleep 100ms
                    continue;
                }
                
                System.out.println("[QueueProcessor] Processing request from user: " + request.username);
                
                // Xử lý request
                AgentProcessor.AgentResponseObject response = agentProcessor.processChat(request);
                
                // Lưu kết quả
                if (response != null && !response.response.isEmpty()) {
                    ChatResult result = ChatResult.success(request, response.response);
                    result.setFigureFlag(response.figureFlag);

                    if (response.figureDTO != null) {
                        result.setFigureDTO(response.figureDTO);
                    } else {
                        result.setFigureDTO(null);
                    }

                    chatResultKeeper.storeResult(result);
                    System.out.println("[QueueProcessor] Request completed successfully for user: " + request.username);
                } else {
                    chatResultKeeper.storeResult(ChatResult.error(request, "Lỗi xử lý yêu cầu"));
                    System.out.println("[QueueProcessor] Request failed for user: " + request.username);
                }
                
                System.out.println("[QueueProcessor] Queue size: " + chatRequestKeeper.size());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[QueueProcessor] Thread interrupted");
                break;
            } catch (Exception e) {
                System.err.println("[QueueProcessor] Error processing request: " + e.getMessage());
                e.printStackTrace();
                // Lưu error vào ChatResultKeeper nếu có request đang xử lý
                try {
                    ChatRequest failedRequest = chatRequestKeeper.poll();
                    if (failedRequest != null) {
                        chatResultKeeper.storeResult(ChatResult.error(failedRequest, "Internal error: " + e.getMessage()));
                    }
                } catch (Exception ignored) {}
            }
        }
        
        System.out.println("[QueueProcessor] Thread stopped");
    }
    
    /**
     * Dừng thread
     */
    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
