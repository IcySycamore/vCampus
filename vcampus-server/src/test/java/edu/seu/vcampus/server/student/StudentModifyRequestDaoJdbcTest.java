package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据库版申请单存储测试。
 *
 * <p>
 * 除了逐条验证自身语义，还用 {@link #agreesWithMemoryImplementation()} 把两种实现放在<b>同一批
 * 数据、同一批查询</b>下对照：过滤与排序的语义写在 {@link ModifyRequestMatcher} /
 * {@link ModifyRequestSorter} 里，SQL 侧是把同一套语义翻了一遍，翻错了只有在两实现对比时才看得见。
 */
class StudentModifyRequestDaoJdbcTest {

    /** 连接来源；null 表示本机不可用（用例跳过）。 */
    private static StudentDataSource source;

    /** 被测存储。 */
    private StudentModifyRequestDao dao;

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
        Assumptions.assumeTrue(source != null, "需要可用的 MySQL 与申请单表");
        StudentJdbcTestSupport.clear(source, StudentJdbcTestSupport.REQUEST_TABLE);
        dao = new StudentModifyRequestDaoJdbc(source);
    }

    /**
     * insert 回填主键，且各字段完整往返。
     */
    @Test
    void insertBackFillsIdAndRoundTrips() {
        StudentModifyRequest request = request(42L, "uuid-stu", "status=SUSPENDED", "申请休学", 1000L);
        request.setStatus(ModifyRequestStatus.PENDING);

        assertTrue(dao.insert(request));
        assertNotNull(request.getRequestId());

        StudentModifyRequest back = dao.findById(request.getRequestId());
        assertEquals(Long.valueOf(42L), back.getProfileId());
        assertEquals("uuid-stu", back.getApplicantUuid());
        assertEquals("status=SUSPENDED", back.getChangesJson());
        assertEquals("申请休学", back.getReason());
        assertEquals(ModifyRequestStatus.PENDING, back.getStatus());
        assertEquals(1000L, back.getAppliedAt());
    }

    /**
     * 审核结果能回写：状态、意见、审核人、审核时间。
     */
    @Test
    void updateWritesAuditResult() {
        StudentModifyRequest request = request(42L, "uuid-stu", "field=软件工程", "改专业", 1000L);
        dao.insert(request);
        request.setStatus(ModifyRequestStatus.APPROVED);
        request.setComment("情况属实");
        request.setAuditedBy("uuid-admin");
        request.setAuditedAt(2000L);

        assertTrue(dao.update(request));

        StudentModifyRequest back = dao.findById(request.getRequestId());
        assertEquals(ModifyRequestStatus.APPROVED, back.getStatus());
        assertEquals("情况属实", back.getComment());
        assertEquals("uuid-admin", back.getAuditedBy());
        assertEquals(2000L, back.getAuditedAt());
    }

    /**
     * 默认按申请时间倒序：清待办时新提交的必须排在最上面。
     */
    @Test
    void defaultOrderIsAppliedAtDescending() {
        dao.insert(request(1L, "uuid-a", "a=b", "第一条", 1000L));
        dao.insert(request(1L, "uuid-a", "a=b", "第三条", 3000L));
        dao.insert(request(1L, "uuid-a", "a=b", "第二条", 2000L));

        List<StudentModifyRequest> items = dao.find(new ModifyRequestQuery(), 0, 10);

        assertEquals(3, items.size());
        assertEquals("第三条", items.get(0).getReason());
        assertEquals("第一条", items.get(2).getReason());
    }

    /**
     * 分页：offset 越界返回空列表，limit 非正返回空列表（与内存实现一致）。
     */
    @Test
    void pagingBoundaries() {
        dao.insert(request(1L, "uuid-a", "a=b", "唯一一条", 1000L));

        assertTrue(dao.find(new ModifyRequestQuery(), 0, 0).isEmpty());
        assertTrue(dao.find(new ModifyRequestQuery(), 5, 10).isEmpty());
    }

    /**
     * find 与 count 的条件必须一致，否则分页总条数与实际页数对不上。
     */
    @Test
    void findAndCountAgreeOnConditions() {
        dao.insert(pending(1L, "uuid-a", "申请休学"));
        dao.insert(pending(1L, "uuid-b", "申请休学"));
        StudentModifyRequest approved = pending(2L, "uuid-c", "改专业");
        approved.setStatus(ModifyRequestStatus.APPROVED);
        dao.insert(approved);

        ModifyRequestQuery pendingOnly = new ModifyRequestQuery();
        pendingOnly.setStatus(ModifyRequestStatus.PENDING);

        assertEquals(2L, dao.count(pendingOnly));
        assertEquals(2, dao.find(pendingOnly, 0, 10).size());
        assertEquals(3L, dao.count(new ModifyRequestQuery()));
    }

    /**
     * 关键词一次比对多条（单号 / 学籍 / 申请人 / 变更内容 / 理由），指定字段时只比那一列。
     */
    @Test
    void keywordMatchesAnyFieldOrOneField() {
        dao.insert(pending(7L, "uuid-stu", "申请休学"));
        dao.insert(pending(9L, "uuid-other", "改专业"));

        assertEquals(1L, dao.count(keyword("休学", null)));
        assertEquals(1L, dao.count(keyword("uuid-other", null)));
        assertEquals(1L, dao.count(keyword("7", null)));
        assertEquals(0L, dao.count(keyword("休学", RequestField.CHANGES)),
                "「休学」只在理由里，限定到变更内容列就不该命中");
        assertEquals(1L, dao.count(keyword("休学", RequestField.REASON)));
    }

    /**
     * 关键词里的 {@code %} 要按字面处理：不转义的话一个字就等于把整张表捞出来。
     */
    @Test
    void keywordWildcardsAreEscaped() {
        dao.insert(pending(7L, "uuid-stu", "申请休学"));
        dao.insert(pending(9L, "uuid-other", "改专业"));

        assertEquals(0L, dao.count(keyword("%", null)), "百分号应作为普通字符，不该匹配全部");
        assertEquals(0L, dao.count(keyword("_", null)));
    }

    /**
     * 与内存实现在同一批数据、同一批查询下结果一致——SQL 侧把过滤/排序语义翻错了，只有这里能发现。
     */
    @Test
    void agreesWithMemoryImplementation() {
        StudentModifyRequestDao memory = new StudentModifyRequestDaoMemory();
        List<StudentModifyRequest> seed = new ArrayList<StudentModifyRequest>();
        seed.add(pending(1L, "uuid-a", "申请休学"));
        seed.add(pending(2L, "uuid-b", "改专业"));
        StudentModifyRequest third = pending(3L, "uuid-a", "休学一年");
        third.setAppliedAt(5000L);
        seed.add(third);
        int index = 0;
        while (index < seed.size()) {
            memory.insert(seed.get(index));
            dao.insert(seed.get(index));
            index = index + 1;
        }

        List<ModifyRequestQuery> queries = new ArrayList<ModifyRequestQuery>();
        queries.add(new ModifyRequestQuery());
        queries.add(pendingOnly());
        queries.add(keyword("休学", null));
        queries.add(keyword("uuid-a", RequestField.APPLICANT_UUID));
        queries.add(sortBy(RequestField.PROFILE_ID, false));
        queries.add(sortBy(RequestField.REQUEST_ID, true));

        int queryIndex = 0;
        while (queryIndex < queries.size()) {
            ModifyRequestQuery query = queries.get(queryIndex);
            assertEquals(memory.count(query), dao.count(query),
                    "第 " + queryIndex + " 组条件的总条数应一致");
            assertEquals(ids(memory.find(query, 0, 10)), ids(dao.find(query, 0, 10)),
                    "第 " + queryIndex + " 组条件的结果顺序应一致");
            queryIndex = queryIndex + 1;
        }
    }

    /**
     * 取结果里的主键序列。
     *
     * @param requests 结果
     * @return 主键列表
     */
    private static List<Long> ids(List<StudentModifyRequest> requests) {
        List<Long> ids = new ArrayList<Long>();
        int index = 0;
        while (index < requests.size()) {
            ids.add(requests.get(index).getRequestId());
            index = index + 1;
        }
        return ids;
    }

    /**
     * 造一条待审申请。
     *
     * @param profileId 目标学籍主键
     * @param applicant 申请人
     * @param reason 理由
     * @return 申请单
     */
    private static StudentModifyRequest pending(long profileId, String applicant, String reason) {
        StudentModifyRequest request = request(profileId, applicant, "status=SUSPENDED", reason,
                1000L);
        request.setStatus(ModifyRequestStatus.PENDING);
        return request;
    }

    /**
     * 造一条申请单（状态未设）。
     *
     * @param profileId 目标学籍主键
     * @param applicant 申请人
     * @param changes 变更内容
     * @param reason 理由
     * @param appliedAt 提交时间
     * @return 申请单
     */
    private static StudentModifyRequest request(long profileId, String applicant, String changes,
            String reason, long appliedAt) {
        StudentModifyRequest request = new StudentModifyRequest(Long.valueOf(profileId), applicant,
                changes, reason);
        request.setStatus(ModifyRequestStatus.PENDING);
        request.setAppliedAt(appliedAt);
        return request;
    }

    /**
     * 造「只筛待审」的条件。
     *
     * @return 条件
     */
    private static ModifyRequestQuery pendingOnly() {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setStatus(ModifyRequestStatus.PENDING);
        return query;
    }

    /**
     * 造带关键词的条件。
     *
     * @param keyword 关键词
     * @param field 搜索字段；null 表示多列
     * @return 条件
     */
    private static ModifyRequestQuery keyword(String keyword, RequestField field) {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setKeyword(keyword);
        query.setSearchField(field);
        return query;
    }

    /**
     * 造带排序的条件。
     *
     * @param field 排序字段
     * @param descending 是否降序
     * @return 条件
     */
    private static ModifyRequestQuery sortBy(RequestField field, boolean descending) {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setSortBy(field);
        query.setDescending(descending);
        return query;
    }
}
