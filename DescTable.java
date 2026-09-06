import edu.seu.vcampus.server.db.DbHelper;
import java.sql.*;

public class DescTable {
    public static void main(String[] args) throws Exception {
        Connection conn = DbHelper.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("DESC tbluser");
        System.out.println("=== tbluser 表结构 ===");
        while (rs.next()) {
            System.out.println(rs.getString("Field") + " | " + rs.getString("Type") + " | " + rs.getString("Null") + " | " + rs.getString("Key"));
        }
        rs.close();
        stmt.close();
        conn.close();
    }
}
