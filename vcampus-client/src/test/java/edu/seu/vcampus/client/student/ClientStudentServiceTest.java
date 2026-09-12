package edu.seu.vcampus.client.student;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.ClientSession;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学籍客户端服务测试：请求参数、token 填充、失败收敛成 {@link ApiException}。
 * 分发器被 mock，不碰网络，只验证「发出去什么」与「收到什么会抛异常」。
 */
class ClientStudentServiceTest {

    /** 被 mock 的分发器。 */
    private ClientMessageDispatcher dispatcher;

    /** 被测服务。 */
    private StudentService service;

    /**
     * 每个测试前重建 mock 分发器与内存会话。
     */
    @BeforeEach
    void setUp() {
        dispatcher = mock(ClientMessageDispatcher.class);
        ClientSession session = new ClientSession();
        session.cache("token-1", new SessionEntry("uuid-stu", "stu001", "学生", 0L));
        service = new StudentService(dispatcher, session, 100L);
    }

    /**
     * 查询我的学籍：成功时直接返回服务端载荷。
     *
     * @throws Exception mock 调用可能抛出的中断异常
     */
    @Test
    void queryMyProfileReturnsPayload() throws Exception {
        StudentProfile profile = new StudentProfile("uuid-stu", 2026,
                CampusStatus.ENROLLED);
        when(dispatcher.request(any(Message.class), anyLong()))
                .thenReturn(ok(Command.STUDENT_QUERY, profile));

        StudentProfile result = service.queryMyProfile();

        assertNotNull(result);
        assertEquals("uuid-stu", result.getUserUuid());
    }

    /**
     * 请求必须带上会话里的 token，否则服务端一定回 401。
     *
     * @throws Exception mock 调用可能抛出的中断异常
     */
    @Test
    void requestCarriesSessionToken() throws Exception {
        when(dispatcher.request(any(Message.class), anyLong()))
                .thenReturn(ok(Command.STUDENT_QUERY, new StudentProfile()));

        service.queryMyProfile();

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(dispatcher).request(captor.capture(), anyLong());
        assertEquals("token-1", captor.getValue().getToken());
        assertEquals(Command.STUDENT_QUERY, captor.getValue().getCommand());
    }

    /**
     * 服务器回 403 时抛 ApiException，且能识别出「无权限」。
     *
     * @throws Exception mock 调用可能抛出的中断异常
     */
    @Test
    void forbiddenThrowsApiException() throws Exception {
        when(dispatcher.request(any(Message.class), anyLong()))
                .thenReturn(error(Command.STUDENT_LIST, StatusCode.FORBIDDEN, "无权限"));

        try {
            service.listStudents(new StudentQuery());
            fail("应当抛出 ApiException");
        } catch (ApiException exception) {
            assertEquals(StatusCode.FORBIDDEN, exception.getStatusCode());
            assertTrue(exception.isForbidden());
            assertEquals("无权限", exception.getMessage());
        }
    }

    /**
     * 超时（返回 null）与线程中断都要收敛成 ApiException，不能返回 null 让界面崩。
     *
     * @throws Exception mock 调用可能抛出的中断异常
     */
    @Test
    void timeoutAndInterruptThrowApiException() throws Exception {
        when(dispatcher.request(any(Message.class), anyLong())).thenReturn(null);

        try {
            service.queryMyProfile();
            fail("应当抛出 ApiException");
        } catch (ApiException exception) {
            assertEquals(null, exception.getStatusCode());
        }

        when(dispatcher.request(any(Message.class), anyLong()))
                .thenThrow(new InterruptedException("被中断"));

        try {
            service.queryMyProfile();
            fail("应当抛出 ApiException");
        } catch (ApiException exception) {
            assertTrue(Thread.interrupted());
        }
    }

    /**
     * 审核接口要把字符串编号解析成主键；非数字直接在本地拒绝，不发请求。
     *
     * @throws Exception mock 调用可能抛出的中断异常
     */
    @Test
    void auditRejectsNonNumericId() throws Exception {
        try {
            service.auditModification("abc", true, "同意");
            fail("应当抛出 ApiException");
        } catch (ApiException exception) {
            assertNotNull(exception.getMessage());
        }
    }

    /**
     * 审核与删除：编号被解析后原样发出，命令码正确。
     *
     * @throws Exception mock 调用可能抛出的中断异常
     */
    @Test
    void writeCommandsSendExpectedPayload() throws Exception {
        when(dispatcher.request(any(Message.class), anyLong()))
                .thenReturn(ok(Command.STUDENT_MODIFY_AUDIT, null))
                .thenReturn(ok(Command.STUDENT_DELETE, null));

        service.auditModification("42", true, "同意");
        service.deleteStudent(7L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(dispatcher, times(2)).request(captor.capture(), anyLong());
        assertEquals(Command.STUDENT_MODIFY_AUDIT,
                captor.getAllValues().get(0).getCommand());
        assertEquals(Command.STUDENT_DELETE,
                captor.getAllValues().get(1).getCommand());
    }
    /**
     * 生成一个成功响应。
     *
     * @param command 命令码
     * @param data 载荷
     * @return 响应消息
     */
    private static Message ok(int command, Object data) {
        Message response = new Message(command, data);
        response.setStatusCode(StatusCode.SUCCESS);
        return response;
    }

    /**
     * 生成一个失败响应。
     *
     * @param command 命令码
     * @param statusCode 状态码
     * @param data 错误说明
     * @return 响应消息
     */
    private static Message error(int command, String statusCode, Object data) {
        Message response = new Message(command, data);
        response.setStatusCode(statusCode);
        return response;
    }
}
