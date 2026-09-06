import edu.seu.vcampus.server.db.DbHelper;
import java.sql.Connection;

public class DbTest {
    public static void main(String[] args) {
        try {
            Connection conn = DbHelper.getConnection();
            System.out.println("连接成功: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("数据库: " + conn.getCatalog());
            conn.close();
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
