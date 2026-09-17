package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据库版学籍存储测试：逐条覆盖 {@code docs/学籍模块数据库对接说明.md} §5 要求的语义。
 *
 * <p>
 * 需要可用的 MySQL 且已执行 {@code sql/vCampus.sql}；环境不满足时整组跳过（见
 * {@link StudentJdbcTestSupport}）。
 */
class StudentDaoJdbcTest {

    /** 连接来源；null 表示本机不可用（用例跳过）。 */
    private static StudentDataSource source;

    /** 被测存储。 */
    private StudentDao dao;

    /**
     * 判断数据库是否可用。
     */
    @BeforeAll
    static void probeDatabase() {
        source = StudentJdbcTestSupport.dataSourceOrNull();
    }

    /**
     * 每条用例前清表并新建存储。
     */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(source != null, "需要可用的 MySQL 与学籍表");
        StudentJdbcTestSupport.clear(source, StudentJdbcTestSupport.PROFILE_TABLE);
        dao = new StudentDaoJdbc(source);
    }

    /**
     * insert 必须把数据库分配的主键回填到对象上，否则后续改状态、提申请全都定位不到这条档案。
     */
    @Test
    void insertBackFillsGeneratedId() {
        StudentProfile profile = profile("uuid-1", "20260001");

        assertTrue(dao.insert(profile));

        assertNotNull(profile.getId(), "主键应由数据库分配并回填");
        assertTrue(profile.getId().longValue() > 0L);
    }

    /**
     * 按主键与按账户各能查到，且字段完整往返（学号、专业、类别、状态、年份）。
     */
    @Test
    void roundTripByBothKeys() {
        StudentProfile profile = profile("uuid-2", "20260002");
        profile.setField("软件工程");
        dao.insert(profile);

        StudentProfile byId = dao.findById(profile.getId());
        StudentProfile byUuid = dao.findByUserUuid("uuid-2");

        assertNotNull(byId);
        assertNotNull(byUuid);
        assertEquals("20260002", byId.getStudentNo());
        assertEquals("软件工程", byUuid.getField());
        assertEquals(2026, byUuid.getJoinYear());
        assertEquals(CampusStatus.ENROLLED, byUuid.getStatus());
        assertEquals(PersonCategory.STUDENT, byUuid.getPersonCategory());
        assertNull(byId.getRealName(), "姓名是联查展示字段，不该从本表读出来");
    }

    /**
     * 软删除后按主键、按账户、列表与计数都不再返回它——漏掉任何一处，注销过的档案都会复活。
     */
    @Test
    void softDeletedRowDisappearsEverywhere() {
        StudentProfile profile = profile("uuid-3", "20260003");
        dao.insert(profile);
        dao.insert(profile("uuid-4", "20260004"));

        assertTrue(dao.softDelete(profile.getId()));

        assertNull(dao.findById(profile.getId()));
        assertNull(dao.findByUserUuid("uuid-3"));
        assertEquals(1, dao.findAll().size());
        assertFalse(dao.softDelete(profile.getId()), "重复注销应返回 false");
    }

    /**
     * 更新按主键定位，且能改到每一个落库字段。
     */
    @Test
    void updateChangesPersistedFields() {
        StudentProfile profile = profile("uuid-5", "20260005");
        dao.insert(profile);
        profile.setField("网络空间安全");
        profile.setStatus(CampusStatus.SUSPENDED);
        profile.setStudentNo("20260099");

        assertTrue(dao.update(profile));

        StudentProfile reloaded = dao.findById(profile.getId());
        assertEquals("网络空间安全", reloaded.getField());
        assertEquals(CampusStatus.SUSPENDED, reloaded.getStatus());
        assertEquals("20260099", reloaded.getStudentNo());
    }

    /**
     * 主键为 null 的更新应返回 false（而不是让 SQL 去更新 id 为 null 的行）。
     */
    @Test
    void updateWithoutIdFails() {
        StudentProfile orphan = profile("uuid-6", null);

        assertFalse(dao.update(orphan));
        assertFalse(dao.softDelete(null));
        assertNull(dao.findById(Long.valueOf(9999L)));
        assertNull(dao.findByUserUuid("uuid-not-exist"));
    }

    /**
     * 空表的查询返回空列表而不是 null：调用方不必为「没有数据」再写一次判空。
     */
    @Test
    void emptyTableYieldsEmptyList() {
        List<StudentProfile> all = dao.findAll();

        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    /**
     * 造一条待落库的档案。
     *
     * @param uuid 账户 uuid
     * @param studentNo 学号；可为 null
     * @return 档案
     */
    private static StudentProfile profile(String uuid, String studentNo) {
        StudentProfile profile = new StudentProfile(uuid, 2026, CampusStatus.ENROLLED);
        profile.setStudentNo(studentNo);
        return profile;
    }
}
