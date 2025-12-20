package SQLites;

public class MessageDAO {

    public static void save(String sender, String receiver, String content) {

        String sql =
                "INSERT INTO messages(sender, receiver, content) VALUES(?,?,?)";

        try (var con = SQLiteUtil.getConnection();
             var ps = con.prepareStatement(sql)) {

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();

            System.out.println("💾 Message saved");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
