package com.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AgentProcessor: tập trung xử lý các agent prompt.
 * Các phương thức tĩnh trả về prompt dựa trên message / context.
 */
public class AgentProcessor {
    public static class AgentResponseObject {
        public String response;
        public boolean figureFlag;

        // Đảm bảo nếu cần có thể trả về FigureDTO
        public FigureDTO figureDTO;

        public AgentResponseObject(String response, boolean figureFlag) {
            this.response = response;
            this.figureFlag = figureFlag;
            this.figureDTO = null;
        }
    }

    private ChatProcessor chatProcessor;

    public AgentProcessor(ChatProcessor chatProcessor) {
        this.chatProcessor = chatProcessor;
    }


    public AgentResponseObject processChat(ChatRequest chatRequest) {
        // Đọc cờ figure flag từ chatRequest
        if (chatRequest.figureFlag) {
            System.out.println("FIGURE FLAG ĐANG BẬT CHO REQUEST " + chatRequest.userMessageAsKey);
            System.out.println("AgentProcessor: Xử lý theo figure flag.");
            String figurePrompt = getThirdAgentPrompt("", chatRequest.userMessage);
            ChatRequest figureRequest = new ChatRequest(
                    chatRequest.username,
                    chatRequest.systemPrompt,
                    chatRequest.conversation,
                    chatRequest.userMessageAsKey,
                    figurePrompt,
                    chatRequest.temperature
            );

            String figureResponse = chatProcessor.processChat(figureRequest);

            Action action = parseResult(figureResponse);

            if (action == null || !action.action.equals("figure")) {
                System.out.println("AgentProcessor: Không tìm thấy action figure trong response.");
                return new AgentResponseObject(figureResponse, false);
            }
            
            System.out.println("PARSING: " + action.query);
            FigureDTO figureDTO = parseFigure(action.query);
            System.out.println("PARSING RESULT: " + figureDTO);

            if (figureDTO == null) {
                System.out.println("AgentProcessor: Không parse được FigureDTO từ response.");
                return new AgentResponseObject(figureResponse, false);
            }

            AgentResponseObject figureResponseObject = new AgentResponseObject(figureResponse, false);
            figureResponseObject.figureDTO = figureDTO;
            return figureResponseObject;
        }

        System.out.println("FIGURE FLAG ĐANG TẮT CHO REQUEST " + chatRequest.userMessageAsKey);

        ChatRequest firstRequest = new ChatRequest(
                chatRequest.username,
                chatRequest.systemPrompt,
                chatRequest.conversation,
                chatRequest.userMessageAsKey,
                getFirstAgentPrompt(chatRequest.userMessage),
                chatRequest.temperature
        );

        String response = chatProcessor.processChat(firstRequest);

        Action action = parseResult(response);

        if (action == null) {
            return new AgentResponseObject(response, false);
        }

        if (!action.action.equals("lookup")) {
            return new AgentResponseObject(response, false);
        }

        System.out.println("AgentProcessor: Thực hiện lookup cho query: " + action.query);

        String context = doLookup(action.query);

        System.out.println("AgentProcessor: Nhận được context từ lookup: " + context);

        ChatRequest secondRequest = new ChatRequest(
                chatRequest.username,
                chatRequest.systemPrompt,
                chatRequest.conversation,
                chatRequest.userMessageAsKey,
                getSecondAgentPrompt(context, chatRequest.userMessage),
                chatRequest.temperature
        );        

        String secondResponse = chatProcessor.processChat(secondRequest);

        System.out.println("AgentProcessor: Kết quả từ second agent prompt: " + secondResponse);

        return new AgentResponseObject(secondResponse, true);
    }

    private static String doLookup(String query) {
		// Gọi search executor (Python TCP server) để lấy context
		// Protocol: send a single UTF-8 line (query + '\n'), read a single-line UTF-8 response
		final String host = "localhost";
		final int port = 8887; // must match search_executor main.py default
		try (java.net.Socket socket = new java.net.Socket(host, port)) {
			// increase timeout to allow the search server to contact external APIs
			socket.setSoTimeout(36000); // 36s timeout

			try (java.io.BufferedWriter out = new java.io.BufferedWriter(
					new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
				 java.io.BufferedReader in = new java.io.BufferedReader(
					new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

				// send query
				out.write(query);
				out.write('\n');
				out.flush();

				// read all lines from the server (join into a single context)
				StringBuilder sb = new StringBuilder();
				String line;
				boolean first = true;
				while ((line = in.readLine()) != null) {
					if (!first) sb.append('\n');
					sb.append(line);
					first = false;
				}

				return sb.toString();
			}

		} catch (Exception e) {
			System.err.println("[AgentProcessor.doLookup] Error connecting to search server: " + e.getMessage());
			return "Không có kết quả tìm kiếm. Có thể do lỗi kết nối.";
		}
    }

    private static Integer parseYear(String yearStr) {
        try {
            // Cách đơn giản: Cố gắng parse toàn bộ chuỗi
            return Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            // Cách nâng cao: Nếu AI trả về "c. 1290" hoặc "1290?", dùng Regex để lấy số đầu tiên tìm thấy
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\d+");
                java.util.regex.Matcher m = p.matcher(yearStr);
                if (m.find()) {
                    return Integer.parseInt(m.group());
                }
            } catch (Exception ex) {
                // Không tìm thấy số nào
            }
            return null;
        }
    }

    private static FigureDTO parseFigure(String responseText) {
        if (responseText == null || responseText.isEmpty()) {
            return null;
        }

        FigureDTO dto = new FigureDTO();
        
        // Khởi tạo mặc định
        dto.setBorn(null);
        dto.setDied(null);

        // 1. Định nghĩa Regex
        // Giải thích:
        // (Name|ShortInfo|...) : Nhóm 1 - Bắt từ khóa (Key)
        // :\s* : Dấu hai chấm và khoảng trắng đi kèm
        // (.*?)                : Nhóm 2 - Nội dung giá trị (Value), lấy ít nhất có thể
        // (?=\s+(?:Name|ShortInfo|Hometown|Born|Died):|$) : Lookahead - Dừng lại khi gặp từ khóa tiếp theo HOẶC hết chuỗi
        String regex = "(Name|ShortInfo|Hometown|Born|Died):\\s*(.*?)(?=\\s+(?:Name|ShortInfo|Hometown|Born|Died):|$)";
        
        // Pattern.CASE_INSENSITIVE: Không phân biệt hoa thường
        // Pattern.DOTALL: Cho phép dấu chấm (.) khớp cả ký tự xuống dòng (xử lý tốt cả multi-line và single-line)
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseText);

        boolean foundName = false;

        // 2. Duyệt qua các kết quả tìm thấy
        while (matcher.find()) {
            String key = matcher.group(1);         // Ví dụ: "Name"
            String value = matcher.group(2).trim(); // Ví dụ: "Nguyễn Phú Trọng"

            // Map dữ liệu vào DTO
            if ("Name".equalsIgnoreCase(key)) {
                dto.setName(value);
                foundName = true;
            } 
            else if ("ShortInfo".equalsIgnoreCase(key)) {
                dto.setShortInfo(value);
            } 
            else if ("Hometown".equalsIgnoreCase(key)) {
                dto.setHometown(value);
            } 
            else if ("Born".equalsIgnoreCase(key)) {
                dto.setBorn(parseYear(value));
            } 
            else if ("Died".equalsIgnoreCase(key)) {
                dto.setDied(parseYear(value));
            }
        }

        // Nếu không tìm thấy Name, coi như parse thất bại
        if (!foundName || dto.getName() == null || dto.getName().isEmpty()) {
            return null;
        }

        return dto;
    }

    private static Action parseResult(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        // Regex giữ nguyên logic của bạn
        String regex = "<(lookup|figure)>(.*?)</\\1>";
        
        // THÊM 2 FLAG QUAN TRỌNG:
        // 1. Pattern.DOTALL: Cho phép dấu chấm (.) ăn cả ký tự xuống dòng (\n)
        // 2. Pattern.CASE_INSENSITIVE: Để bắt được cả <LOOKUP> hoặc <Figure>
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            Action actionObj = new Action();
            // group(1) là tên thẻ (lookup hoặc figure)
            // chuyển về lowercase để chuẩn hóa dữ liệu đầu ra
            actionObj.action = matcher.group(1).toLowerCase(); 
            
            // group(2) là nội dung bên trong
            actionObj.query = matcher.group(2).trim();
            return actionObj;
        }

        return null;
    }

	public static String getAgentPrompt(String userMessage) {
		return getFirstAgentPrompt(userMessage);
	}

	public static String getFirstAgentPrompt(String userMessage) {
        return "[INSTRUCTION]\nNếu người dùng yêu cầu tìm hiểu thông tin về một nhân vật nào đó, thì hãy trả lời theo định dạng <lookup>Nhân vật đó</lookup>\n" +
                "Ví dụ:\n" +
                "User: Ai là người lãnh đạo khởi nghĩa Lam Sơn?\n" +
                "Model: <lookup>người lãnh đạo khởi nghĩa Lam Sơn</lookup>\n" +
                "User: Nguyễn Huệ là ai?\n" +
                "Model: <lookup>Nguyễn Huệ</lookup>\n" +
                "User: Ai là vị vua đầu tiên của nhà Lý?\n" +
                "Model: <lookup>vị vua đầu tiên của nhà Lý</lookup>\n" +
                "User: Còn vị vua thứ hai là ai?\n" +
                "Model: <lookup>vị vua thứ hai của nhà Lý</lookup>\n" +
                "User: Trần Hưng Đạo có công trạng gì?\n" +
                "Model: <lookup>Trần Hưng Đạo</lookup>\n" +
                "User: Lê Lợi sinh năm bao nhiêu?\n" +
                "Model: <lookup>Lê Lợi</lookup>\n" +
                "User: Ai là người sáng lập triều Nguyễn?\n" +
                "Model: <lookup>người sáng lập triều Nguyễn</lookup>\n" +
                "User: Hồ Quý Ly là ai?\n" +
                "Model: <lookup>Hồ Quý Ly</lookup>\n" +
                "User: Ai là vị tướng nổi tiếng thời Tây Sơn?\n" +
                "Model: <lookup>vị tướng nổi tiếng thời Tây Sơn</lookup>\n" +
                "User: Vị vua đầu tiên của nhà Trần là ai?\n" +
                "Model: <lookup>vị vua đầu tiên của nhà Trần</lookup>\n" +
                "User: Nguyễn Trãi có vai trò gì trong lịch sử Việt Nam?\n" +
                "Model: <lookup>Nguyễn Trãi</lookup>\n" +
                "User: Đinh Bộ Lĩnh có vai trò gì trong lịch sử?\n" +
                "Model: <lookup>Đinh Bộ Lĩnh</lookup>\n" +
                "User: Ai là người giúp vua Quang Trung đánh thắng quân Thanh?\n" +
                "Model: <lookup>người giúp vua Quang Trung đánh thắng quân Thanh</lookup>\n" +
                "Nếu người dùng không yêu cầu tìm hiểu về một nhân vật nào đó, hãy trả lời lịch sự.\n" +
                "[QUESTION]\n" + userMessage + "\n";
	}

	public static String getSecondAgentPrompt(String context, String user_question) {
		return "[INSTRUCTION]\n" +
				"Từ thông tin được cung cấp trong CONTEXT, hãy dựa vào đó trả lời câu hỏi của người dùng. Sau khi trả lời xong, hãy hỏi người dùng có muốn lưu nhân vật này vào dữ liệu không.\n" +
				"Nếu không tìm thấy thông tin liên quan trong CONTEXT, hãy trả lời 'Xin lỗi, tôi không tìm thấy thông tin liên quan.'\n" +
				"\n" +
				"[CONTEXT]\n" + context + "\n" +
				"[USER QUESTION]\n" + user_question + "\n";
	}

	public static String getThirdAgentPrompt(String context, String user_question) {
		return "[INSTRUCTION]\n" +
				"Nếu người dùng trả lời có, hãy dựa vào CONTEXT để trả lời theo định dạng sau:\n" +
				"<figure>\n" +
				"Name: Trần Thủ Độ\n" +
				"ShortInfo: Công thần triều Trần, đưa Trần Thái Tông lên ngôi\n" +
				"Hometown: Thái Bình\n" +
				"Born: 1194\n" +
				"Died: 1264\n" +
				"</figure>\n" +
				"\n" +
				"Nếu người dùng trả lời không, hãy trả lời 'Chúng ta sẽ tìm hiểu về nhân vật này sau nhé!.'\n" +
				"\n" +
				"[CONTEXT]\n" + context + "\n" +
				"[USER QUESTION]\n" + user_question + "\n";
	}

	public static String getAgentToSavePrompt(String context, String user_question) {
		return "[INSTRUCTION]\n" +
				"Từ thông tin được cung cấp trong CONTEXT, hãy dựa vào đó trả lời câu hỏi của người dùng, sau khi trả lời xong, hãy hỏi người dùng có muốn lưu nhân vật này vào dữ liệu không.\n" +
				"Nếu có, hãy trích xuất thông tin nhân vật từ CONTEXT và định dạng theo mẫu FIGURE ở bên dưới.\n" +
				"Nếu không, hãy trả lời Chúng ta sẽ tìm hiểu về nhân vật này sau nhé!.\n" +
				"<figure>\n" +
				"Name: Trần Thủ Độ\n" +
				"ShortInfo: Công thần triều Trần, đưa Trần Thái Tông lên ngôi\n" +
				"Hometown: Thái Bình\n" +
				"Born: 1194\n" +
				"Died: 1264\n" +
				"</figure>\n" +
				"\n" +
				"[CONTEXT]\n" + context + "\n" +
				"[USER QUESTION]\n" + user_question + "\n";
	}
}
