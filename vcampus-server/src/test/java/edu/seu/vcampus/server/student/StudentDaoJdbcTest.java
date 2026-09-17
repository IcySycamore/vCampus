package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StudentDaoJdbc} 的真库集成测试。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过，没起库的机器上不会把构建打红。 前置是库中已有 {@code tblStudentProfile}（见
 * sql/vCampus.sql）。
 *
 * <p>
 * 用例的 {@code uUuid} 带时间戳，跑完按 uuid 物理删除，不污染既有数据；<b>不使用固定 uuid</b>， 否则会撞上 {@code uUuid}
 * 唯一键、把上一次运行的记录改掉。
 */
class StudentDaoJdbcTest {

    /** 本用例使用的用户 uuid。 */
    private String m_userUuid;

    /** 被测 DAO。 */
    private StudentDaoJdbc m_dao;

    /** 每个用例前确认数据库可用并生成唯一 uuid。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过 JDBC 集成测试（docker compose up -d mysql 后自动执行）");
        m_userUuid = "jdbc_sp_test_" + System.currentTimeMillis();
        m_dao = new StudentDaoJdbc();
    }

    /** 用例后物理删除测试数据（软删除的记录要直接清掉）。 */
    @AfterEach
    void tearDown() {
        hardDelete(m_userUuid);
    }

    @Test
    void insertBackfillsIdAndReadsBackAllFields() {
        StudentProfile profile = new StudentProfile(m_userUuid, PersonCategory.STUDENT, 2026,
                CampusStatus.ENROLLED);
        profile.setStudentNo("2026001");
        profile.setField("计算机科学与技术");

        assertTrue(m_dao.insert(profile), "插入应成功");
        assertNotNull(profile.getId(), "插入后应回填自增主键");

        StudentProfile byId = m_dao.findById(profile.getId());
        assertNotNull(byId, "按主键应能查到");
        assertEquals(m_userUuid, byId.getUserUuid());
        assertEquals(PersonCategory.STUDENT, byId.getPersonCategory());
        assertEquals(CampusStatus.ENROLLED, byId.getStatus());
        assertEquals("2026001", byId.getStudentNo());
        assertEquals("计算机科学与技术", byId.getField());
        assertEquals(2026, byId.getJoinYear());
        assertFalse(byId.isDeleted());

        StudentProfile byUuid = m_dao.findByUserUuid(m_userUuid);
        assertNotNull(byUuid, "按用户 uuid 应能查到");
        assertEquals(profile.getId(), byUuid.getId());
    }

    @Test
    void findAllContainsInsertedRecord() {
        StudentProfile profile = new StudentProfile(m_userUuid, PersonCategory.TEACHER, 2020,
                CampusStatus.ENROLLED);
        m_dao.insert(profile);

        boolean found = false;
        List<StudentProfile> all = m_dao.findAll();
        for (StudentProfile item : all) {
            if (m_userUuid.equals(item.getUserUuid())) {
                found = true;
            }
        }
        assertTrue(found, "列表里应包含刚插入的档案");
    }

    @Test
    void updateChangesStoredFields() {
        StudentProfile profile = new StudentProfile(m_userUuid, PersonCategory.STUDENT, 2025,
                CampusStatus.ENROLLED);
        profile.setField("软件工程");
        m_dao.insert(profile);

        profile.setField("人工智能");
        profile.setStatus(CampusStatus.GRADUATED);
        profile.setStudentNo("2025001");
        assertTrue(m_dao.update(profile), "更新应命中一条记录");

        StudentProfile reloaded = m_dao.findByUserUuid(m_userUuid);
        assertEquals("人工智能", reloaded.getField());
        assertEquals(CampusStatus.GRADUATED, reloaded.getStatus());
        assertEquals("2025001", reloaded.getStudentNo());
    }

    @Test
    void softDeleteHidesRecordButKeepsRow() {
        StudentProfile profile = new StudentProfile(m_userUuid, PersonCategory.STUDENT, 2026,
                CampusStatus.ENROLLED);
        m_dao.insert(profile);

        assertTrue(m_dao.softDelete(profile.getId()), "软删除应命中一条记录");
        assertNull(m_dao.findById(profile.getId()), "软删除后按主键查不到");
        assertNull(m_dao.findByUserUuid(m_userUuid), "软删除后按 uuid 也查不到");

        boolean found = false;
        for (StudentProfile item : m_dao.findAll()) {
            if (m_userUuid.equals(item.getUserUuid())) {
                found = true;
            }
        }
        assertFalse(found, "列表里不应再出现已删除的档案");
        assertTrue(rowExists(m_userUuid), "软删除只是置标记，行必须还在");
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

    /**
     * 物理删除测试数据。
     *
     * @param userUuid 用户 uuid；null 时不做处理
     */
    private static void hardDelete(String userUuid) {
        if (userUuid == null) {
            return;
        }
        run("DELETE FROM tblStudentProfile WHERE uUuid = ?", userUuid);
    }

    /**
     * 判断记录行是否仍存在（含已软删除）。
     *
     * @param userUuid 用户 uuid
     * @return 存在返回 true
     */
    private static boolean rowExists(String userUuid) {
        Connection connection = null;
        PreparedStatement statement = null;
        java.sql.ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM tblStudentProfile WHERE uUuid = ?");
            statement.setString(1, userUuid);
            rows = statement.executeQuery();
            return rows.next() && rows.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        } finally {
            if (rows != null) {
                try {
                    rows.close();
                } catch (SQLException ignored) {
                    // 收尾忽略
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                    // 收尾忽略
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // 收尾忽略
                }
            }
        }
    }

    /**
     * 执行一条带单参数的写语句（测试清理用）。
     *
     * @param sql   语句
     * @param param 参数
     */
    private static void run(String sql, String param) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, param);
            statement.executeUpdate();
        } catch (SQLException e) {
            // 数据库不可用时无需清理
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                    // 收尾忽略
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // 收尾忽略
                }
            }
        }
    }
}
