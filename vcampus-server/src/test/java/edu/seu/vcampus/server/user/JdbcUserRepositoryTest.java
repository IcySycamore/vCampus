package edu.seu.vcampus.server.user;

import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;
import edu.seu.vcampus.server.user.UserRepository.Credential;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assumptions;

/**
 * {@link JdbcUserRepository} 的真库集成测试。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过，因此本测试在没起数据库的机器上 不会把构建打红；起了库（`docker compose up -d
 * mysql`）之后自动变成真跑的集成测试。
 *
 * <p>
 * 前置：库中需有 {@code sql/vCampus.sql} 建出的 {@code tblUserCredential}。用例使用带时间戳前缀 的临时账号，跑完即删，不影响既有数据。
 */
class JdbcUserRepositoryTest {

    /** 临时账号前缀，便于识别与清理。 */
    private static final String PREFIX = "jdbc_it_";

    /** 本用例使用的登录名。 */
    private String m_username;

    /** 被测仓库。 */
    private JdbcUserRepository m_repository;

    /** 每个用例前确认数据库可用并准备唯一登录名。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过 JDBC 集成测试（docker compose up -d mysql 后自动执行）");
        m_username = PREFIX + System.currentTimeMillis();
        m_repository = new JdbcUserRepository();
    }

    /** 用例后清理临时账号。 */
    @AfterEach
    void tearDown() {
        if (m_repository == null || m_username == null) {
            return;
        }
        try {
            m_repository.delete(m_username);
        } catch (RuntimeException ignored) {
            // 数据库不可用时无需清理
        }
    }

    @Test
    void savesAndFindsByUsernameAndUuid() {
        m_repository.save(m_username, "uuid-" + m_username, "salt-1", "hash-1", "学生");

        Credential byName = m_repository.findByUsername(m_username);
        assertNotNull(byName, "按登录名应能查到刚存的凭证");
        assertEquals("uuid-" + m_username, byName.getUuid());
        assertEquals("salt-1", byName.getSalt());
        assertEquals("hash-1", byName.getHash());
        assertEquals("学生", byName.getRole());
        assertTrue(byName.isEnabled(), "新账号默认启用");
        assertEquals(m_username, byName.getDisplayName(), "未指定姓名时回落登录名");

        Credential byUuid = m_repository.findByUuid("uuid-" + m_username);
        assertNotNull(byUuid, "按 uuid 也应能查到");
        assertEquals(m_username, byUuid.getUsername());
    }

    @Test
    void saveIsUpsertSoRepeatDoesNotDuplicate() {
        m_repository.save(m_username, "uuid-" + m_username, "salt-a", "hash-a", "学生");
        m_repository.save(m_username, "uuid-" + m_username, "salt-b", "hash-b", "教师");

        List<Credential> all = m_repository.findAll();
        int matched = 0;
        for (Credential credential : all) {
            if (m_username.equals(credential.getUsername())) {
                matched++;
            }
        }
        assertEquals(1, matched, "同名重复保存应覆盖而不是插入第二行");
        assertEquals("教师", m_repository.findByUsername(m_username).getRole());
        assertEquals("hash-b", m_repository.findByUsername(m_username).getHash());
    }

    @Test
    void updatesNameEnabledAndCredential() {
        m_repository.save(m_username, "uuid-" + m_username, "salt-x", "hash-x", "学生");
        assertTrue(m_repository.exists(m_username));

        m_repository.update(m_username, "张三");
        assertEquals("张三", m_repository.findByUsername(m_username).getDisplayName());

        m_repository.setEnabled(m_username, false);
        assertFalse(m_repository.findByUsername(m_username).isEnabled(), "禁用位应落库");

        m_repository.updateCredential(m_username, "new-salt", "new-hash");
        assertEquals("new-salt", m_repository.findByUsername(m_username).getSalt());
        assertEquals("new-hash", m_repository.findByUsername(m_username).getHash());
    }

    @Test
    void deletesAccount() {
        m_repository.save(m_username, "uuid-" + m_username, "salt-c", "hash-c", "教师");

        m_repository.delete(m_username);

        assertNull(m_repository.findByUsername(m_username), "删除后不应再查到");
        assertFalse(m_repository.exists(m_username));
    }

    /**
     * 探测数据库是否可用。
     *
     * @return 能取到连接返回 true
     */
    private static boolean databaseAvailable() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            // 连得上不代表建表脚本跑过；缺表时应当整体跳过，而不是抛一堆 Table doesn't exist
            return connection != null && DatabaseAvailability.isReady();
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // 探测用连接，关闭失败无影响
                }
            }
        }
    }
}
