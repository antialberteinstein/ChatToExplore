package model.bo;

import model.dao.FigureDAO;

/**
 * Figure Business Object - xử lý logic nhân vật lịch sử
 */
public class FigureBO {
    
    private FigureDAO figureDAO;
    private static FigureBO instance = null;

    public FigureBO() {
        this.figureDAO = new FigureDAO();
        instance = this;
    }

    public static synchronized FigureBO getInstance() {
        if (instance == null) {
            instance = new FigureBO();
        }
        return instance;
    }

    /**
     * Backwards-compatible method that accepts a period string like "1228-1300"
     * and delegates to the new born/died signature.
     */
    public String createUserFigure(String username, String figureName, String period, String description) {
        Integer born = null;
        Integer died = null;
        if (period != null) {
            String[] parts = period.split("-");
            try {
                if (parts.length > 0 && parts[0].trim().length() > 0) born = Integer.parseInt(parts[0].trim());
                if (parts.length > 1 && parts[1].trim().length() > 0) died = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return createUserFigure(username, figureName, born, died, description, null, null);
    }
    
    /**
     * Lấy figure theo ID
     */
    public String getFigureById(String figureId) {
        if (figureId == null || figureId.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"ID nhân vật không được để trống\"}";
        }
        
        return figureDAO.getFigureById(figureId);
    }
    
    /**
     * Lấy tất cả figures của user (thông qua UserFigure)
     */
    public String getFiguresByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập không được để trống\"}";
        }
        
        return figureDAO.getFiguresByUsername(username);
    }
    
    /**
     * Tạo UserFigure mới (nếu Figure chưa tồn tại thì tạo mới)
     */
    public String createUserFigure(String username, String figureName, Integer born, Integer died, String description, String imageUrl, String hometown) {
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập không được để trống\"}";
        }
        
        if (figureName == null || figureName.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Tên nhân vật không được để trống\"}";
        }
        
        return figureDAO.createUserFigure(username.trim(), figureName.trim(), born, died, description, imageUrl, hometown);
    }

    /**
     * Cập nhật ảnh của một figure (cập nhật `image_url` trong DB)
     */
    public String updateFigureImage(String username, String figureId, String imageUrl) {
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập không được để trống\"}";
        }

        if (figureId == null || figureId.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"ID nhân vật không được để trống\"}";
        }

        long id;
        try {
            id = Long.parseLong(figureId.trim());
        } catch (NumberFormatException e) {
            return "{\"success\": false, \"message\": \"ID nhân vật không hợp lệ\"}";
        }

        boolean ok = figureDAO.updateFigureImage(id, imageUrl);
        if (ok) {
            return "{\"success\": true, \"message\": \"Cập nhật ảnh thành công\"}";
        } else {
            return "{\"success\": false, \"message\": \"Không thể cập nhật ảnh nhân vật\"}";
        }
    }

    /**
     * Trả về ID của figure theo tên (exact match) hoặc null nếu không tồn tại.
     */
    public Long getFigureIdByName(String figureName) {
        if (figureName == null || figureName.trim().isEmpty()) return null;
        return figureDAO.getFigureIdByName(figureName.trim());
    }

    /**
     * Cập nhật thông tin figure (delegate tới DAO)
     */
    public String updateFigureInfo(String username, String figureId, String name, Integer born, Integer died, String description, String hometown) {
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập không được để trống\"}";
        }

        if (figureId == null || figureId.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"ID nhân vật không được để trống\"}";
        }

        long id;
        try {
            id = Long.parseLong(figureId.trim());
        } catch (NumberFormatException e) {
            return "{\"success\": false, \"message\": \"ID nhân vật không hợp lệ\"}";
        }

        boolean ok = figureDAO.updateFigureInfo(id, name, description, born, died, hometown);
        if (ok) {
            return "{\"success\": true, \"message\": \"Cập nhật thông tin thành công\"}";
        } else {
            return "{\"success\": false, \"message\": \"Không thể cập nhật thông tin nhân vật\"}";
        }
    }

    /**
     * Xóa quan hệ user-figure (chỉ xóa relation cho user), trả JSON kết quả
     */
    public String removeUserFigure(String username, String figureId) {
        if (username == null || username.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"Tên đăng nhập không được để trống\"}";
        }
        if (figureId == null || figureId.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"ID nhân vật không được để trống\"}";
        }

        long id;
        try {
            id = Long.parseLong(figureId.trim());
        } catch (NumberFormatException e) {
            return "{\"success\": false, \"message\": \"ID nhân vật không hợp lệ\"}";
        }

        boolean ok = figureDAO.deleteUserFigureRelation(username.trim(), id);
        if (ok) {
            return "{\"success\": true, \"message\": \"Đã xóa nhân vật khỏi danh sách của bạn\"}";
        } else {
            return "{\"success\": false, \"message\": \"Không thể xóa nhân vật khỏi danh sách\"}";
        }
    }
}
