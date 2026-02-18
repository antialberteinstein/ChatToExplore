# ChatToExplore - Hệ thống Hỗ trợ Tìm kiếm và Trò chuyện Thông minh

**ChatToExplore** là một dự án phần mềm tích hợp đa ngôn ngữ (Java, Python) cung cấp khả năng tìm kiếm và trò chuyện thông minh sử dụng Mô hình Ngôn ngữ Lớn (LLM). Dự án được thiết kế theo mô hình MVC và triển khai trên nền tảng Web.

## 🚀 Tính năng chính

*   **Trò chuyện thông minh (Chat Executor)**: Tích hợp mô hình `Gemma-2-9b-it` (thông qua thư viện Llama Java binding) để phản hồi người dùng tự nhiên.
*   **Tìm kiếm thông tin (Search Executor)**: Module Python sử dụng Google GenAI để xử lý và tìm kiếm thông tin.
*   **Giao diện Web**: Giao diện người dùng thân thiện, tương tác với hệ thống qua trình duyệt.
*   **Quản lý dữ liệu**: Lưu trữ lịch sử và thông tin người dùng bằng MySQL.

## 🛠 Yêu cầu hệ thống

Để chạy dự án, bạn cần cài đặt các công cụ sau:

*   **Java Development Kit (JDK)**: Phiên bản 8 trở lên.
*   **Maven**: Phiên bản 3.9.x.
*   **Python**: Phiên bản 3.10.x hoặc 3.11.x (Khuyên dùng `pyenv`, `anaconda`, hoặc `uv` để quản lý phiên bản).
*   **MySQL**: Cơ sở dữ liệu.

## 📦 Cài đặt

### 1. Chuẩn bị môi trường

1.  **Cài đặt Python dependencies**:
    Di chuyển vào thư mục `src/search_executor/` và chạy lệnh:
    ```bash
    pip install -r requirements.txt
    ```
    *Lưu ý*: Kiểm tra và cài đặt đúng phiên bản Python yêu cầu.

2.  **Cài đặt Cơ sở dữ liệu (MySQL)**:
    *   Tải và cài đặt MySQL Server.
    *   Chạy file `database.sql` (ở thư mục gốc) để tạo cấu trúc bảng (Hệ thống có thể tự động tạo khi khởi chạy, nhưng khuyến khích chạy thủ công để đảm bảo).
    *   Cấu hình kết nối Database trong file `src/main/java/config/DatabaseManager.java`:
        ```java
        private static final String DB_HOST = "localhost";
        private static final String DB_PORT = "3306"; // Cổng mặc định
        private static final String DB_NAME = "finalproject";
        private static final String DB_USERNAME = "root"; // Username của bạn
        private static final String DB_PASSWORD = "YOUR_PASSWORD"; // Mật khẩu của bạn
        ```

3.  **Tải Model LLM**:
    *   Do kích thước lớn, model không được đính kèm trong source code.
    *   Tải model `gemma-3-4b-it-Q4_0.gguf` từ HuggingFace:
        [Link tải Model](https://huggingface.co/unsloth/gemma-3-4b-it-GGUF/blob/main/gemma-3-4b-it-Q4_0.gguf)
    *   Tạo thư mục `src/chat_executor/models/` và đặt file model đã tải vào đó.
    *   *Lưu ý*: Model yêu cầu khoảng 1-2GB RAM. Nếu máy yếu hơn, hãy dùng bản `Q3_K_S`; nếu mạnh hơn (>32GB RAM), hãy dùng bản `BF16`.

### 2. Biến môi trường

Đảm bảo cấu hình các biến môi trường cần thiết (ví dụ API Keys cho Google GenAI) trong file `.env` tại các thư mục tương ứng (tham khảo `.gitignore` để biết vị trí file `env` cần tạo).

## ▶️ Chạy chương trình

### Bước 1: Khởi chạy module Search (Python)
Tại thư mục `src/search_executor/`:
```bash
python main.py
# hoặc python3 main.py
```

### Bước 2: Khởi chạy module Chat (Java)
Tại thư mục `src/chat_executor/`:
*   **Linux/macOS**:
    ```bash
    ./build.sh  # Chỉ chạy lần đầu hoặc khi clean build
    ./run.sh
    ```
*   **Windows**:
    ```bash
    mvn clean compile
    mvn package
    java -jar .\target\chat_executor\chat_executor-1.0-SNAPSHOT.jar
    ```

### Bước 3: Khởi chạy Web Server
Tại thư mục gốc của dự án:
```bash
mvn cargo:run
```
*Lệnh này sẽ tải và chạy Tomcat 9 server.*

Nếu gặp lỗi, hãy thử chạy lần lượt:
```bash
mvn clean compile
mvn package
mvn cargo:run
```

## 🤝 Đóng góp

Dự án được phát triển như một bài tập lớn/đồ án môn học Lập trình mạng.
