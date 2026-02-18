package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import config.DatabaseManager;

/**
 * FlagDAO - quản lý bảng `flags`
 *
 * Hai hàm chính:
 * - enableFlag(String username, String flag)
 * - disableFlag(String username, String flag)
 *
 * Nếu hàng cho `username` chưa tồn tại sẽ được tạo với tất cả các cờ = false.
 */
public class FlagDAO {

	// Chỉ cho phép cập nhật những cột đã định nghĩa ở đây để tránh SQL injection
	private static final Set<String> VALID_FLAGS = Set.of("figure_flag");

	private final DatabaseManager dbManager;

	public FlagDAO() {
		this.dbManager = DatabaseManager.getInstance();
	}

    public boolean isFlagEnabled(String username, String flag) {
        if (username == null || username.isBlank()) return false;
        if (flag == null || !VALID_FLAGS.contains(flag)) {
            System.err.println("Invalid flag name: " + flag);
            return false;
        }

        try (Connection connection = dbManager.getConnection()) {
            String selectSql = "SELECT " + flag + " FROM flags WHERE username = ?";
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean(flag);
                    } else {
                        return false; // user không tồn tại, coi như cờ tắt
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error in FlagDAO.isFlagEnabled: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

	public boolean enableFlag(String username, String flag) {
		return setFlag(username, flag, true);
	}

	public boolean disableFlag(String username, String flag) {
		return setFlag(username, flag, false);
	}

	private boolean setFlag(String username, String flag, boolean value) {
		if (username == null || username.isBlank()) return false;
		if (flag == null || !VALID_FLAGS.contains(flag)) {
			System.err.println("Invalid flag name: " + flag);
			return false;
		}

		try (Connection connection = dbManager.getConnection()) {
			// Ensure row exists for the user
			ensureUserRowExists(connection, username);

			// Safe to concatenate the validated column name
			String updateSql = "UPDATE flags SET " + flag + " = ? WHERE username = ?";
			try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
				ps.setBoolean(1, value);
				ps.setString(2, username);
				int affected = ps.executeUpdate();
				return affected > 0;
			}

		} catch (SQLException e) {
			System.err.println("Database error in FlagDAO.setFlag: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Kiểm tra xem đã có row cho username chưa; nếu chưa có thì insert với tất cả cờ = false
	 */
	private void ensureUserRowExists(Connection connection, String username) throws SQLException {
		String selectSql = "SELECT username FROM flags WHERE username = ?";
		try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return; // tồn tại
				}
			}
		}

		// Nếu chưa tồn tại thì chèn với các cờ mặc định = FALSE
		// Hiện tại chỉ có `figure_flag` trong schema
		String insertSql = "INSERT INTO flags (username, figure_flag) VALUES (?, FALSE)";
		try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
			ps.setString(1, username);
			ps.executeUpdate();
		} catch (SQLException e) {
			// Có thể xảy ra race condition nếu một thread khác vừa insert
			if (e.getMessage().toLowerCase().contains("duplicate")) {
				// ignore
			} else {
				throw e;
			}
		}
	}

}
