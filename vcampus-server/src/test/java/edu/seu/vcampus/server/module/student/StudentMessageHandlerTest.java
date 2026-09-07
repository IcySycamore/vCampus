package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.entity.EnrollmentStatus;
import edu.seu.vcampus.common.entity.StudentProfile;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 学籍消息处理器测试：命令码分支、成功/失败状态码。
 */
class StudentMessageHandlerTest {

    /** 被测处理器。 */
    private StudentMessageHandler handler;

    /** 底层服务（内存 DAO）。 */
    private StudentService service;

    /**
     * 每个测试前重建处理器。
     */
    @BeforeEach
    void setUp() {
        service = new StudentService(new StudentDaoMemory());
        handler = new StudentMessageHandler(service);
    }

    /**
     * 查询命令（201）应回 SUCCESS 并携带学籍记录。
     */
    @Test
    void queryReturnsProfile() {
        StudentProfile profile = new StudentProfile(1001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_QUERY, profile.getId()));

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
        Message response = send(new Message(Command.STUDENT_QUERY, 9999L));

        assertEquals(StatusCode.NOT_FOUND, response.getStatusCode());
    }

    /**
     * 登记命令（204）应回 SUCCESS。
     */
    @Test
    void registerReturnsSuccess() {
        StudentProfile profile = new StudentProfile(2001L, 2026,
                EnrollmentStatus.ENROLLED);

        Message response = send(new Message(Command.STUDENT_REGISTER, profile));

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    /**
     * 更新命令（202）应回 SUCCESS。
     */
    @Test
    void updateReturnsSuccess() {
        StudentProfile profile = new StudentProfile(3001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);
        profile.setStatus(EnrollmentStatus.SUSPENDED);

        Message response = send(new Message(Command.STUDENT_MODIFY_APPLY, profile));

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(EnrollmentStatus.SUSPENDED,
                service.queryProfile(profile.getId()).getStatus());
    }

    /**
     * 删除命令（205）应回 SUCCESS，且记录被软删除。
     */
    @Test
    void deleteReturnsSuccess() {
        StudentProfile profile = new StudentProfile(4001L, 2026,
                EnrollmentStatus.GRADUATED);
        service.registerStudent(profile);

        Message response = send(new Message(Command.STUDENT_DELETE, profile.getId()));

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(null, service.queryProfile(profile.getId()));
    }

    /**
     * 审核命令（203）暂未实现，应回 400。
     */
    @Test
    void auditReturnsBadRequest() {
        Message response = send(new Message(Command.STUDENT_MODIFY_AUDIT, null));

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 查询命令 data 类型错误应回 400。
     */
    @Test
    void queryWithWrongTypeReturnsBadRequest() {
        Message response = send(new Message(Command.STUDENT_QUERY, "not-a-long"));

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 未知学籍命令应回 400。
     */
    @Test
    void unknownCommandReturnsBadRequest() {
        Message response = send(new Message(299, null));

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 发送一条请求并捕获响应。
     *
     * @param request 请求
     * @return 响应
     */
    private Message send(Message request) {
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
