import edu.seu.vcampus.server.db.DbHelper;
import java.sql.*;

public class ShowTables {
    public static void main(String[] args) throws Exception {
        Connection conn = DbHelper.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getTables("vCampus", null, "%", new String[]{"TABLE"});
        System.out.println("=== vCampus 数据库现有表 ===");
        while (rs.next()) {
            System.out.println("  " + rs.getString("TABLE_NAME"));
        }
        rs.close();
        conn.close();
    }
}
