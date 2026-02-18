import os
import time
import datetime
import socket
from google import genai
from google.genai import types
from google.genai.errors import APIError

# --- 1. KHỞI TẠO BIẾN CACHE TOÀN CỤC ---
# Khởi tạo None để biểu thị rằng chúng chưa được tạo
_GEMINI_CLIENT = None
_BASE_CONFIG = None
_MODEL_NAME = "gemini-2.5-flash" # Biến này có thể là hằng số
_CITY_NAME = "Đà Nẵng, Việt Nam"

# Cấu hình Thử lại
MAX_RETRIES = 3
RETRY_DELAY_SECONDS = 5

# --- 2. SINGLETON/LAZY LOADERS ---

def get_gemini_client():
    """Tải hoặc khởi tạo Gemini Client (Singleton)."""
    global _GEMINI_CLIENT

    if _GEMINI_CLIENT is not None:
        return _GEMINI_CLIENT
    
    # Logic khởi tạo nặng (chỉ chạy một lần)
    # Support both GEMINI_API_KEY and GOOGLE_API_KEY environment variable names
    api_key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
    if not api_key:
        raise RuntimeError("Gemini API key not found. Set GEMINI_API_KEY or GOOGLE_API_KEY in environment.")
    _GEMINI_CLIENT = genai.Client(api_key=api_key)
    return _GEMINI_CLIENT

def get_base_config():
    """Tải hoặc khởi tạo GenerateContentConfig (Singleton)."""
    global _BASE_CONFIG

    if _BASE_CONFIG is not None:
        return _BASE_CONFIG
    
    # Logic khởi tạo nặng (chỉ chạy một lần)
    SYSTEM_INSTRUCTION = (
        "Bạn là một trợ lý tìm kiếm thông minh và súc tích. "
        "Câu trả lời phải dựa trên thông tin thực tế từ Google Search và phải ngắn gọn, đầy đủ, đúng với thực tại. "
        "Trả lời dưới dạng tiếng Việt."
    )
    _BASE_CONFIG = types.GenerateContentConfig(
        tools=[{"googleSearch": {}}],
        max_output_tokens=4096,
        system_instruction=SYSTEM_INSTRUCTION,
    )
    return _BASE_CONFIG

# --- 3. XỬ LÝ LỖI TUỲ CHỈNH ---
class NoInternetError(Exception):
    """Lỗi khi không có kết nối Internet."""
    pass

class NoSearchResultsError(Exception):
    """Lỗi khi API không thể hoàn tất tìm kiếm."""
    pass

# --- 4. KIỂM TRA MẠNG ---
def check_internet(host="8.8.8.8", port=53, timeout=3):
    try:
        socket.setdefaulttimeout(timeout)
        socket.socket(socket.AF_INET, socket.SOCK_STREAM).connect((host, port))
        return True
    except socket.error:
        print("[WARNING] Không có kết nối Internet")
        return False

# --- 5. HÀM SEARCH TỐI ƯU VỚI LAZY LOADING ---
def search(query):
    # Lấy Client và Config thông qua hàm lazy
    client = get_gemini_client()
    config = get_base_config()

    if not check_internet():
        raise NoInternetError() 
    
    # Biến thời gian phải là cục bộ để đảm bảo tính thời gian thực
    current_time = datetime.datetime.now().strftime("%H:%M %d/%m/%Y")
    
    # Tạo prompt động cho tác vụ tìm kiếm thông tin về nhân vật lịch sử
    prompt = (
        f"YÊU CẦU: Tìm kiếm và cung cấp thông tin về nhân vật lịch sử: '{query}'.\n"
        "HÃY TRẢ LỜI THEO ĐÚNG 2 PHẦN SAU (BẮT BUỘC):\n\n"
        
        "PHẦN 1: MÔ TẢ CHI TIẾT\n"
        "- Viết một đoạn văn cung cấp thông tin ngắn gọn, súc tích về nhân vật (bao gồm năm sinh, năm mất, quê quán, công trạng/thông tin nổi bật).\n"
        "- Tránh đưa tin không có thật.\n"
        "- Nếu không tìm thấy thông tin, chỉ ghi: 'Xin lỗi, tôi không tìm thấy thông tin phù hợp' và dừng lại.\n\n"
        
        "PHẦN 2: TRÍCH XUẤT ĐỊNH DẠNG\n"
        "- Dựa hoàn toàn vào thông tin ở Phần 1, hãy trích xuất dữ liệu theo đúng mẫu dưới đây (giữ nguyên từ khóa tiếng Anh):\n"
        "Name: [Tên nhân vật]\n"
        "ShortInfo: [Tóm tắt công trạng/thông tin nổi bật nhất trong 1 câu]\n"
        "Hometown: [Quê quán]\n"
        "Born: [Năm sinh]\n"
        "Died: [Năm mất]\n"
    )

    for attempt in range(MAX_RETRIES):
        try:
            response = client.models.generate_content(
                model=_MODEL_NAME,
                contents=[prompt],
                config=config,
            )
            
            answer = response.text.strip()
            
            # Trả về câu trả lời nếu có, ngược lại trả về None
            return answer if answer else None 

        except APIError:
            if attempt == MAX_RETRIES - 1:
                raise NoSearchResultsError() 
            
            time.sleep(RETRY_DELAY_SECONDS)

        except Exception:
            raise NoSearchResultsError()
            
    raise NoInternetError()