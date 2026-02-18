package controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Image Upload Servlet - xử lý upload ảnh vào folder images
 */
@WebServlet("/upload-image")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB
    maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class ImageUploadServlet extends HttpServlet {
    
    private static final String UPLOAD_DIR = "images";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Kiểm tra authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            out.print("{\"success\": false, \"message\": \"Vui lòng đăng nhập để upload ảnh\"}");
            out.flush();
            return;
        }
        
        try {
            // Lấy file từ request
            Part filePart = request.getPart("image");
            
            if (filePart == null) {
                out.print("{\"success\": false, \"message\": \"Vui lòng chọn file ảnh\"}");
                out.flush();
                return;
            }
            
            // Lấy tên file gốc
            String originalFileName = getFileName(filePart);
            
            if (originalFileName == null || originalFileName.isEmpty()) {
                out.print("{\"success\": false, \"message\": \"Tên file không hợp lệ\"}");
                out.flush();
                return;
            }
            
            // Kiểm tra extension
            String fileExtension = getFileExtension(originalFileName);
            if (!isAllowedExtension(fileExtension)) {
                out.print("{\"success\": false, \"message\": \"Chỉ chấp nhận file ảnh: JPG, JPEG, PNG, GIF, WEBP\"}");
                out.flush();
                return;
            }
            
            // Kiểm tra kích thước file
            if (filePart.getSize() > MAX_FILE_SIZE) {
                out.print("{\"success\": false, \"message\": \"File quá lớn. Kích thước tối đa: 10MB\"}");
                out.flush();
                return;
            }
            
            // Tạo tên file unique để tránh trùng lặp
            String uniqueFileName = generateUniqueFileName(fileExtension);
            
            // Lấy đường dẫn thực tế của folder images
            // Mặc định lưu vào ~/FinalProject/images (thư mục FinalProject trong home)
            String userHome = System.getProperty("user.home");
            String uploadPath = userHome + File.separator + "FinalProject" + File.separator + UPLOAD_DIR;
            
            // Tạo folder images nếu chưa tồn tại
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // Đường dẫn file đầy đủ
            String filePath = uploadPath + File.separator + uniqueFileName;
            
            // Lưu file
            Files.copy(filePart.getInputStream(), 
                      Paths.get(filePath), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            // Cố gắng sao chép thêm vào folder images của webapp để có thể được phục vụ tĩnh
            String webappImagesRealPath = getServletContext().getRealPath("/images");
            if (webappImagesRealPath != null) {
                try {
                    File webappDir = new File(webappImagesRealPath);
                    if (!webappDir.exists()) webappDir.mkdirs();
                    Files.copy(Paths.get(filePath),
                               Paths.get(webappImagesRealPath, uniqueFileName),
                               StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    System.err.println("Warning: failed to copy uploaded image to webapp images folder: " + ex.getMessage());
                }
            }

            // Trả về URL có thể truy cập từ trình duyệt (http(s)://host:port/<context>/images/<file>)
            String webUrl = request.getScheme() + "://" + request.getServerName();
            if (("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() != 80)
                    || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() != 443)) {
                webUrl += ":" + request.getServerPort();
            }
            webUrl += request.getContextPath() + "/images/" + uniqueFileName;

            // Return both the web URL (for immediate viewing) and the absolute filesystem path
            String imagePathJson = filePath.replace("\\", "\\\\"); // escape backslashes for JSON
            out.print("{\"success\": true, \"message\": \"Upload ảnh thành công\", \"imageUrl\": \"" + webUrl + "\", \"imagePath\": \"" + imagePathJson + "\"}");
            
        } catch (Exception e) {
            System.err.println("Error uploading image: " + e.getMessage());
            e.printStackTrace();
            out.print("{\"success\": false, \"message\": \"Lỗi khi upload ảnh: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            out.flush();
        }
    }
    
    /**
     * Lấy tên file từ Part
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }
        
        for (String content : contentDisposition.split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    /**
     * Lấy extension từ tên file
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex).toLowerCase();
    }
    
    /**
     * Kiểm tra extension có hợp lệ không
     */
    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tạo tên file unique
     */
    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
