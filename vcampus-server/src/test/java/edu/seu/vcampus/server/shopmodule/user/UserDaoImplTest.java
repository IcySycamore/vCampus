package edu.seu.vcampus.server.shopmodule.user;

import edu.seu.vcampus.common.user.HumanInfo;
import edu.seu.vcampus.common.user.Student;
import edu.seu.vcampus.common.user.User;
import edu.seu.vcampus.server.db.DbHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserDaoImpl 集成测试：真连 MySQL 验证 SQL 正确性（见 ADR-0005）。
 *
 * <p>全类以环境变量 {@code DB_NAME} 门控，本地无数据库时自动跳过；CI 由 MySQL
 * service 注入连接信息。用例依赖 {@code sql/vCampus.sql} 写入的演示账号 001/002/003，
 * 新增类用例自行清理落库数据，避免影响后续测试。
 */
@EnabledIfEnvironmentVariable(named = "DB_NAME", matches = ".+")
class UserDaoImplTest {

    /** 被测对象。 */
    private final UserDao dao = new UserDaoImpl();

    /** 新增用例使用的登录ID，避免与演示数据冲突。 */
    private static final String TEMP_ID = "t901";

    /**
     * 删除临时用户，保证用例可重复执行。
     *
     * @throws SQLException 清理失败时抛出
     */
    private void deleteTempUser() throws SQLException {
        String sql = "DELETE FROM tblUser WHERE uId = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, TEMP_ID);
            stmt.executeUpdate();
        }
    }

    /**
     * 按登录ID应能查到演示账号 001，且字段与建库脚本一致。
     */
    @Test
    void findByUserIdHitsSeedData() {
        User user = dao.findByUserId("001");

        assertNotNull(user, "应能查到演示账号 001");
        assertEquals("001", user.getHumanInfo().getId());
        assertEquals("演示学生", user.getHumanInfo().getName());
        assertTrue(user instanceof Student);
    }

    /**
     * 查询不存在的登录ID应返回 null。
     */
    @Test
    void findByUserIdReturnsNullWhenAbsent() {
        assertNull(dao.findByUserId("nobody"), "不存在的账号应返回 null");
    }

    /**
     * 查询全部用户应包含建库脚本写入的三个演示账号。
     */
    @Test
    void findAllContainsSeedData() {
        List<User> users = dao.findAll();

        assertNotNull(users, "应返回列表而非 null");
        assertTrue(users.size() >= 3, "应至少包含三个演示账号，实际 " + users.size());
    }

    /**
     * 新增用户后应能按登录ID查回，且字段与写入值一致。
     *
     * @throws SQLException 清理失败时抛出
     */
    @Test
    void addUserThenFindItBack() throws SQLException {
        deleteTempUser();
        try {
                User newUser = new Student(new HumanInfo(TEMP_ID, "临时用户", null, null, null,
                    19, HumanInfo.Gender.FEMALE), TEMP_ID, "pwd");

            assertTrue(dao.addUser(newUser), "新增用户应返回 true");

            User saved = dao.findByUserId(TEMP_ID);
            assertNotNull(saved, "新增后应能查回");
            assertEquals("临时用户", saved.getHumanInfo().getName());
            assertEquals(19, saved.getHumanInfo().getAge());
            assertEquals(HumanInfo.Gender.FEMALE, saved.getHumanInfo().getGender());
            assertTrue(saved instanceof Student);
        } finally {
            deleteTempUser();
        }
    }

    /**
     * 清理后临时用户应确实不存在，确认用例之间互不残留。
     *
     * @throws SQLException 清理失败时抛出
     */
    @Test
    void tempUserIsCleanedUp() throws SQLException {
        deleteTempUser();

        assertNull(dao.findByUserId(TEMP_ID), "清理后不应残留临时用户");
        assertFalse(DbHelper.isTableEmpty("tblUser"), "演示数据不应为空");
    }
}
