package model.bo;

import model.dao.ChatDAO;
import model.dao.FlagDAO;
import model.dto.ChatRequest;
import model.dto.ChatResult;
import model.dto.FigureDTO;
import model.dto.Action;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat Business Object - xử lý logic chat với bot
 * Architecture mới:
 * - loadHistory(): Lấy lịch sử từ ChatDAO
 * - submitQuestion(): Gửi request vào queue (process command)
 * - pollResult(): Lấy kết quả (load command) - frontend sẽ polling
 */
public class ChatBO {

    public interface AgentPrompt {
        String getPrompt(String context, String user_question);
    }

    private final FigureBO figureBO = FigureBO.getInstance();
    
    private static final String TCP_HOST = "localhost";
    private static final int TCP_PORT = 8888;
    private ChatDAO chatDAO;
    private FlagDAO flagDAO;
    private static final String DEFAULT_SYSTEM_PROMPT = 
        "Bạn là trợ lý AI thông minh chuyên về lịch sử Việt Nam. " +
        "Hãy trả lời các câu hỏi về lịch sử, nhân vật, sự kiện lịch sử Việt Nam một cách chính xác, " +
        "ngắn gọn, thân thiện và dễ hiểu. Trả lời bằng tiếng Việt.";

    private static String get_agent_prompt(String userMessage) {
        return "[INSTRUCTION]\nNếu người dùng yêu cầu tìm hiểu thông tin về một nhân vật nào đó, thì hãy trả lời theo định dạng <lookup>Nhân vật đó</lookup>\n" + //
                "Ví dụ:\n" + //
                "Ai là người lãnh đạo khởi nghĩa Lam Sơn?\n" + //
                "<lookup>người lãnh đạo khởi nghĩa Lam Sơn</lookup>\n" + //
                "Nguyễn Huệ là ai?\n" + //
                "<lookup>Nguyễn Huệ</lookup>\n" + //
                "Nếu người dùng không yêu cầu tìm hiểu về một nhân vật nào đó, hãy trả lời lịch sự." + "\n" + //
                "[QUESTION]\n" + userMessage + "\n";
    }

    private static String get_first_agent_prompt(String userMessage) {
        return "[INSTRUCTION]\nNếu người dùng yêu cầu tìm hiểu thông tin về một nhân vật nào đó, thì hãy trả lời theo định dạng <lookup>Nhân vật đó</lookup>\n" + //
                "Ví dụ:\n" + //
                "Ai là người lãnh đạo khởi nghĩa Lam Sơn?\n" + //
                "<lookup>người lãnh đạo khởi nghĩa Lam Sơn</lookup>\n" + //
                "Nguyễn Huệ là ai?\n" + //
                "<lookup>Nguyễn Huệ</lookup>\n" + //
                "Nếu người dùng không yêu cầu tìm hiểu về một nhân vật nào đó, hãy trả lời lịch sự." + "\n" + //
                "[QUESTION]\n" + userMessage + "\n";
    }

    private static final String get_second_agent_prompt(String context, String user_question) {
        return "[INSTRUCTION]\n" + //
                "Từ thông tin được cung cấp trong CONTEXT, hãy dựa vào đó trả lời câu hỏi của người dùng, sau đó hỏi người dùng có muốn thêm nhân vật này không?\n" + //
                "Nếu không tìm thấy thông tin liên quan trong CONTEXT, hãy trả lời 'Xin lỗi, tôi không tìm thấy thông tin liên quan.'\n" + //
                "Nếu CONTEXT trả về không có thông tin, CHẮC CHẮN TRẢ LỜI là Tôi không biết về nhân vật này, không bịa câu trả lời.\n" + //
                "\n" + //
                "[CONTEXT]\n" + context + "\n"
                + "[USER QUESTION]\n" + user_question + "\n";
    }

    private static final String get_third_agent_prompt(String context, String user_question) {
        return "[INSTRUCTION]\n" + //
                "Nếu người dùng trả lời có, hãy dựa vào CONTEXT để trả lời theo định dạng sau:\n" + //
                "<figure>\n" + //
                "Name: Trần Thủ Độ\n" + //
                "ShortInfo: Công thần triều Trần, đưa Trần Thái Tông lên ngôi\n" + //
                "Hometown: Thái Bình\n" + //
                "Born: 1194\n" + //
                "Died: 1264\n" + //
                "</figure>\n" + //
                "\n" + //
                "Nếu người dùng trả lời không, hãy trả lời 'Chúng ta sẽ tìm hiểu về nhân vật này sau nhé!.'\n" + //
                "\n" + //
                "[CONTEXT]\n" + context + "\n"
                +
                "[USER QUESTION]\n" + user_question + "\n";
    }

    private static final String get_agent_to_save_prompt(String context, String user_question) {
        return "[INSTRUCTION]\n" + //
                "Từ thông tin được cung cấp trong CONTEXT, hãy dựa vào đó trả lời câu hỏi của người dùng, sau khi trả lời xong, hãy hỏi người dùng có muốn lưu nhân vật này vào dữ liệu không.\n" + //
                "Nếu có, hãy trích xuất thông tin nhân vật từ CONTEXT và định dạng theo mẫu FIGURE ở bên dưới.\n" + //
                "Nếu không, hãy trả lời Chúng ta sẽ tìm hiểu về nhân vật này sau nhé!.\n" + //
                "<figure>\n" + //
                "Name: Trần Thủ Độ\n" + //
                "ShortInfo: Công thần triều Trần, đưa Trần Thái Tông lên ngôi\n" + //
                "Hometown: Thái Bình\n" + //
                "Born: 1194\n" + //
                "Died: 1264\n" + //
                "</figure>\n" + //
                "\n" + //
                "[CONTEXT]\n" + context + "\n"
                + "[USER QUESTION]\n" + user_question + "\n";
    }



    
    public ChatBO() {
        this.chatDAO = new ChatDAO();
        this.flagDAO = new FlagDAO();
    }
    
    /**
     * Lấy lịch sử chat của user từ ChatDAO
     */
    public List<ChatRequest.Message> loadHistory(String username) {
        return chatDAO.getChatHistory(username);
    }

    public String getLastMessage(String username) {
        return chatDAO.getLastMessage(username);
    }
    
    /**
     * Gửi câu hỏi vào queue (process command)
     * @return true nếu thành công, false nếu queue đầy
     */
    public boolean submitQuestion(String username, String message) {
        return submitQuestion(username, message, 0.3f, (context, user_question) -> {
            return user_question;
        }, "");
    }
    
    /**
     * Gửi câu hỏi vào queue với system prompt và temperature tùy chỉnh
     */
    public boolean submitQuestion(String username, String message, float temperature, AgentPrompt agentPrompt, String context) {
        String systemPrompt = DEFAULT_SYSTEM_PROMPT;
        String userMessage = message.trim();
        
        if (message == null || userMessage.isEmpty()) {
            chatDAO.saveMessage(username, userMessage, "Không thể kết nối đến server, vui lòng thử lại sau.");
            return false;
        }

        // Lưu câu hỏi đang được xử lý
        chatDAO.saveLastMessage(username, userMessage);
        
        // Lấy lịch sử chat từ DAO
        List<ChatRequest.Message> chatHistory = chatDAO.getChatHistory(username);
        
        // Build ChatRequest với history
        ChatRequest.Conversation conversation = new ChatRequest.Conversation();
        conversation.messages.addAll(chatHistory);

        String agentUserMessage = agentPrompt.getPrompt(context, userMessage);
        
        ChatRequest request = new ChatRequest(username, systemPrompt, conversation, userMessage, agentUserMessage, temperature);

        // Đọc cờ figure flag
        boolean figureFlag = flagDAO.isFlagEnabled(username, "figure_flag");
        request.figureFlag = figureFlag;
        
        System.out.println("[ChatBO] Submitting question for user: " + username);
        
        // Gửi command "process" để đưa request vào queue
        return sendProcessCommand(request);
    }
    
    /**
     * Lấy kết quả từ server (load command)
     * @return ChatResult với status: 1=success, 0=pending, -1=error, -2=not found
     */
    public ChatResult pollResult(String username, String userMessage) {
        ChatResult result = sendLoadCommand(username, userMessage);

        if (result != null && result.getStatus() == 1) {
            if (result.isFigureFlag()) {
                flagDAO.enableFlag(username, "figure_flag");
                System.out.println("FIGURE FLAG ĐÃ ĐƯỢC BẬT CHO USER " + username);
            } else {
                flagDAO.disableFlag(username, "figure_flag");
                System.out.println("FIGURE FLAG ĐÃ ĐƯỢC TẮT CHO USER " + username);
            }

            // Kiểm tra Figure.
            FigureDTO figureDTO = result.getFigureDTO();
            if (figureDTO != null && !figureDTO.isEmpty) {
                // Lưu Figure
                String createResult = figureBO.createUserFigure(
                    username,
                    figureDTO.getName(),
                    figureDTO.getBorn(),
                    figureDTO.getDied(),
                    figureDTO.getShortInfo(),
                    "",
                    figureDTO.getHometown()
                );
                // Chỉ in ra phần "message" từ JSON trả về
                String createMessage = extractMessageFromJson(createResult);
                System.out.println(createMessage);
                System.out.println("FIGURE ĐÃ ĐƯỢC LƯU CHO USER " + username);

                // Lấy biến `success` từ JSON trả về. Nếu true thì yêu cầu tải lại trang,
                // nếu false thì chỉ hiển thị message mà không yêu cầu reload.
                boolean createSuccess = extractSuccessFromJson(createResult);
                if (createSuccess) {
                    result.setResponse(createMessage + " Vui lòng tải lại trang để xem nhân vật mới.");
                } else {
                    result.setResponse(createMessage);
                }
            } else {
                System.out.println("KHÔNG CÓ FIGURE NÀO ĐỂ LƯU CHO USER " + username);
            }
        }

        return result;
    }

    private static ArrayList<Action> parseResult(String response) {
        ArrayList<Action> actions = new ArrayList<>();
        
        if (response == null || response.isEmpty()) {
            return actions;
        }

        // Regex giải thích:
        // <(lookup|figure)> : Tìm thẻ mở là 'lookup' hoặc 'figure' (Group 1)
        // (.*?)             : Lấy nội dung bên trong một cách non-greedy (Group 2)
        // </\\1>            : Tìm thẻ đóng tương ứng với thẻ mở ở Group 1
        String regex = "<(lookup|figure)>(.*?)</\\1>";
        
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            Action actionObj = new Action();
            actionObj.action = matcher.group(1); // Lấy tên thẻ (lookup hoặc figure)
            actionObj.query = matcher.group(2).trim(); // Lấy nội dung query và xóa khoảng trắng thừa
            
            actions.add(actionObj);
        }

        return actions;
    }

    private static void parseFigure(String figureText) {

    }

    /**
     * Extract the `message` field from a simple JSON object like:
     * {"success": true, "message": "...", "figureId": "..."}
     * If no message field found, returns the original string.
     */
    private static String extractMessageFromJson(String json) {
        if (json == null) return "";
        try {
            Pattern p = Pattern.compile("\"message\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // ignore and fall through
        }
        return json;
    }

    /**
     * Extract the `success` boolean from a simple JSON object like:
     * {"success": true, "message": "..."}
     * Returns false if not found or parse error.
     */
    private static boolean extractSuccessFromJson(String json) {
        if (json == null) return false;
        try {
            Pattern p = Pattern.compile("\"success\"\\s*:\\s*(true|false|\"true\"|\"false\"|1|0)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(json);
            if (m.find()) {
                String v = m.group(1).toLowerCase();
                if (v.equals("true") || v.equals("\"true\"") || v.equals("1")) return true;
                return false;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
    
    /**
     * Lưu kết quả thành công vào ChatDAO
     */
    public void saveResult(String username, String question, String response) {
        chatDAO.saveMessage(username, question, response);
    }
    
    /**
     * Xóa lịch sử chat của user
     */
    public void clearHistory(String username) {
        chatDAO.clearChatHistory(username);
    }
    
    /**
     * Gửi command "process" để đưa ChatRequest vào queue
     */
    private boolean sendProcessCommand(ChatRequest request) {
        try (Socket socket = new Socket(TCP_HOST, TCP_PORT)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            // Gửi command "process"
            out.writeUTF("process");

            System.out.println("FIGURE FLAG CHO REQUEST " + request.userMessageAsKey + ": " + request.figureFlag);
            
            // Gửi ChatRequest
            request.sendBy(out);
            out.flush();
            
            // Nhận response (status + message)
            int status = in.readInt();
            String message = in.readUTF();
            
            System.out.println("[Process] Status: " + status + ", Message: " + message);
            
            return status == 1;
            
        } catch (IOException e) {
            System.err.println("[Process] Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gửi command "load" để lấy kết quả
     */
    private ChatResult sendLoadCommand(String username, String userMessage) {
        try (Socket socket = new Socket(TCP_HOST, TCP_PORT)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            // Gửi command "load"
            out.writeUTF("load");
            
            // Gửi username + userMessage
            out.writeUTF(username);
            out.writeUTF(userMessage);
            out.flush();
            
            // Nhận ChatResult
            ChatResult result = ChatResult.receiveFrom(in);
            
            System.out.println("[Load] Status: " + result.getStatus() + " for user: " + username);
            
            return result;
            
        } catch (IOException e) {
            System.err.println("[Load] Error: " + e.getMessage());
            return null;
        }
    }
}
