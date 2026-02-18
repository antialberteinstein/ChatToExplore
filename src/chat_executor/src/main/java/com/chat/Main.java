package com.chat;

public class Main {
    
    public static void main(String... args) {
        TcpServer tcpServer = null;
        QueueProcessor queueProcessor = null;
        
        try {
            // Tạo configuration
            ServerConfig config = ServerConfig.defaultConfig();
            System.out.println("Server configuration: " + config);
            
            // Dependency injection
            ChatRequestKeeper chatRequestKeeper = new ChatRequestKeeper(100);
            ChatResultKeeper chatResultKeeper = new ChatResultKeeper();
            ChatProcessor chatProcessor = new ChatProcessor(config);
            AgentProcessor agentProcessor = new AgentProcessor(chatProcessor);
            tcpServer = new TcpServer(config, chatRequestKeeper, chatResultKeeper);
            
            // Khởi động QueueProcessor thread
            queueProcessor = new QueueProcessor(chatRequestKeeper, agentProcessor, chatResultKeeper);
            queueProcessor.start();
            System.out.println("QueueProcessor thread started");
            
            // Đăng ký shutdown hook để xử lý Ctrl+C
            final TcpServer serverToShutdown = tcpServer;
            final QueueProcessor processorToShutdown = queueProcessor;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Shutdown] Received interrupt signal (Ctrl+C)");
                System.out.println("[Shutdown] Shutting down server gracefully...");
                
                // Shutdown QueueProcessor trước
                if (processorToShutdown != null) {
                    processorToShutdown.shutdown();
                    try {
                        processorToShutdown.join(3000); // Đợi tối đa 3s
                        System.out.println("[Shutdown] QueueProcessor stopped");
                    } catch (InterruptedException e) {
                        System.err.println("[Shutdown] QueueProcessor interrupted during shutdown");
                    }
                }
                
                // Shutdown TcpServer
                serverToShutdown.shutdown();
                System.out.println("[Shutdown] Server stopped successfully");
            }));
            
            // Khởi động server
            tcpServer.start();
            
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
