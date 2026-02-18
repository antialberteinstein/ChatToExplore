package com.chat;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * TCP Server xử lý kết nối từ clients theo command
 * - "process": đưa ChatRequest vào queue
 * - "load": lấy kết quả từ ChatResultKeeper
 */
public class TcpServer {
    
    private final ServerConfig config;
    private final ChatRequestKeeper chatRequestKeeper;
    private final ChatResultKeeper chatResultKeeper;
    private final ExecutorService connectionPool;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    
    public TcpServer(ServerConfig config, ChatRequestKeeper chatRequestKeeper, ChatResultKeeper chatResultKeeper) {
        this.config = config;
        this.chatRequestKeeper = chatRequestKeeper;
        this.chatResultKeeper = chatResultKeeper;
        this.connectionPool = Executors.newCachedThreadPool();
    }
    
    /**
     * Xử lý request từ client theo command
     */
    private void handleClient(Socket clientSocket) {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            System.out.println("[Server] Connection from " + clientAddress);
            
            // Đọc command ("process" hoặc "load")
            String command = in.readUTF();
            System.out.println("[Server] Command: " + command);
            
            if ("process".equals(command)) {
                handleProcessCommand(clientSocket, in);
            } else if ("load".equals(command)) {
                handleLoadCommand(clientSocket, in);
            } else {
                System.out.println("[Server] Unknown command: " + command);
            }
        } catch (Exception e) {
            System.err.println("[Server] Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * Xử lý command "process" - đưa ChatRequest vào queue
     */
    private void handleProcessCommand(Socket clientSocket, DataInputStream in) throws IOException {
        try {
            // Đọc ChatRequest từ stream
            ChatRequest chatRequest = ChatRequest.receiveBy(in);
            System.out.println("[Process] Received from user: " + chatRequest.username);
            
            // Đưa vào queue
            boolean enqueued = chatRequestKeeper.enqueue(chatRequest);

            // Đưa kết quả.
            chatResultKeeper.storeResult(ChatResult.pending(chatRequest));
            
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            if (enqueued) {
                System.out.println("[Process] Request queued. Queue size: " + chatRequestKeeper.size());
                out.writeInt(1); // Success
                out.writeUTF("Request queued successfully");
            } else {
                System.out.println("[Process] Queue full, rejected");
                out.writeInt(0); // Error
                out.writeUTF("Queue is full, please try again later");
            }
            out.flush();
        } catch (Exception e) {
            System.err.println("[Process] Error: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Xử lý command "load" - lấy kết quả từ ChatResultKeeper
     * Client gửi: username + userMessage
     * Server trả về: kết quả (status: 1=success, 0=pending, -1=error)
     */
    private void handleLoadCommand(Socket clientSocket, DataInputStream in) throws IOException {
        try {
            // Đọc username và userMessage
            String username = in.readUTF();
            String userMessage = in.readUTF();
            
            System.out.println("[Load] Looking for result: user=" + username + ", message=" + userMessage);
            
            // Lấy kết quả từ ChatResultKeeper
            ChatResult result = chatResultKeeper.getResult(username, userMessage);
            
            if (result != null) {
                System.out.println("[Load] Result found with status: " + result.getStatus());
                
                // Gửi kết quả về client
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                result.sendBy(out);
                out.flush();
            } else {
                System.out.println("[Load] Result not found");
                
                // Gửi result not found
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                ChatResult notFound = ChatResult.error(new ChatRequest(username, "", null, userMessage, userMessage, 0f), "Câu hỏi chưa được xử lý");
                notFound.sendBy(out);
                out.flush();
            }
            
        } catch (Exception e) {
            System.err.println("[Load] Error: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Khởi động server
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getPort());
        System.out.println("Llama TCP Server đang chạy trên port " + config.getPort());
        System.out.println("Đang chờ client kết nối...");
        System.out.println("Press Ctrl+C to stop server");

        while (running) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                System.out.println("[Server] Client connected from " +
                    clientSocket.getInetAddress().getHostAddress());

                final Socket socket = clientSocket;
                connectionPool.execute(() -> handleClient(socket));
            } catch (SocketException e) {
                if (!running) {
                    // Expected during shutdown
                    break;
                }
                System.err.println("Socket error: " + e.getMessage());
            } catch (Exception e) {
                if (!running) break;
                System.err.println("Error accepting/handling client: " + e.getMessage());
                if (clientSocket != null) {
                    try { clientSocket.close(); } catch (IOException ignored) {}
                }
            }
        }
    }
    
    /**
     * Shutdown server gracefully
     */
    public void shutdown() {
        running = false;
        
        try {
            // Đóng server socket để dừng accept()
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("[Shutdown] Server socket closed");
            }
            
            // Shutdown connection pool
            connectionPool.shutdown();
            if (!connectionPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                connectionPool.shutdownNow();
            }
            System.out.println("[Shutdown] Connection pool terminated");
            
        } catch (Exception e) {
            System.err.println("[Shutdown] Error during shutdown: " + e.getMessage());
        }
    }
}
