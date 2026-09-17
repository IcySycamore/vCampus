package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StudentModifyRequestDaoJdbc} 的真库集成测试。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过。前置是库中已有 {@code sql/vCampus-extend.sql} 建出的
 * {@code tblStudentModifyRequest}。
 *
 * <p>
 * 申请表有指向 {@code tblUser} 的外键，所以每个用例先插一行临时用户，跑完按外键顺序 （申请单 → 用户）物理删除。{@code uUuid} 是
 * {@code CHAR(36)}、{@code uId} 是 {@code VARCHAR(8)}，标识都压在列宽内。
 */
class StudentModifyRequestDaoJdbcTest {

    /** 测试 uuid 前缀。 */
    private static final String UUID_PREFIX = "jdbc-smr-it-";

    /** 本轮测试申请人 uuid。 */
    private String m_applicantUuid;

    /** 被测 DAO。 */
    private StudentModifyRequestDaoJdbc m_dao;

    /** 每个用例前确认数据库可用、建临时用户并生成唯一标识。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过 JDBC 集成测试（docker compose up -d mysql 后自动执行）");
        long stamp = System.nanoTime();
        m_applicantUuid = uuid(stamp);
        m_dao = new StudentModifyRequestDaoJdbc();
        insertUser(m_applicantUuid, "S" + Long.toHexString(stamp).substring(0, 6));
    }

    /** 用例后按外键顺序物理删除测试数据。 */
    @AfterEach
    void tearDown() {
        executeUpdate("DELETE FROM tblStudentModifyRequest WHERE uUuid = ?", m_applicantUuid);
        executeUpdate("DELETE FROM tblUser WHERE uUuid = ?", m_applicantUuid);
    }

    @Test
    void insertBackfillsRequestIdAndReadsBackAllFields() {
        StudentModifyRequest request = request(1L, "joinYear=2028", "家庭原因");
        assertTrue(m_dao.insert(request), "新增申请单应成功");
        assertNotNull(request.getRequestId(), "插入后应回填 Long 单号");

        StudentModifyRequest loaded = m_dao.findById(request.getRequestId());
        assertNotNull(loaded, "按单号应能查到");
        assertEquals(m_applicantUuid, loaded.getApplicantUuid());
        assertEquals(Long.valueOf(1L), loaded.getProfileId());
        assertEquals("joinYear=2028", loaded.getChangesJson());
        assertEquals("家庭原因", loaded.getReason());
        assertEquals(ModifyRequestStatus.PENDING, loaded.getStatus(), "新单默认待审核");
        assertNull(loaded.getComment(), "未审核时没有意见");
        assertTrue(loaded.getAppliedAt() > 0L, "提交时间应落库");
        assertEquals(0L, loaded.getAuditedAt(), "未审核时审核时间为 0");
    }

    @Test
    void updateWritesAuditResult() {
        StudentModifyRequest request = request(2L, "status=GRADUATED", "提前毕业");
        m_dao.insert(request);

        request.setStatus(ModifyRequestStatus.REJECTED);
        request.setComment("材料不全");
        request.setAuditedBy(uuid(System.nanoTime()));
        request.setAuditedAt(System.currentTimeMillis());
        assertTrue(m_dao.update(request), "审核结果应写回");

        StudentModifyRequest reloaded = m_dao.findById(request.getRequestId());
        assertEquals(ModifyRequestStatus.REJECTED, reloaded.getStatus());
        assertEquals("材料不全", reloaded.getComment());
        assertNotNull(reloaded.getAuditedBy(), "审核人应落库");
        assertTrue(reloaded.getAuditedAt() > 0L, "审核时间应落库");
    }

    @Test
    void findAndCountShareTheSameConditions() {
        m_dao.insert(request(3L, "field=人工智能", "转专业"));
        StudentModifyRequest other = request(4L, "field=软件工程", "转专业");
        m_dao.insert(other);
        other.setStatus(ModifyRequestStatus.APPROVED);
        m_dao.update(other);

        ModifyRequestQuery pending = new ModifyRequestQuery(ModifyRequestStatus.PENDING);
        pending.setApplicantUuid(m_applicantUuid);
        List<StudentModifyRequest> page = m_dao.find(pending, 0, 10);
        assertEquals(1, page.size(), "只应剩下待审核的那条");
        assertEquals(Long.valueOf(3L), page.get(0).getProfileId());
        assertEquals(m_dao.count(pending), (long) page.size(), "总数应与查询条件一致");

        ModifyRequestQuery all = new ModifyRequestQuery();
        all.setApplicantUuid(m_applicantUuid);
        assertEquals(2L, m_dao.count(all), "不带状态时两条都算");
    }

    @Test
    void keywordSearchMatchesChangesReasonAndApplicant() {
        m_dao.insert(request(5L, "field=人工智能", "转专业申请"));

        ModifyRequestQuery byReason = new ModifyRequestQuery();
        byReason.setApplicantUuid(m_applicantUuid);
        byReason.setKeyword("转专业");
        byReason.setSearchField(RequestField.ALL);
        assertEquals(1L, m_dao.count(byReason), "关键词应能命中理由");

        ModifyRequestQuery byChanges = new ModifyRequestQuery();
        byChanges.setApplicantUuid(m_applicantUuid);
        byChanges.setKeyword("人工智能");
        byChanges.setSearchField(RequestField.CHANGES);
        assertEquals(1L, m_dao.count(byChanges), "限定列后也能命中变更内容");

        ModifyRequestQuery miss = new ModifyRequestQuery();
        miss.setApplicantUuid(m_applicantUuid);
        miss.setKeyword("不存在的词");
        miss.setSearchField(RequestField.ALL);
        assertEquals(0L, m_dao.count(miss));
    }

    @Test
    void sortableFieldsAndPagingAreApplied() {
        for (int i = 0; i < 3; i++) {
            StudentModifyRequest item = request(Long.valueOf(10L + i), "joinYear=202" + i, "批量");
            item.setAppliedAt(System.currentTimeMillis() + i * 1000L);
            m_dao.insert(item);
        }

        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setApplicantUuid(m_applicantUuid);
        query.setSortBy(RequestField.PROFILE_ID);
        query.setDescending(true);
        List<StudentModifyRequest> sorted = m_dao.find(query, 0, 2);
        assertEquals(2, sorted.size(), "每页两条");
        assertEquals(Long.valueOf(12L), sorted.get(0).getProfileId(), "应按学籍主键倒序");

        List<StudentModifyRequest> second = m_dao.find(query, 2, 2);
        assertEquals(1, second.size(), "第二页只剩一条");
    }

    /**
     * 造一条申请单。
     *
     * @param profileId 目标学籍序号
     * @param changes   变更内容
     * @param reason    申请理由
     * @return 申请单实体
     */
    private StudentModifyRequest request(Long profileId, String changes, String reason) {
        StudentModifyRequest request = new StudentModifyRequest(profileId, m_applicantUuid,
                changes, reason);
        request.setAppliedAt(System.currentTimeMillis());
        return request;
    }

    /**
     * 生成恰好 36 位的测试 uuid（{@code uUuid} 是 {@code CHAR(36)}）。
     *
     * @param stamp 时间戳
     * @return 补足 36 位的 uuid
     */
    private static String uuid(long stamp) {
        StringBuilder builder = new StringBuilder(UUID_PREFIX);
        builder.append(Long.toHexString(stamp));
        while (builder.length() < 36) {
            builder.append('0');
        }
        return builder.substring(0, 36);
    }

    /**
     * 插入一行临时用户，满足申请表的外键。
     *
     * @param applicantUuid 用户 uuid
     * @param loginId       登录 ID，最多 8 字符
     */
    private static void insertUser(String applicantUuid, String loginId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("INSERT INTO tblUser (uUuid, uId, uName,"
                    + " uPwd, uRole) VALUES (?, ?, ?, ?, ?)");
            statement.setString(1, applicantUuid);
            statement.setString(2, loginId);
            statement.setString(3, "集成测试");
            statement.setString(4, "x");
            statement.setString(5, "学生");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("插入测试用户失败", e);
        } finally {
            close(null, statement, connection);
        }
    }

    /**
     * 执行一条写语句（用于清理测试数据）。
     *
     * @param sql   语句
     * @param param 唯一参数
     */
    private static void executeUpdate(String sql, String param) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, param);
            statement.executeUpdate();
        } catch (SQLException e) {
            // 清理失败不影响用例结论
        } catch (RuntimeException e) {
            // 数据库不可用时 setUp 已跳过，这里同样忽略
        } finally {
            close(null, statement, connection);
        }
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
            return connection != null;
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            close(null, null, connection);
        }
    }

    /**
     * 安静关闭资源。
     *
     * @param rows       结果集；可为 null
     * @param statement  语句；可为 null
     * @param connection 连接；可为 null
     */
    private static void close(java.sql.ResultSet rows, PreparedStatement statement,
            Connection connection) {
        if (rows != null) {
            try {
                rows.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
    }
}
