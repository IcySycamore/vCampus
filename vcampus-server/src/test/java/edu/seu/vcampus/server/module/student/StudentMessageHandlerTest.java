package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.entity.EnrollmentStatus;
import edu.seu.vcampus.common.entity.StudentProfile;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.auth.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 学籍消息处理器测试：命令码分支、权限校验、成功/失败状态码。
 */
class StudentMessageHandlerTest {

    /** 被测处理器。 */
    private StudentMessageHandler handler;

    /** 底层服务（内存 DAO）。 */
    private StudentService service;

    /** 会话管理器（签发测试 token）。 */
    private SessionManager sessions;

    /** 管理员 token。 */
    private String adminToken;

    /** 学生 token。 */
    private String studentToken;

    /** 教师 token。 */
    private String teacherToken;

    /**
     * 每个测试前重建处理器并预置三种角色的 token。
     */
    @BeforeEach
    void setUp() {
        service = new StudentService(new StudentDaoMemory());
        sessions = new SessionManager();
        handler = new StudentMessageHandler(service, sessions);
        adminToken = sessions.create("admin", "管理员");
        studentToken = sessions.create("stu001", "学生");
        teacherToken = sessions.create("tea001", "教师");
    }

    /**
     * 查询命令（201）应回 SUCCESS 并携带学籍记录。
     */
    @Test
    void queryReturnsProfile() {
        StudentProfile profile = new StudentProfile(1001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_QUERY, profile.getId()),
                adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        StudentProfile data = (StudentProfile) response.getData();
        assertNotNull(data);
        assertEquals(profile.getUserId(), data.getUserId());
    }

    /**
     * 查询不存在的记录应回 404。
     */
    @Test
    void queryMissingReturnsNotFound() {
        Message response = send(new Message(Command.STUDENT_QUERY, 9999L), adminToken);

        assertEquals(StatusCode.NOT_FOUND, response.getStatusCode());
    }

    /**
     * 登记命令（204）应回 SUCCESS（管理员）。
     */
    @Test
    void registerReturnsSuccess() {
        StudentProfile profile = new StudentProfile(2001L, 2026,
                EnrollmentStatus.ENROLLED);

        Message response = send(new Message(Command.STUDENT_REGISTER, profile), adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 更新命令（202）应回 SUCCESS（管理员）。
     */
    @Test
    void updateReturnsSuccess() {
        StudentProfile profile = new StudentProfile(3001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);
        profile.setStatus(EnrollmentStatus.SUSPENDED);

        Message response = send(new Message(Command.STUDENT_MODIFY_APPLY, profile), adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(EnrollmentStatus.SUSPENDED,
                service.queryProfile(profile.getId()).getStatus());
    }

    /**
     * 删除命令（205）应回 SUCCESS（管理员），且记录被软删除。
     */
    @Test
    void deleteReturnsSuccess() {
        StudentProfile profile = new StudentProfile(4001L, 2026,
                EnrollmentStatus.GRADUATED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_DELETE, profile.getId()), adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(null, service.queryProfile(profile.getId()));
    }

    /**
     * 审核命令（203）暂未实现，应回 400（管理员有权限，但功能未实现）。
     */
    @Test
    void auditReturnsBadRequest() {
        Message response = send(new Message(Command.STUDENT_MODIFY_AUDIT, null), adminToken);

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 查询命令 data 类型错误应回 400。
     */
    @Test
    void queryWithWrongTypeReturnsBadRequest() {
        Message response = send(new Message(Command.STUDENT_QUERY, "not-a-long"), adminToken);

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 未知学籍命令应回 400。
     */
    @Test
    void unknownCommandReturnsBadRequest() {
        Message response = send(new Message(299, null), adminToken);

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 未携带 token 应回 401。
     */
    @Test
    void missingTokenReturnsUnauthorized() {
        Message response = send(new Message(Command.STUDENT_QUERY, 1001L), null);

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * 学生尝试删除学籍应回 403。
     */
    @Test
    void studentCannotDelete() {
        StudentProfile profile = new StudentProfile(5001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_DELETE, profile.getId()), studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 学生尝试登记学籍应回 403。
     */
    @Test
    void studentCannotRegister() {
        StudentProfile profile = new StudentProfile(6001L, 2026,
                EnrollmentStatus.ENROLLED);

        Message response = send(new Message(Command.STUDENT_REGISTER, profile), studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 教师尝试删除学籍应回 403。
     */
    @Test
    void teacherCannotDelete() {
        StudentProfile profile = new StudentProfile(7001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_DELETE, profile.getId()), teacherToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 学生查询应回 SUCCESS（查询对所有角色开放）。
     */
    @Test
    void studentCanQuery() {
        StudentProfile profile = new StudentProfile(8001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_QUERY, profile.getId()), studentToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 发送一条请求并捕获响应。
     *
     * @param request 请求
     * @param token   会话令牌（可为 null）
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
