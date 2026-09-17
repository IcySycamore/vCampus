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

/**
 * 审核流的命令层闭环与两条视角。
 *
 * <p>
 * 与 {@link StudentModifyFlowTest} 的分工：那边测业务层（申请只落单不改学籍、通过才生效），这边测
 * 命令层——207 学生只看得到自己提的、203 的审批权限、以及审批通过后 201 查回来的学籍是否已落实。
 * 这两层都得出问题才能算「功能没做」：业务层全绿而学生在 207 被 403 挡住时，界面上就只是一个
 * 「看不到申请进展」的黑盒。
 */
class StudentModifyLoopTest {

    /** 被测处理器。 */
    private StudentMessageHandler handler;

    /** 底层服务（内存 DAO）。 */
    private StudentService service;

    /** 会话管理器（签发测试 token）。 */
    private SessionManager sessions;

    /** 学生 token（uuid-stu）。 */
    private String studentToken;

    /** 管理员 token（uuid-admin，审核权持有者）。 */
    private String adminToken;

    /**
     * 每个测试前重建处理器与存储，并给学生会话预置一条在校档案。
     */
    @BeforeEach
    void setUp() {
        service = new StudentService(new StudentDaoMemory());
        sessions = new SessionManager();
        handler = new StudentMessageHandler(service, sessions);
        studentToken = sessions.create("uuid-stu", "stu001", "学生");
        adminToken = sessions.create("uuid-admin", "admin001", "管理员");
        service.registerStudent(new StudentProfile("uuid-stu", 2026, CampusStatus.ENROLLED));
    }

    /**
     * 五条需求的整体闭环：学生提申请 → 学生看到待审 → 教务通过 → 学生看到已通过 → 学籍真的变了。
     *
     * <p>
     * 这条测试刻意走完整条链路（202 → 207 → 203 → 207 → 201），因为「同意之后要在学生账户里落实」
     * 只有在最后一步从 201 查回来才算真的证明。
     */
    @Test
    void applyAuditAndProfileUpdateFormOneLoop() {
        assertEquals(StatusCode.SUCCESS, applyAsStudent().getStatusCode());
        assertEquals(ModifyRequestStatus.PENDING, firstRequestAsStudent().getStatus());

        Message audited = send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(firstRequestAsStudent().getRequestId(), Boolean.TRUE,
                        "情况属实")), adminToken);
        assertEquals(StatusCode.SUCCESS, audited.getStatusCode());
        assertEquals(ModifyRequestStatus.APPROVED, firstRequestAsStudent().getStatus());

        Message mine = send(new Message(Command.STUDENT_QUERY, new StudentQuery()), studentToken);
        StudentProfile updated = (StudentProfile) mine.getData();
        assertEquals(2024, updated.getJoinYear());
        assertEquals("软件工程", updated.getField());
    }

    /**
     * 学生只看得到自己提的申请；请求体里伪造「申请人」也筛不出别人的。
     *
     * <p>
     * 收窄必须是<b>覆盖</b>而不是「客户端没传就补上」——后者只要客户端传一个别人的 uuid 就绕过去了。
     */
    @Test
    @SuppressWarnings("unchecked")
    void studentSeesOnlyOwnRequestsEvenWithForgedApplicant() {
        StudentProfile other = new StudentProfile("uuid-other", 2026, CampusStatus.ENROLLED);
        service.registerStudent(other);
        applyAsStudent();
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("field", "土木工程");
        service.applyModification(other.getId(), "uuid-other", changes, "别人的申请");
        Map<String, String> second = new LinkedHashMap<String, String>();
        second.put("joinYear", "2019");
        service.applyModification(other.getId(), "uuid-other", second, "别人的第二条");

        Message mine = send(listQuery(new ModifyRequestQuery()), studentToken);
        Message all = send(listQuery(new ModifyRequestQuery()), adminToken);
        ModifyRequestQuery forged = new ModifyRequestQuery();
        forged.setApplicantUuid("uuid-other");
        Message cheated = send(listQuery(forged), studentToken);

        assertEquals(StatusCode.SUCCESS, mine.getStatusCode());
        PageResponse<StudentModifyRequest> minePage =
                (PageResponse<StudentModifyRequest>) mine.getData();
        assertEquals(1L, minePage.getTotal());
        assertEquals("uuid-stu", minePage.getItems().get(0).getApplicantUuid());
        // 有审核权的是另一条视角：三条都在，说明收窄只发生在无审核权那条路上
        assertEquals(3L, ((PageResponse<StudentModifyRequest>) all.getData()).getTotal());
        // 伪造申请人必须无效：别人的有两条，若筛选被采信这里就会是 2 而不是 1
        PageResponse<StudentModifyRequest> cheatedPage =
                (PageResponse<StudentModifyRequest>) cheated.getData();
        assertEquals(1L, cheatedPage.getTotal());
        assertEquals("uuid-stu", cheatedPage.getItems().get(0).getApplicantUuid());
    }

    /**
     * 没有审批权的人只能给自己的学籍提申请：指向他人直接 403，且不留申请单。
     */
    @Test
    void studentCannotApplyForOthersProfile() {
        StudentProfile other = new StudentProfile("uuid-other", 2026, CampusStatus.ENROLLED);
        service.registerStudent(other);
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("joinYear", "2020");
        Message response = send(new Message(Command.STUDENT_MODIFY_APPLY,
                new edu.seu.vcampus.common.student.dto.StudentModifyRequest(other.getId(),
                        changes, "替别人提申请")), studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
        assertEquals(0L, service.listModifyRequests(null).getTotal());
    }

    /**
     * 学生提一条合法申请（改入学年份与专业）。
     *
     * @return 响应
     */
    private Message applyAsStudent() {
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("joinYear", "2024");
        changes.put("field", "软件工程");
        return send(new Message(Command.STUDENT_MODIFY_APPLY,
                new edu.seu.vcampus.common.student.dto.StudentModifyRequest(
                        service.queryByUserUuid("uuid-stu").getId(), changes, "入学年份录错")),
                studentToken);
    }

    /**
     * 以学生视角取自己最新的一条申请。
     *
     * @return 申请单
     */
    @SuppressWarnings("unchecked")
    private StudentModifyRequest firstRequestAsStudent() {
        Message response = send(listQuery(new ModifyRequestQuery()), studentToken);
        return ((PageResponse<StudentModifyRequest>) response.getData()).getItems().get(0);
    }

    /**
     * 包一条 207 请求。
     *
     * @param query 过滤条件
     * @return 请求消息
     */
    private static Message listQuery(ModifyRequestQuery query) {
        return new Message(Command.STUDENT_MODIFY_LIST, query);
    }

    /**
     * 把请求交给被测处理器并取回响应。
     *
     * @param request 请求
     * @param token 会话令牌
     * @return 响应
     */
    private Message send(Message request, String token) {
        request.setToken(token);
        final Message[] sent = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };
        handler.handle(request, sender);
        return sent[0];
    }
}
