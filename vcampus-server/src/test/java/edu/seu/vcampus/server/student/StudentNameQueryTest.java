package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.InMemoryUserRepository;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端：201 / 208 真的能把师生姓名带回来。
 *
 * <p>
 * 这是组长那条需求（「我可以用 201 获取到教师和学生的名字吗？」）的验收用例。链路是：
 * 用户模块存姓名 → 学籍只存账户 uuid → 服务端查询时按 uuid 联查补名 → 客户端一次请求拿全。
 * 单测只覆盖了装饰器本身，这里把 handler、service、用户仓储串起来跑一遍。
 */
class StudentNameQueryTest {

    /** 用户凭证存储（师生姓名在这里）。 */
    private UserRepository users;

    /** 学籍服务（已接入姓名联查）。 */
    private StudentService service;

    /** 被测处理器。 */
    private StudentMessageHandler handler;

    /** 会话管理器。 */
    private SessionManager sessions;

    /** 管理员 token。 */
    private String adminToken;

    /** 学生 token（uuid-stu）。 */
    private String studentToken;

    /**
     * 每个用例前重建存储，并预置一个带姓名的学生。
     */
    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        sessions = new SessionManager();
        service = new StudentService(new StudentDaoMemory(),
                new StudentModifyRequestDaoMemory(), users);
        handler = new StudentMessageHandler(service, sessions);
        adminToken = sessions.create("uuid-admin", "admin", "管理员");
        studentToken = sessions.create("uuid-stu", "001", "张三", "学生");

        users.save(new UserRepository.Credential("001", "uuid-stu", "张三", "salt", "hash",
                "学生", true));
        service.registerStudent(new StudentProfile("uuid-stu", 2026,
                CampusStatus.ENROLLED));
    }

    /**
     * 管理员按主键查学籍，返回的档案里就带姓名——客户端不必再发第二次请求。
     */
    @Test
    void query201CarriesStudentName() {
        StudentProfile stored = service.queryByUserUuid("uuid-stu");

        Message response = send(new Message(Command.STUDENT_QUERY, stored.getId()), adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        StudentProfile found = (StudentProfile) response.getData();
        assertEquals("张三", found.getRealName());
    }

    /**
     * 教师档案同样能带出姓名，且人员类别标记正确（201 对师生一视同仁）。
     */
    @Test
    void query201CarriesTeacherName() {
        users.save(new UserRepository.Credential("t01", "uuid-tea", "李老师", "salt", "hash",
                "教师", true));
        service.registerStudent(new StudentProfile("uuid-tea", PersonCategory.TEACHER, 2020,
                CampusStatus.ENROLLED));
        StudentProfile stored = service.queryByUserUuid("uuid-tea");

        Message response = send(new Message(Command.STUDENT_QUERY, stored.getId()), adminToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        StudentProfile found = (StudentProfile) response.getData();
        assertEquals("李老师", found.getRealName());
        assertEquals(PersonCategory.TEACHER, found.getPersonCategory());
    }

    /**
     * 学生查自己（201 不带参数）也能拿到姓名。
     */
    @Test
    void studentSeesOwnName() {
        Message response = send(new Message(Command.STUDENT_QUERY, null), studentToken);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        StudentProfile found = (StudentProfile) response.getData();
        assertEquals("张三", found.getRealName());
    }

    /**
     * 学籍列表（208）里每一条都带姓名，不是只补第一条。
     */
    @Test
    void list208CarriesEveryName() {
        users.save(new UserRepository.Credential("002", "uuid-stu2", "李四", "salt", "hash",
                "学生", true));
        service.registerStudent(new StudentProfile("uuid-stu2", 2025,
                CampusStatus.ENROLLED));

        PageResponse<StudentProfile> page = service.listStudents(new StudentQuery());

        assertEquals(2L, page.getTotal());
        Set<String> names = new HashSet<String>();
        names.add(page.getItems().get(0).getRealName());
        names.add(page.getItems().get(1).getRealName());
        assertTrue(names.contains("张三"), "列表里应含张三，实得 " + names);
        assertTrue(names.contains("李四"), "列表里应含李四，实得 " + names);
    }

    /**
     * 账户没采集姓名时用账户 uuid 顶上，保证姓名字段非空、界面不会出现空白。
     */
    @Test
    void missingNameFallsBackToUuid() {
        users.save(new UserRepository.Credential("002", "uuid-x", null, "salt", "hash",
                "学生", true));
        service.registerStudent(new StudentProfile("uuid-x", 2025, CampusStatus.ENROLLED));
        StudentProfile stored = service.queryByUserUuid("uuid-x");

        Message response = send(new Message(Command.STUDENT_QUERY, stored.getId()), adminToken);

        StudentProfile found = (StudentProfile) response.getData();
        assertEquals("uuid-x", found.getRealName());
    }

    /**
     * 发送一条请求并捕获响应。
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
