package edu.seu.vcampus.server.student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC DAO 测试的公共支撑：判断「能不能真跑数据库测试」，以及清表。
 *
 * <p>
 * <b>为什么宁可跳过也不失败</b>：本机没装 MySQL、或者装了但没执行 {@code sql/vCampus.sql}，都是
 * 「环境没准备好」，不是代码错。这类情况一律跳过（见 ADR-0005：集成测试按环境门控），但会在控制台
 * 打印一句说明，免得「全绿」让人误以为 JDBC 实现被验证过了。
 *
 * <p>
 * 真正的验证发生在两处：本地按 {@code sql/vCampus.sql} 建好库后运行本组测试；CI 里由 mysql 服务
 * 提供数据库（{@code ci.yml} 已导出 DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD）。
 */
final class StudentJdbcTestSupport {

    /** 档案表名。 */
    static final String PROFILE_TABLE = "tblCampusProfile";

    /** 申请单表名。 */
    static final String REQUEST_TABLE = "tblModifyRequest";

    /** 私有构造器，禁止实例化工具类。 */
    private StudentJdbcTestSupport() {
    }

    /**
     * 库连得上且两张表都在时返回连接来源，否则返回 null（调用方据此跳过用例）。
     *
     * @return 连接来源；不可用时返回 null
     */
    static StudentDataSource dataSourceOrNull() {
        StudentDataSource source = new StudentDataSource();
        try (Connection connection = source.getConnection()) {
            if (!exists(connection, PROFILE_TABLE)) {
                System.out.println("跳过 JDBC 学籍测试：数据库里没有 " + PROFILE_TABLE
                        + "，请先执行 sql/vCampus.sql（当前 " + source.getUrl() + "）");
                return null;
            }
            if (!exists(connection, REQUEST_TABLE)) {
                System.out.println("跳过 JDBC 学籍测试：数据库里没有 " + REQUEST_TABLE
                        + "，请先执行 sql/vCampus.sql（当前 " + source.getUrl() + "）");
                return null;
            }
            return source;
        } catch (SQLException exception) {
            System.out.println("跳过 JDBC 学籍测试：连不上数据库（" + exception.getMessage() + "）");
            return null;
        }
    }

    /**
     * 清空一张表，让每条用例从干净状态开始（否则上一条插入的数据会让计数断言飘）。
     *
     * @param source 连接来源
     * @param table 表名
     */
    static void clear(StudentDataSource source, String table) {
        try (Connection connection = source.getConnection();
                PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE " + table)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            System.out.println("清表 " + table + " 失败：" + exception.getMessage());
        }
    }

    /**
     * 判断表是否存在。
     *
     * @param connection 连接
     * @param table 表名
     * @return 存在返回 true
     * @throws SQLException 查询失败
     */
    private static boolean exists(Connection connection, String table) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            return true;
        } catch (SQLException missing) {
            return false;
        }
    }
}
