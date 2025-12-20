package SQLites;

public class InitDB {

    public static void main(String[] args) {

        String sql = """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT,
                receiver TEXT,
                content TEXT,
                time DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        """;

        try (var con = SQLiteUtil.getConnection();
             var st = con.createStatement()) {

            st.execute(sql);
            MessageDAO.save("Bao", "Nam", "Hello Nam");
            System.out.println("SQLite DB initialized");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}