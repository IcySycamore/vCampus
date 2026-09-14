package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 学籍查询与状态变更的 handler 测试：201 的「我的轨 / 全量轨」分轨，以及 206 的改状态。
 *
 * <p>
 * 201 的特殊之处是它对所有角色开放（不需要额外能力），但查询范围按角色收窄：没有
 * {@code STUDENT_VIEW_ALL} 的人按主键查别人必须 403，不能静默返回他人数据。这一组用例
 * 就是这条规则的守卫。
 */
class StudentQueryHandlerTest {

    /** 被测处理器。 */
    private StudentMessageHandler handler;

    /** 底层服务。 */
    private StudentService service;

    /** 会话管理器。 */
    private SessionManager sessions;

    /** 管理员 token。 */
    private String adminToken;

    /** 学生 token（uuid-stu）。 */
    private String studentToken;

    /** 教师 token。 */
    private String teacherToken;

    /**
     * 每个测试前重建处理器并签发三种角色的 token。
     */
    @BeforeEach
    void setUp() {
        service = new StudentService(new StudentDaoMemory(),
                new StudentModifyRequestDaoMemory());
        sessions = new SessionManager();
        handler = new StudentMessageHandler(service, sessions);
        adminToken = sessions.create("uuid-admin", "admin", "管理员");
        studentToken = sessions.create("uuid-stu", "stu001", "学生");
        teacherToken = sessions.create("uuid-tea", "tea001", "教师");
    }

    /**
     * 管理员按主键查询应回 SUCCESS 并携带学籍记录。
     */
    @Test
    void queryReturnsProfile() {
        StudentProfile profile = new StudentProfile("uuid-1001", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_QUERY, profile.getId()),
                adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        StudentProfile data = (StudentProfile) response.getData();
        assertNotNull(data);
        assertEquals(profile.getUserUuid(), data.getUserUuid());
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
     * 学生不带参数查询时返回自己的学籍（学生要能看到自己的记录）。
     */
    @Test
    void studentCanQueryOwnProfile() {
        service.registerStudent(new StudentProfile("uuid-stu", 2026,
                CampusStatus.ENROLLED));

        Message response = send(new Message(Command.STUDENT_QUERY, null), studentToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 学生按主键查询别人的学籍应回 403（验收标准：学生查不到别人的学籍）。
     */
    @Test
    void studentCannotQueryOthersProfile() {
        StudentProfile others = new StudentProfile("uuid-other", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(others);

        Message response = send(new Message(Command.STUDENT_QUERY, others.getId()),
                studentToken);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
    }

    /**
     * 教师拥有 STUDENT_VIEW_ALL，可查看任意学生。
     */
    @Test
    void teacherCanQueryOthersProfile() {
        StudentProfile others = new StudentProfile("uuid-8002", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(others);

        Message response = send(new Message(Command.STUDENT_QUERY, others.getId()),
                teacherToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 教师改学籍状态（206）应回 SUCCESS 且状态生效。
     */
    @Test
    void changeStatusReturnsSuccess() {
        StudentProfile profile = new StudentProfile("uuid-9001", 2026,
                CampusStatus.ENROLLED);
        service.registerStudent(profile);

        StudentProfile change = new StudentProfile();
        change.setId(profile.getId());
        change.setStatus(CampusStatus.SUSPENDED);

        Message response = send(new Message(Command.STUDENT_CHANGE_STATUS, change),
                teacherToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(CampusStatus.SUSPENDED,
                service.queryProfile(profile.getId()).getStatus());
    }

    /**
     * 改不存在的学籍状态应回 404。
     */
    @Test
    void changeStatusMissingReturnsNotFound() {
        StudentProfile change = new StudentProfile();
        change.setId(9999L);
        change.setStatus(CampusStatus.GRADUATED);

        Message response = send(new Message(Command.STUDENT_CHANGE_STATUS, change),
                adminToken);

        assertEquals(StatusCode.NOT_FOUND, response.getStatusCode());
    }

    /**
     * 请求体里没有目标主键且会话用户名下无学籍时应回 404（而不是 500）。
     */
    @Test
    void queryOwnProfileWithoutRecordReturnsNotFound() {
        Message response = send(new Message(Command.STUDENT_QUERY, null), adminToken);

        assertEquals(StatusCode.NOT_FOUND, response.getStatusCode());
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
