package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 学籍命令的越权测试：按 {@code Permissions} 矩阵逐条验证「哪种角色不该能做什么」。
 *
 * <p>
 * 每条用例都期望 403 而不是 400/404——403 才说明是「鉴权拦住的」，若是 400/404 就说明请求
 * 已经进到业务层，权限形同虚设。未携带 token 则必须是 401，不能和 403 混为一谈。
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
     * 学生不能拉全量学籍列表（208 要求 STUDENT_VIEW_ALL）。
     */
    @Test
    void studentCannotListAllStudents() {
        Message response = send(new Message(Command.STUDENT_LIST, new StudentQuery()),
                studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 学生不能查询待审申请列表（207 要求 STUDENT_MODIFY_AUDIT）。
     */
    @Test
    void studentCannotListModifyRequests() {
        Message response = send(new Message(Command.STUDENT_MODIFY_LIST, null), studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
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
