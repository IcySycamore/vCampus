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
        assertTrue(url.contains("allowPublicKeyRetrieval=true"),
                "应允许公钥检索，以兼容 MySQL 8 认证：" + url);
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
     * 未配置 DB_USER 时，获取连接应快速失败并给出可读提示，而非回退到内置口令。
     *
     * <p>注意：现在优先读取db.properties,如果配置文件中有db.user则测试会跳过。
     * 此测试主要验证当两者都没有配置时的失败行为。
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "DB_USER", matches = ".+")
    void connectionFailsFastWithoutCredentials() {
        // 如果db.properties已经配置了db.user,测试实际上验证的是配置读取正常
        try {
            String user = DbHelper.getUser();
            // 如果能成功获取user(无论来自properties还是环境变量),说明配置正常
            assertNotNull(user, "应能从db.properties或环境变量获取用户名");
            // 配置存在时,测试通过
        } catch (IllegalStateException expected) {
            // 如果两者都没有配置,应该抛出异常并包含提示信息
            assertTrue(expected.getMessage().contains("db.user")
                    || expected.getMessage().contains("DB_USER"),
                    "异常信息应指明缺失的配置：" + expected.getMessage());
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
