package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyAuditRequest;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 学籍命令的越权测试：按 {@code Permissions} 矩阵逐条验证「哪种角色不该能做什么」。
 *
 * <p>
 * 越权用例期望 403 而不是 400/404——403 才说明是「鉴权拦住的」，若是 400/404 就说明请求
 * 已经进到业务层，权限形同虚设。未携带 token 则必须是 401，不能和 403 混为一谈。
 *
 * <p>
 * 例外是 207：学生<b>应该</b>能查申请单（需求就是「学生看自己的申请进展」），所以那里的断言
 * 落在「放行 + 范围收窄」上——放开准入的同时必须把范围钉死，否则放开就等于把全员的申请单
 * 摊给任何登录用户。见 {@link #studentModifyListIsNarrowedToOwnRequests()}。
 */
class StudentPermissionHandlerTest {

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
     * 每个测试前重建处理器并签发 token。
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
     * 学生尝试删除学籍应回 403。
     */
    @Test
    void studentCannotDelete() {
        StudentProfile profile = new StudentProfile("uuid-5001", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_DELETE, profile.getId()),
                studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 学生尝试登记学籍应回 403。
     */
    @Test
    void studentCannotRegister() {
        StudentProfile profile = new StudentProfile("uuid-6001", 2026,
                CampusStatus.ENROLLED);

        Message response = send(new Message(Command.STUDENT_REGISTER, profile), studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 学生尝试改学籍状态应回 403。
     */
    @Test
    void studentCannotChangeStatus() {
        StudentProfile change = new StudentProfile();
        change.setId(1L);
        change.setStatus(CampusStatus.SUSPENDED);

        Message response = send(new Message(Command.STUDENT_CHANGE_STATUS, change),
                studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 教师也没有注销学籍的权力（只有管理员可以）。
     */
    @Test
    void teacherCannotDelete() {
        StudentProfile profile = new StudentProfile("uuid-7001", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_DELETE, profile.getId()),
                teacherToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 教师不能改学籍状态（206）：教师对学籍只读。
     *
     * <p>
     * 这条能力（{@code STUDENT_CHANGE_STATUS}）原先是发给教师的，现在收回给管理员。
     * 之所以要在服务端测而不只靠界面隐按钮：客户端判定只管「显示与否」，绕过去直接发命令
     * 仍然必须被挡住，否则「隐藏按钮」就成了一种冒充安全的做法。
     */
    @Test
    void teacherCannotChangeStatus() {
        StudentProfile profile = new StudentProfile("uuid-7002", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(profile);
        StudentProfile change = new StudentProfile();
        change.setId(profile.getId());
        change.setStatus(CampusStatus.SUSPENDED);

        Message response = send(new Message(Command.STUDENT_CHANGE_STATUS, change),
                teacherToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
        assertEquals(CampusStatus.ENROLLED, service.queryProfile(profile.getId()).getStatus(),
                "被拒的请求不应改动学籍");
    }

    /**
     * 教师不能审核修改申请（203）：审核权是管理员的。
     */
    @Test
    void teacherCannotAudit() {
        Message response = send(new Message(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(Long.valueOf(1L), Boolean.TRUE, "越权尝试")), teacherToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 教师仍然能查学籍列表（208）：只读不等于看不见，查得到正是教师这项职责的全部。
     */
    @Test
    void teacherCanStillListStudents() {
        Message response = send(new Message(Command.STUDENT_LIST, new StudentQuery()),
                teacherToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 学生不能拉全量学籍列表（208 要求 STUDENT_VIEW_ALL）。
     */
    @Test
    void studentCannotListAllStudents() {
        Message response = send(new Message(Command.STUDENT_LIST, new StudentQuery()),
                studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 学生能查申请单，但只看得到自己提的那些。
     *
     * <p>
     * 207 早先要求 {@code STUDENT_MODIFY_AUDIT}，学生一律 403，界面上的表现就是「看不到自己的
     * 申请进展」。现在准入放开为「登录即可」，范围由服务端按会话 uuid 钉死——这里断言的就是这个范围：
     * 放行之后拿到的必须是自己的那一条，而不是全员的。
     */
    @Test
    @SuppressWarnings("unchecked")
    void studentModifyListIsNarrowedToOwnRequests() {
        StudentProfile mine = new StudentProfile("uuid-stu", 2026, CampusStatus.ENROLLED);
        service.registerStudent(mine);
        StudentProfile other = new StudentProfile("uuid-other", 2026, CampusStatus.ENROLLED);
        service.registerStudent(other);
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("joinYear", "2021");
        service.applyModification(mine.getId(), "uuid-stu", changes, "我的申请");
        service.applyModification(other.getId(), "uuid-other", changes, "别人的申请");

        Message response = send(new Message(Command.STUDENT_MODIFY_LIST, null), studentToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        PageResponse<StudentModifyRequest> page =
                (PageResponse<StudentModifyRequest>) response.getData();
        assertEquals(1L, page.getTotal());
        assertEquals("uuid-stu", page.getItems().get(0).getApplicantUuid());
    }

    /**
     * 未携带 token 应回 401（而非 403，两者语义不同）。
     */
    @Test
    void missingTokenReturnsUnauthorized() {
        Message response = send(new Message(Command.STUDENT_QUERY, 1001L), null);

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * 伪造的 token 同样回 401。
     */
    @Test
    void forgedTokenReturnsUnauthorized() {
        Message response = send(new Message(Command.STUDENT_QUERY, 1001L), "not-a-real-token");

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * 发送一条请求并捕获同步响应。
     *
     * @param request 请求
     * @param token 会话令牌（可为 null）
     * @return 处理器写出的响应
     */
    private Message send(Message request, String token) {
        if (token != null) {
            request.setToken(token);
        }
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
