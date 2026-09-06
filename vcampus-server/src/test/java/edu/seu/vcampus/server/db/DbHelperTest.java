package edu.seu.vcampus.server.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

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
 * <p>不依赖数据库的用例始终执行；真连数据库的集成用例以环境变量 {@code DB_NAME}
 * 门控，本地无 MySQL 时自动跳过（见 ADR-0005）。
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
        assertTrue(url.contains("serverTimezone=UTC"), "应指定时区：" + url);
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
     * 未配置 DB_USER 时，获取连接应快速失败并给出可读提示，而非回退到内置口令。
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "DB_USER", matches = ".+")
    void connectionFailsFastWithoutCredentials() {
        try {
            DbHelper.getConnection();
            fail("缺少 DB_USER 时应抛出 IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("DB_USER"),
                    "异常信息应指明缺失的变量名：" + expected.getMessage());
        } catch (SQLException e) {
            fail("应在建立连接前就因缺少凭据而失败：" + e.getMessage());
        }
    }

    /**
     * 集成用例：配置了数据库环境变量时应能真正建立连接。
     *
     * @throws SQLException 连接失败时抛出
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DB_NAME", matches = ".+")
    void connectsWhenConfigured() throws SQLException {
        Connection conn = DbHelper.getConnection();
        try {
            assertNotNull(conn, "应返回可用连接");
            assertFalse(conn.isClosed(), "返回的连接不应已关闭");
        } finally {
            conn.close();
        }
    }
}
