package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public static boolean isUserExist(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // true nếu tồn tại

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Update IP và Port khi user đăng nhập
    public static boolean updateIpPort(String ip, int port, String status, String username) {
        String sql = "UPDATE users SET ip = ?, port = ?, status = ? WHERE username = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, ip);
            ps.setInt(2, port);
            ps.setString(3, status);
            ps.setString(4, username);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updateStatus(String status, String username) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, username);

            return ps.executeUpdate() > 0; // true nếu update thành công

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}

