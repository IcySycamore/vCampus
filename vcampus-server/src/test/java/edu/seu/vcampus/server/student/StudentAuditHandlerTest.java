package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyAuditRequest;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 审核流端到端（handler 层）：覆盖设计文档给出的验收标准。
 *
 * <p>
 * 关键的一条是 {@link #studentApplyThenTeacherApproveChangesProfile()}：学生提交申请 → 教务在
 * 待审列表看到 → 审核通过 → 学籍字段真的变了。这条链路跨了 202 / 207 / 203 三个命令，
 * 任何一环接错都会在这里暴露。
 */
class StudentAuditHandlerTest {

    /** 被测处理器。 */
    private StudentMessageHandler handler;

    /** 底层服务。 */
    private StudentService service;

    /** 会话管理器。 */
    private SessionManager sessions;

    /** 学生 token。 */
    private String studentToken;

    /** 教师 token。 */
    private String teacherToken;

    /** 管理员 token（审核权持有者）。 */
    private String adminToken;

    /**
     * 每个测试前重建处理器与 token。
     */
    @BeforeEach
    void setUp() {
        service = new StudentService(new StudentDaoMemory(),
                new StudentModifyRequestDaoMemory());
        sessions = new SessionManager();
        handler = new StudentMessageHandler(service, sessions);
        studentToken = sessions.create("uuid-stu", "stu001", "学生");
        teacherToken = sessions.create("uuid-tea", "tea001", "教师");
        adminToken = sessions.create("uuid-admin", "admin001", "管理员");
    }

    /**
     * 验收主链路：学生申请 → 管理员待审列表可见 → 通过 → 学籍真的变了。
     */
    @Test
    void studentApplyThenAdminApproveChangesProfile() {
        StudentProfile profile = new StudentProfile("uuid-stu", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(profile);

        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("joinYear", "2025");
        changes.put("status", "SUSPENDED");
        edu.seu.vcampus.common.student.dto.StudentModifyRequest dto =
                new edu.seu.vcampus.common.student.dto.StudentModifyRequest(
                        profile.getId(), changes, "休学一年");
        assertEquals(StatusCode.SUCCESS,
                send(new Message(Command.STUDENT_MODIFY_APPLY, dto), studentToken).getStatusCode());

        // 教师看不到这条队列：207 对「没有审核权的人」按会话 uuid 收窄成「自己提的申请」，
        // 而教师本来就不提学籍申请，于是拿到 0 条。这与「教师对学籍只读」是一件事的两面：
        // 看不见队列，也就不会出现「看得见却批不了」这种半吊子状态。
        Message teacherList = send(new Message(Command.STUDENT_MODIFY_LIST, pendingQuery()),
                teacherToken);
        assertEquals(StatusCode.SUCCESS, teacherList.getStatusCode());
        assertEquals(0L, ((PageResponse<?>) teacherList.getData()).getTotal(),
                "教师的待审列表应被收窄成自己的申请（他没提过，所以是 0）");

        // 管理员才是有审核权的那条视角
        Message listResponse = send(new Message(Command.STUDENT_MODIFY_LIST, pendingQuery()),
                adminToken);
        assertEquals(StatusCode.SUCCESS, listResponse.getStatusCode());
        @SuppressWarnings("unchecked")
        PageResponse<StudentModifyRequest> page =
                (PageResponse<StudentModifyRequest>) listResponse.getData();
        assertEquals(1L, page.getTotal());
        Long requestId = page.getItems().get(0).getRequestId();
        assertNotNull(requestId);

        // 教师批不了：教师对学籍只读
        assertEquals(StatusCode.FORBIDDEN, send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(requestId, true, "越权尝试")), teacherToken)
                .getStatusCode());

        Message auditResponse = send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(requestId, true, "同意休学")), adminToken);
        assertEquals(StatusCode.SUCCESS, auditResponse.getStatusCode());

        StudentProfile updated = service.queryProfile(profile.getId());
        assertEquals(2025, updated.getJoinYear());
        assertEquals(CampusStatus.SUSPENDED, updated.getStatus());
    }

    /**
     * 学生无审核权，203 应回 403。
     */
    @Test
    void studentCannotAudit() {
        Message response = send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(1L, true, "我说了算")), studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 学生无全量查看权，208 应回 403。
     */
    @Test
    void studentCannotListAllStudents() {
        Message response = send(new Message(Command.STUDENT_LIST, new StudentQuery()),
                studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 教师可查学籍列表（208），关键字过滤生效。
     */
    @Test
    void teacherCanListStudentsWithKeyword() {
        service.registerStudent(new StudentProfile("uuid-a", 2026, CampusStatus.ENROLLED));
        service.registerStudent(new StudentProfile("uuid-b", 2025, CampusStatus.ENROLLED));
        StudentQuery query = new StudentQuery();
        query.setKeyword("uuid-a");

        Message response = send(new Message(Command.STUDENT_LIST, query), teacherToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        @SuppressWarnings("unchecked")
        PageResponse<StudentProfile> page = (PageResponse<StudentProfile>) response.getData();
        assertEquals(1L, page.getTotal());
        assertEquals("uuid-a", page.getItems().get(0).getUserUuid());
    }

    /**
     * 同一条申请重复审核应回 400（不能反复应用变更）。
     */
    @Test
    void duplicateAuditReturnsBadRequest() {
        StudentProfile profile = new StudentProfile("uuid-stu", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(profile);
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("status", "SUSPENDED");
        service.applyModification(profile.getId(), "uuid-stu", changes, "休学");

        assertEquals(StatusCode.SUCCESS, send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(1L, true, "通过")), adminToken).getStatusCode());
        assertEquals(StatusCode.BAD_REQUEST, send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(1L, true, "再通过")), adminToken).getStatusCode());
    }

    /**
     * 学生查询自己的学籍（201 不带参数）应回 SUCCESS。
     */
    @Test
    void studentQueryWithoutPayloadReturnsOwnProfile() {
        service.registerStudent(new StudentProfile("uuid-stu", 2026,
                CampusStatus.ENROLLED));

        Message response = send(new Message(Command.STUDENT_QUERY, null), studentToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 造一份「只筛待审」的查询条件。
     *
     * <p>
     * 每次调用都返回新对象，<b>不能复用同一份</b>：207 在处理「没有审核权的人」时会把请求里的
     * 申请人<b>就地覆盖</b>成会话 uuid（这正是防伪造申请人的机制）。同一个对象先给教师用、再给
     * 管理员用，第二次就带着教师被改写过的条件，管理员也会只看到 0 条——一条假失败。
     *
     * @return 新的查询条件
     */
    private static ModifyRequestQuery pendingQuery() {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setStatus(ModifyRequestStatus.PENDING);
        return query;
    }

    /**
     * 发送一条请求并捕获同步响应。
     *
     * @param request 请求
     * @param token 会话令牌
     * @return 处理器写出的响应
     */
    private Message send(Message request, String token) {
        request.setToken(token);
        final Message[] captured = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                captured[0] = response;
            }
        };
        handler.handle(request, sender);
        return captured[0];
    }
}
