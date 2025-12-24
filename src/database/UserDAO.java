package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {


    public static boolean isUserExist(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // có dòng → tồn tại

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean register(String username, String password,
                                   String fullName, String email) {
        String sql = "INSERT INTO users(username, password, full_name, email) VALUES (?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password); // ⚠ nên hash
            ps.setString(3, fullName);
            ps.setString(4, email);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updateUserStatus(String username, String status) {
        String sql = "UPDATE peers SET status = ?, last_seen = NOW() WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, username);

            int rows = ps.executeUpdate();
            return rows > 0; // true nếu update thành công

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}

