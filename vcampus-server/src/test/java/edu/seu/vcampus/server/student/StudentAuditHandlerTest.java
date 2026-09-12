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
    }

    /**
     * 验收主链路：学生申请 → 教务待审列表可见 → 通过 → 学籍真的变了。
     */
    @Test
    void studentApplyThenTeacherApproveChangesProfile() {
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

        ModifyRequestQuery pending = new ModifyRequestQuery();
        pending.setStatus(ModifyRequestStatus.PENDING);
        Message listResponse = send(new Message(Command.STUDENT_MODIFY_LIST, pending),
                teacherToken);
        assertEquals(StatusCode.SUCCESS, listResponse.getStatusCode());
        @SuppressWarnings("unchecked")
        PageResponse<StudentModifyRequest> page =
                (PageResponse<StudentModifyRequest>) listResponse.getData();
        assertEquals(1L, page.getTotal());
        Long requestId = page.getItems().get(0).getRequestId();
        assertNotNull(requestId);

        Message auditResponse = send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(requestId, true, "同意休学")), teacherToken);
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
                new ModifyAuditRequest(1L, true, "通过")), teacherToken).getStatusCode());
        assertEquals(StatusCode.BAD_REQUEST, send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(1L, true, "再通过")), teacherToken).getStatusCode());
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
