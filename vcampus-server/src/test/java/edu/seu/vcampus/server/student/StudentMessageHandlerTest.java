package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.EnrollmentStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        adminToken = sessions.create("uuid-admin", "admin", "管理员");
        studentToken = sessions.create("uuid-stu", "stu001", "学生");
        teacherToken = sessions.create("uuid-tea", "tea001", "教师");
    }

    /**
     * 登记命令（204）应回 SUCCESS（管理员）。
     */
    @Test
    void registerReturnsSuccess() {
        StudentProfile profile = new StudentProfile("uuid-2001", 2026, EnrollmentStatus.ENROLLED);

        Message response = send(new Message(Command.STUDENT_REGISTER, profile), adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 提交修改申请（202）应回 SUCCESS，且只创建待审申请、不直接改学籍。
     *
     * <p>
     * 这是审核流的核心语义：学生提交后学籍立即改变就失去了审核的意义。
     */
    @Test
    void applyModificationCreatesPendingRequest() {
        StudentProfile profile = new StudentProfile("uuid-stu", 2026, EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("status", "SUSPENDED");
        StudentModifyRequest dto = new StudentModifyRequest(profile.getId(), changes, "状态有误");

        Message response = send(new Message(Command.STUDENT_MODIFY_APPLY, dto), studentToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(EnrollmentStatus.ENROLLED,
                service.queryProfile(profile.getId()).getStatus());
        assertEquals(1L, service.listModifyRequests(null).getTotal());
    }

    /**
     * 删除命令（205）应回 SUCCESS（管理员），且记录被软删除。
     */
    @Test
    void deleteReturnsSuccess() {
        StudentProfile profile = new StudentProfile("uuid-4001", 2026, EnrollmentStatus.GRADUATED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_DELETE, profile.getId()), adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(null, service.queryProfile(profile.getId()));
    }

    /**
     * 审核命令（203）不携带参数应回 400（缺少审核对象）。
     */
    @Test
    void auditWithoutPayloadReturnsBadRequest() {
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
     * 发送一条请求并捕获响应。
     *
     * @param request 请求
     * @param token 会话令牌（可为 null）
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
