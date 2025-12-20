package SQLites;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public class SQLiteUtil {

    private static final String DB_PATH =
            System.getProperty("user.dir") + "\\MasterChat\\data\\message.db";

    static {
        try {
            File dir = new File(System.getProperty("user.dir") + "\\MasterChat\\data");
            if (!dir.exists()) {
                dir.mkdirs(); // Tạo thư mục data
            }

            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite driver loaded");
            System.out.println("DB path = " + DB_PATH);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }
}