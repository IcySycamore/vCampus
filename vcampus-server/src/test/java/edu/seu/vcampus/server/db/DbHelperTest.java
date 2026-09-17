package edu.seu.vcampus.server.db;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * DbHelper 测试：连接串组装规则与凭据来源。
 *
 * <p>
 * 不依赖数据库的用例始终执行；真连数据库的集成用例以环境变量 {@code DB_NAME} 门控，本地无 MySQL 时自动跳过（见 ADR-0005）。
 */
class DbHelperTest {

    /**
     * 连接串应为 MySQL JDBC 格式，并带上项目约定的连接参数。
     */
    @Test
    void urlHasExpectedShape() {
        String url = DbHelper.getUrl();

        assertTrue(url.startsWith("jdbc:mysql://"), "应为 MySQL JDBC 连接串：" + url);
        assertTrue(url.contains("useSSL=false"), "应显式关闭 SSL：" + url);
        assertTrue(url.contains("serverTimezone="), "应指定时区：" + url);
        assertTrue(url.contains("characterEncoding=utf8"), "应指定编码：" + url);
    }

    /**
     * 连接串中不得出现用户名与密码：凭据只经 DriverManager 参数传递，避免随日志外泄。
     */
    @Test
    void urlCarriesNoCredentials() {
        String url = DbHelper.getUrl();

        assertFalse(url.contains("user="), "连接串不应携带用户名：" + url);
        assertFalse(url.contains("password="), "连接串不应携带密码：" + url);
    }

    /**
     * 多次调用应得到一致的连接串（同一份环境变量下结果稳定）。
     */
    @Test
    void urlIsStableAcrossCalls() {
        assertEquals(DbHelper.getUrl(), DbHelper.getUrl());
    }

    /**
     * 连接参数必须能从配置取到；不再有「缺配置就回退到内置口令」这条路。
     *
     * <p>
     * 原先这里写的是一个 try/catch 分支的测试，但 getConfig 对缺失值只会返回空串、不会抛异常， catch
     * 分支从来没被执行过——它看起来在验证「快速失败」，实际什么都没验。
     */
    @Test
    void credentialsComeFromConfiguration() {
        assertNotNull(DbHelper.getUser(), "db.user 必须配置");
        assertNotNull(DbHelper.getPassword(), "db.password 必须能取到");
        assertTrue(DbHelper.getUser().length() > 0, "db.user 不应为空");
    }

    /**
     * 集成用例：数据库可用时应能真正建立连接。
     *
     * <p>
     * 门控原先写的是 {@code @EnabledIfEnvironmentVariable("DB_NAME")}，要求环境变量存在； 而本地是在 db.properties
     * 里配的连接，于是同一个库上、其它真库测试都跑了，这一个却静默跳过。 统一改成 {@link DatabaseAvailability}，与其它真库测试同一口径。
     *
     * @throws SQLException 连接失败时抛出
     */
    @Test
    void connectsWhenConfigured() throws SQLException {
        Assumptions.assumeTrue(DatabaseAvailability.isReady(), "MySQL 不可用，跳过");

        Connection conn = DbHelper.getConnection();
        try {
            assertNotNull(conn, "应返回可用连接");
            assertFalse(conn.isClosed(), "返回的连接不应已关闭");
        } finally {
            conn.close();
        }
    }
}
