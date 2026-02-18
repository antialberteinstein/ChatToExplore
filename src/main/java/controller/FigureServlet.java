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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import model.bo.FigureBO;

/**
 * Figure Servlet - xử lý thông tin nhân vật lịch sử
 */
@WebServlet("/figure")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024)
public class FigureServlet extends HttpServlet {

    private FigureBO figureBO;

    public FigureServlet() {
        this.figureBO = FigureBO.getInstance();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Kiểm tra authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print("{\"success\": false, \"message\": \"Vui lòng đăng nhập để xem nhân vật\"}");
            out.flush();
            return;
        }
        
        String username = (String) session.getAttribute("username");
        String action = request.getParameter("action");
        String jsonResult;
        
        if ("all".equals(action)) {
            // Lấy tất cả figures của user hiện tại (thông qua UserFigure)
            jsonResult = figureBO.getFiguresByUsername(username);
        } else if ("each".equals(action)) {
            // Lấy một figure cụ thể từ id
            String figureId = request.getParameter("id");
            if (figureId == null || figureId.trim().isEmpty()) {
                jsonResult = "{\"success\": false, \"message\": \"Thiếu tham số id\"}";
            } else {
                jsonResult = figureBO.getFigureById(figureId);
            }
        } else {
            jsonResult = "{\"success\": false, \"message\": \"Tham số action không hợp lệ (all hoặc each)\"}";
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.print(jsonResult);
        out.flush();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Kiểm tra authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print("{\"success\": false, \"message\": \"Vui lòng đăng nhập để thêm nhân vật\"}");
            out.flush();
            return;
        }

        String username = (String) session.getAttribute("username");

        String action = request.getParameter("action");

        // If frontend is checking only by name (first-step flow)
        if ("checkByName".equals(action)) {
            String figureName = request.getParameter("figureName");
            if (figureName == null || figureName.trim().isEmpty()) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                out.print("{\"success\": false, \"message\": \"Thiếu tên nhân vật\"}");
                out.flush();
                return;
            }

            // Trim and normalize
            figureName = figureName.trim();

            Long existingId = figureBO.getFigureIdByName(figureName);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            if (existingId != null) {
                // Figure exists: simply add a UserFigure relation for current user
                String result = figureBO.createUserFigure(username, figureName, (Integer) null, (Integer) null, null, null, null);
                out.print(result);
                out.flush();
                return;
            } else {
                // Figure not found: instruct frontend to show full details form
                out.print("{\"success\": false, \"needsDetails\": true, \"message\": \"Chưa có nhân vật, vui lòng nhập thêm thông tin\"}");
                out.flush();
                return;
            }
        }

        // Nếu là cập nhật ảnh cho figure
        if ("updateImage".equals(action)) {
            String figureId = request.getParameter("figureId");
            if (figureId == null || figureId.trim().isEmpty()) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                out.print("{\"success\": false, \"message\": \"Thiếu tham số figureId\"}");
                out.flush();
                return;
            }
            // If frontend already uploaded via /upload-image, it will pass imageUrl parameter
            String imageUrl = request.getParameter("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                // Otherwise try to process a multipart file upload (backward-compatible)
                try {
                    Part imagePart = request.getPart("image");
                    if (imagePart != null && imagePart.getSize() > 0) {
                        String submitted = imagePart.getSubmittedFileName();
                        String ext = "";
                        if (submitted != null && submitted.contains(".")) {
                            ext = submitted.substring(submitted.lastIndexOf('.'));
                        }

                        String fileName = UUID.randomUUID().toString() + ext;
                        // Save to default ~/FinalProject/images to match ImageUploadServlet behavior
                        String userHome = System.getProperty("user.home");
                        String imagesPath = userHome + File.separator + "FinalProject" + File.separator + "images";
                        File imagesDir = new File(imagesPath);
                        if (!imagesDir.exists()) imagesDir.mkdirs();

                        File outFile = new File(imagesDir, fileName);
                        try (InputStream in = imagePart.getInputStream()) {
                            Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Attempt to copy into webapp images folder so the image becomes web-accessible
                        String webappImagesRealPath = getServletContext().getRealPath("/images");
                        if (webappImagesRealPath != null) {
                            try {
                                File webappDir = new File(webappImagesRealPath);
                                if (!webappDir.exists()) webappDir.mkdirs();
                                Files.copy(outFile.toPath(), Paths.get(webappImagesRealPath, fileName), StandardCopyOption.REPLACE_EXISTING);
                                // build web URL
                                String webUrl = request.getScheme() + "://" + request.getServerName();
                                if (("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() != 80)
                                        || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() != 443)) {
                                    webUrl += ":" + request.getServerPort();
                                }
                                webUrl += request.getContextPath() + "/images/" + fileName;
                                imageUrl = webUrl;
                            } catch (Exception ex) {
                                System.err.println("Warning: failed to copy image to webapp images: " + ex.getMessage());
                                imageUrl = imagesPath + File.separator + fileName;
                            }
                        } else {
                            imageUrl = imagesPath + File.separator + fileName;
                        }
                    }
                } catch (IllegalStateException | IOException | ServletException ex) {
                    System.err.println("Image upload failed: " + ex.getMessage());
                }
            }

            String result = figureBO.updateFigureImage(username, figureId, imageUrl);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print(result);
            out.flush();
            return;
        }

        // Nếu là cập nhật thông tin cho figure
        if ("updateInfo".equals(action)) {
            String figureId = request.getParameter("figureId");
            String name = request.getParameter("figureName");
            String bornYear = request.getParameter("bornYear");
            String diedYear = request.getParameter("diedYear");
            String hometown = request.getParameter("hometown");
            String description = request.getParameter("description");

            Integer born = null;
            Integer died = null;
            try {
                if (bornYear != null && !bornYear.trim().isEmpty()) born = Integer.parseInt(bornYear.trim());
            } catch (NumberFormatException ignored) {}
            try {
                if (diedYear != null && !diedYear.trim().isEmpty()) died = Integer.parseInt(diedYear.trim());
            } catch (NumberFormatException ignored) {}

            String result = figureBO.updateFigureInfo(username, figureId, name, born, died, description, hometown);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out2 = response.getWriter();
            out2.print(result);
            out2.flush();
            return;
        }

        // Nếu là xóa figure khỏi user (chỉ xóa relation user_figures)
        if ("removeUserFigure".equals(action)) {
            String figureId = request.getParameter("figureId");
            String result = figureBO.removeUserFigure(username, figureId);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out3 = response.getWriter();
            out3.print(result);
            out3.flush();
            return;
        }

        // default: tạo nhân vật mới (nguyên code)
        String figureName = request.getParameter("figureName");
        String bornYear = request.getParameter("bornYear");
        String diedYear = request.getParameter("diedYear");
        String hometown = request.getParameter("hometown");
        String description = request.getParameter("description");

        // Parse born/died into Integer values
        Integer born = null;
        Integer died = null;
        try {
            if (bornYear != null && !bornYear.trim().isEmpty()) {
                born = Integer.parseInt(bornYear.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        try {
            if (diedYear != null && !diedYear.trim().isEmpty()) {
                died = Integer.parseInt(diedYear.trim());
            }
        } catch (NumberFormatException ignored) {
        }

        // Handle image upload
        String imageUrl = null;
        // First, accept an `imageUrl` request parameter (uploaded earlier via /upload-image)
        String paramImageUrl = request.getParameter("imageUrl");
        if (paramImageUrl != null && !paramImageUrl.trim().isEmpty()) {
            imageUrl = paramImageUrl.trim();
        } else {
            // Otherwise try to process a multipart file upload (backward-compatible)
            try {
                Part imagePart = request.getPart("image");
                if (imagePart != null && imagePart.getSize() > 0) {
                    String submitted = imagePart.getSubmittedFileName();
                    String ext = "";
                    if (submitted != null && submitted.contains(".")) {
                        ext = submitted.substring(submitted.lastIndexOf('.'));
                    }

                    String fileName = UUID.randomUUID().toString() + ext;
                    // Save to default ~/FinalProject/images to match ImageUploadServlet behavior
                    String userHome = System.getProperty("user.home");
                    String imagesPath = userHome + File.separator + "FinalProject" + File.separator + "images";
                    File imagesDir = new File(imagesPath);
                    if (!imagesDir.exists()) imagesDir.mkdirs();

                    File outFile = new File(imagesDir, fileName);
                    try (InputStream in = imagePart.getInputStream()) {
                        Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Attempt to copy into webapp images folder so the image becomes web-accessible
                    String webappImagesRealPath = getServletContext().getRealPath("/images");
                    if (webappImagesRealPath != null) {
                        try {
                            File webappDir = new File(webappImagesRealPath);
                            if (!webappDir.exists()) webappDir.mkdirs();
                            Files.copy(outFile.toPath(), Paths.get(webappImagesRealPath, fileName), StandardCopyOption.REPLACE_EXISTING);
                            // build web URL
                            String webUrl = request.getScheme() + "://" + request.getServerName();
                            if (("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() != 80)
                                    || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() != 443)) {
                                webUrl += ":" + request.getServerPort();
                            }
                            webUrl += request.getContextPath() + "/images/" + fileName;
                            imageUrl = webUrl;
                        } catch (Exception ex) {
                            System.err.println("Warning: failed to copy image to webapp images: " + ex.getMessage());
                            imageUrl = imagesPath + File.separator + fileName;
                        }
                    } else {
                        imageUrl = imagesPath + File.separator + fileName;
                    }
                }
            } catch (IllegalStateException | IOException | ServletException ex) {
                System.err.println("Image upload failed: " + ex.getMessage());
            }
        }
        
        // Validate input
        if (figureName == null || figureName.trim().isEmpty()) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print("{\"success\": false, \"message\": \"Tên nhân vật không được để trống\"}");
            out.flush();
            return;
        }
        
        String jsonResult = figureBO.createUserFigure(username, figureName, born, died, description, imageUrl, hometown);
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.print(jsonResult);
        out.flush();
    }
}
