import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 简单的数据库连接测试工具
 */
public class TestDbConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/vCampus?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
        String user = "root";
        String password = "";

        System.out.println("=== 数据库连接测试 ===");
        System.out.println("URL: " + url);
        System.out.println("User: " + user);
        System.out.println();

        try {
            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL 驱动加载成功");

            // 获取连接
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✓ 数据库连接成功");

            // 测试查询
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATABASE() as db, VERSION() as version");

            if (rs.next()) {
                System.out.println("✓ 查询执行成功");
                System.out.println("  当前数据库: " + rs.getString("db"));
                System.out.println("  MySQL 版本: " + rs.getString("version"));
            }

            // 检查表是否存在
            rs = stmt.executeQuery("SHOW TABLES LIKE 'tblShopItem'");
            if (rs.next()) {
                System.out.println("✓ 表 tblShopItem 存在");
            } else {
                System.out.println("⚠ 表 tblShopItem 不存在，需要执行建表脚本");
            }

            rs.close();
            stmt.close();
            conn.close();

            System.out.println();
            System.out.println("=== 测试完成：所有检查通过 ===");

        } catch (ClassNotFoundException e) {
            System.err.println("✗ MySQL 驱动未找到");
            System.err.println("  请确保 pom.xml 中包含 mysql-connector-java 依赖");
            e.printStackTrace();
        } catch (java.sql.SQLException e) {
            System.err.println("✗ 数据库连接失败");
            System.err.println("  错误信息: " + e.getMessage());
            System.err.println();
            System.err.println("可能的原因：");
            System.err.println("  1. MySQL 服务未启动");
            System.err.println("  2. 数据库 vCampus 不存在（需要先执行 sql/vCampus.sql）");
            System.err.println("  3. 用户名或密码不正确");
            System.err.println("  4. 端口号错误（默认 3306）");
        }
    }
}
