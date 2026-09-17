import java.sql.*;

public class TestShopDb {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/vcampus?useSSL=false&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "123465";
        
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✓ 数据库连接成功");
            
            String sql = "SELECT siId, siName, siPrice, siStock FROM tblShopItem";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                System.out.println("\n商品列表：");
                System.out.println("----------------------------------------");
                while (rs.next()) {
                    System.out.printf("ID: %s | %s | ¥%.2f | 库存: %d\n",
                        rs.getString("siId"),
                        rs.getString("siName"),
                        rs.getDouble("siPrice"),
                        rs.getInt("siStock"));
                }
                System.out.println("----------------------------------------");
                System.out.println("\n✓ 数据库查询成功");
                System.out.println("✓ 你的数据库工作完全正常！");
            }
        } catch (Exception e) {
            System.err.println("✗ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
