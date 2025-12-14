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
}