package edu.seu.vcampus.server;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyAuditRequest;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务端端到端集成测试：真起服务器 + 真 socket 连接，验证 「监听 → 线程池 → ClientThread 连接级鉴权 → 全局分发器路由 → 业务处理器 →
 * 响应经同一连接回传」这条完整链路，而不是各层单测拼凑。
 *
 * <p>
 * 装配走的是生产入口 {@link VCampusServerApp#startServer(int)}，因此这里能跑通 就意味着真实启动路径可用。端口用 0 由系统分配，避免与本机占用冲突。
 *
 * <p>
 * 连接与收发夹具（{@code TestClient}）见父类 {@link ServerEndToEndSupport}。
 */
class ServerEndToEndTest extends ServerEndToEndSupport {

    /** 预置管理员账号。 */
    private static final String ADMIN_NAME = "e2e_admin";

    /** 预置管理员密码。 */
    private static final String ADMIN_PASSWORD = "e2e_pwd_2026";

    /** 测试学生账号。 */
    private static final String STUDENT_NAME = "e2e_student";

    /** 测试学生密码。 */
    private static final String STUDENT_PASSWORD = "e2e_stu_pwd";

    /** 测试教师账号。 */
    private static final String TEACHER_NAME = "e2e_teacher";

    /** 测试教师密码。 */
    private static final String TEACHER_PASSWORD = "e2e_tea_pwd";

    /** 测试教师姓名。 */
    private static final String TEACHER_DISPLAY_NAME = "端到端教师";

    /** 审核闭环用例的学生账号。 */
    private static final String MODIFY_STUDENT_NAME = "e2e_modify_student";

    /** 审核闭环用例的学生密码。 */
    private static final String MODIFY_STUDENT_PASSWORD = "e2e_mod_pwd";

    /** 审核闭环用例的教师账号（具备审批权）。 */
    private static final String MODIFY_TEACHER_NAME = "e2e_modify_teacher";

    /** 审核闭环用例的教师密码。 */
    private static final String MODIFY_TEACHER_PASSWORD = "e2e_mod_tea_pwd";

    /** 等待服务器开始监听的上限（毫秒）。 */
    private static final long STARTUP_TIMEOUT_MILLIS = 5000L;

    /** 服务端线程。 */
    private static Thread s_serverThread;

    /** 实际监听端口。 */
    private static int s_port;

    /**
     * 启动测试服务器并预置管理员。
     *
     * @throws Exception 启动失败
     */
    @BeforeAll
    static void startServer() throws Exception {
        // 账户库与引导文件改到临时目录：测试不污染工作目录，并覆盖「账号从本地文件导入」的真实路径。
        File directory = Files.createTempDirectory("vcampus-e2e").toFile();
        directory.deleteOnExit();
        File bootstrap = new File(directory, "admins.tsv");
        Writer writer = new OutputStreamWriter(new FileOutputStream(bootstrap),
                Charset.forName("UTF-8"));
        try {
            writer.write(ADMIN_NAME + "\t端到端管理员\t" + ADMIN_PASSWORD + "\t管理员\n");
        } finally {
            writer.close();
        }
        System.setProperty("vcampus.users.file", new File(directory, "users.tsv").getPath());
        System.setProperty("vcampus.admins.file", bootstrap.getPath());

        s_serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VCampusServerApp.startServer(0);
                } catch (IOException e) {
                    System.err.println("测试服务器退出: " + e.getMessage());
                }
            }
        }, "e2e-server");
        s_serverThread.setDaemon(true);
        s_serverThread.start();

        final long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MILLIS;
        while (s_port <= 0 && System.currentTimeMillis() < deadline) {
            s_port = VCampusServerApp.getPort();
            if (s_port <= 0) {
                Thread.sleep(20L);
            }
        }
        assertTrue(s_port > 0, "测试服务器未在 5 秒内开始监听");
    }

    /**
     * 停止测试服务器。
     *
     * @throws Exception 停止失败
     */
    @AfterAll
    static void stopServer() throws Exception {
        VCampusServerApp.stopServer();
        s_serverThread.join(3000L);
    }

    /**
     * 审核闭环走真 socket：学生提申请 → 学生看得到 → <b>管理员看得到</b> → 教师通过 → 学籍真的变。
     *
     * <p>
     * 这条用例源自一条真实反馈：「管理员在修改审核页看不到任何申请」。当时服务端单测全绿，根因在
     * 客户端把「填写」与「申请修改」挂在同一个按钮上，学生点下去走的是立即生效那条路，审核队列
     * 于是长期是空的。所以这里刻意不做 shortcut：从 202 一直走到 201，确认申请真的出现在管理员
     * 的待审列表里，且通过后真的落到学生档案上。
     *
     * @throws Exception 通信失败
     */
    @Test
    void modifyRequestReachesAdminAndApprovalUpdatesProfile() throws Exception {
        try (TestClient client = new TestClient(s_port)) {
            String adminToken = client.login(ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(adminToken, "管理员登录应返回 token");
            // 账号可能已存在（认证服务为全局单例），已存在时注册回 400，不影响后续登录
            client.registerUser(MODIFY_STUDENT_NAME, MODIFY_STUDENT_PASSWORD,
                    Role.STUDENT.getDisplayName(), adminToken);
            client.registerUser(MODIFY_TEACHER_NAME, MODIFY_TEACHER_PASSWORD,
                    Role.TEACHER.getDisplayName(), adminToken);
            String studentToken = client.login(MODIFY_STUDENT_NAME, MODIFY_STUDENT_PASSWORD);
            String teacherToken = client.login(MODIFY_TEACHER_NAME, MODIFY_TEACHER_PASSWORD);
            assertNotNull(studentToken, "学生登录应返回 token");
            assertNotNull(teacherToken, "教师登录应返回 token");

            // 学生查自己的学籍（开户钩子已建档）；万一没有则先自助填写（204）
            StudentProfile mine = (StudentProfile) queryMine(client, studentToken);
            if (mine == null) {
                StudentProfile blank = new StudentProfile(null, 2026, CampusStatus.ENROLLED);
                blank.setField("待教务填写");
                Message enroll = new Message(Command.STUDENT_REGISTER, blank);
                enroll.setToken(studentToken);
                assertEquals(StatusCode.SUCCESS, client.exchange(enroll).getStatusCode(),
                        "学生自助填写学籍应成功");
                mine = (StudentProfile) queryMine(client, studentToken);
            }
            assertNotNull(mine, "学生应能查到自己的学籍");
            long profileId = mine.getId().longValue();
            String fieldBefore = mine.getField();

            // 学生提申请 → 200，且学籍当场不变（审核流的意义就在这里）
            Map<String, String> changes = new LinkedHashMap<String, String>();
            changes.put("joinYear", "2024");
            changes.put("field", "软件工程");
            Message apply = new Message(Command.STUDENT_MODIFY_APPLY,
                    new edu.seu.vcampus.common.student.dto.StudentModifyRequest(
                            Long.valueOf(profileId), changes, "入学年份录错"));
            apply.setToken(studentToken);
            assertEquals(StatusCode.SUCCESS, client.exchange(apply).getStatusCode(),
                    "学生提交修改申请应成功");
            assertEquals(fieldBefore,
                    ((StudentProfile) queryMine(client, studentToken)).getField(),
                    "提交申请不应立即改学籍");

            // 学生看得到自己那条待审申请；管理员看得到同一条——后者正是那条反馈的落点
            assertEquals(ModifyRequestStatus.PENDING,
                    firstRequest(client, studentToken, profileId, ModifyRequestStatus.PENDING)
                            .getStatus(),
                    "学生应能看到自己提交的待审申请");
            StudentModifyRequest pending = firstRequest(client, adminToken, profileId,
                    ModifyRequestStatus.PENDING);
            assertEquals("软件工程", decodeField(pending.getChangesJson()),
                    "管理员的待审列表里应出现学生刚提交的那条申请");

            // 教师看到同一批申请，但审核应被拒：教师对学籍只读（能查不能改）
            Message teacherAudit = new Message(Command.STUDENT_MODIFY_AUDIT,
                    new ModifyAuditRequest(pending.getRequestId(), Boolean.TRUE, "越权尝试"));
            teacherAudit.setToken(teacherToken);
            assertEquals(StatusCode.FORBIDDEN, client.exchange(teacherAudit).getStatusCode(),
                    "教师没有审核权，203 应回 403");
            assertEquals(ModifyRequestStatus.PENDING,
                    firstRequest(client, adminToken, profileId, ModifyRequestStatus.PENDING)
                            .getStatus(),
                    "被拒的审核不应改动申请单");

            // 管理员通过 → 200；学生再查，学籍已落实、申请单状态已变
            Message audit = new Message(Command.STUDENT_MODIFY_AUDIT,
                    new ModifyAuditRequest(pending.getRequestId(), Boolean.TRUE, "情况属实"));
            audit.setToken(adminToken);
            assertEquals(StatusCode.SUCCESS, client.exchange(audit).getStatusCode(),
                    "管理员审核通过应成功");

            StudentProfile updated = (StudentProfile) queryMine(client, studentToken);
            assertEquals(2024, updated.getJoinYear(), "通过后入学年份应落实到学籍");
            assertEquals("软件工程", updated.getField(), "通过后学术方向应落实到学籍");
            assertEquals(ModifyRequestStatus.APPROVED,
                    firstRequest(client, studentToken, profileId, ModifyRequestStatus.APPROVED)
                            .getStatus(),
                    "学生应看到自己的申请已通过");
        }
    }

    /**
     * 查自己的学籍；名下没有记录时返回 null。
     *
     * @param client 测试客户端
     * @param token 会话令牌
     * @return 本人学籍；没有则 null
     * @throws Exception 通信失败
     */
    private static StudentProfile queryMine(TestClient client, String token) throws Exception {
        Message query = new Message(Command.STUDENT_QUERY, null);
        query.setToken(token);
        Message response = client.exchange(query);
        return StatusCode.SUCCESS.equals(response.getStatusCode())
                ? (StudentProfile) response.getData()
                : null;
    }

    /**
     * 取指定学籍下某状态的第一条申请单。
     *
     * @param client 测试客户端
     * @param token 会话令牌
     * @param profileId 目标学籍主键
     * @param status 申请单状态
     * @return 申请单
     * @throws Exception 通信失败
     */
    private static StudentModifyRequest firstRequest(TestClient client, String token,
            long profileId, ModifyRequestStatus status) throws Exception {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setProfileId(Long.valueOf(profileId));
        query.setStatus(status);
        Message request = new Message(Command.STUDENT_MODIFY_LIST, query);
        request.setToken(token);
        Message response = client.exchange(request);
        assertEquals(StatusCode.SUCCESS, response.getStatusCode(), "207 应成功");
        PageResponse<?> page = (PageResponse<?>) response.getData();
        assertTrue(page.getTotal() > 0L, "应至少有一条 " + status.getDisplayName() + " 申请");
        return (StudentModifyRequest) page.getItems().get(0);
    }

    /**
     * 从变更文本里取学术方向的新值（{@code 字段=值} 以 {@code ;} 分隔）。
     *
     * @param changesJson 变更文本
     * @return 学术方向的新值；没有该字段返回 null
     */
    private static String decodeField(String changesJson) {
        if (changesJson == null) {
            return null;
        }
        String[] entries = changesJson.split(";");
        int index = 0;
        while (index < entries.length) {
            String[] pair = entries[index].split("=", 2);
            if (pair.length == 2 && "field".equals(pair[0])) {
                return pair[1];
            }
            index = index + 1;
        }
        return null;
    }

    /**
     * 未携带 token 的业务命令应在连接层被拦下并回 401， 而不是让 NPE 中断连接。
     *
     * @throws Exception 通信失败
     */
    @Test
    void unauthenticatedRequestIsRejectedByConnectionLayer() throws Exception {
        try (TestClient client = new TestClient(s_port)) {
            Message request = new Message(Command.STUDENT_QUERY, 1L);

            Message response = client.exchange(request);

            assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode(),
                    "未带 token 的学籍查询应由连接层拦下回 401");
        }
    }

    /**
     * 管理员全流程：登录 → 查询不存在 → 登记 → 查询命中 → 改状态。
     *
     * @throws Exception 通信失败
     */
    @Test
    void adminCanLoginAndWalkStudentFlow() throws Exception {
        try (TestClient client = new TestClient(s_port)) {
            String token = client.login(ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(token, "管理员登录应返回 token");

            // 查询尚不存在的学籍 → 404
            Message queryMissing = new Message(Command.STUDENT_QUERY, 9999L);
            queryMissing.setToken(token);
            assertEquals(StatusCode.NOT_FOUND, client.exchange(queryMissing).getStatusCode(),
                    "查询不存在的学籍应回 404");

            // 登记学籍 → 200（主键由服务端分配，客户端本地对象拿不到写回值）
            StudentProfile profile = new StudentProfile("uuid-e2e-1", 2026,
                    CampusStatus.ENROLLED);
            Message register = new Message(Command.STUDENT_REGISTER, profile);
            register.setToken(token);
            assertEquals(StatusCode.SUCCESS, client.exchange(register).getStatusCode(),
                    "管理员登记学籍应成功");

            // 主键由服务端自增分配，客户端按序探测出刚登记那条
            long allocatedId = client.findProfileId(token, "uuid-e2e-1");
            assertTrue(allocatedId > 0, "登记后应能查到该学籍记录");

            // 改学籍状态 → 200
            StudentProfile statusChange = new StudentProfile();
            statusChange.setId(allocatedId);
            statusChange.setStatus(CampusStatus.SUSPENDED);
            Message change = new Message(Command.STUDENT_CHANGE_STATUS, statusChange);
            change.setToken(token);
            assertEquals(StatusCode.SUCCESS, client.exchange(change).getStatusCode(),
                    "管理员改学籍状态应成功");

            // 再查同一主键 → 200，且字段与登记/修改结果一致
            Message queryById = new Message(Command.STUDENT_QUERY, allocatedId);
            queryById.setToken(token);
            Message queryResponse = client.exchange(queryById);
            assertEquals(StatusCode.SUCCESS, queryResponse.getStatusCode(), "已登记的学籍应可查到");
            StudentProfile found = (StudentProfile) queryResponse.getData();
            assertEquals("uuid-e2e-1", found.getUserUuid(), "查到的学籍应属于登记时的用户 uuid");
            assertEquals(CampusStatus.SUSPENDED, found.getStatus(), "改状态后查询应返回新状态");
        }
    }

    /**
     * 学生角色：能登录、能查自己的学籍，但登记学籍与改状态应被拒 403。
     *
     * <p>
     * 注意 201 的口径已按设计文档收窄：学生只能查自己的（请求不带目标主键），按主键查他人 应被拒。这里只断言「鉴权层放行且不被当成未登录」，精确的 403 断言在
     * {@code StudentMessageHandlerTest} 里（那里能保证目标记录一定存在）。
     *
     * @throws Exception 通信失败
     */
    @Test
    void studentCannotRegisterOrChangeStatus() throws Exception {
        try (TestClient client = new TestClient(s_port)) {
            String adminToken = client.login(ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(adminToken, "管理员登录应返回 token");

            // 学生账号可能已存在（认证服务为全局单例），已存在时注册回 400，不影响后续登录
            client.registerUser(STUDENT_NAME, STUDENT_PASSWORD, Role.STUDENT.getDisplayName(),
                    adminToken);

            String studentToken = client.login(STUDENT_NAME, STUDENT_PASSWORD);
            assertNotNull(studentToken, "学生登录应返回 token");

            // 查自己的学籍：登录即可，不该被拒；名下尚无记录时回 404 也属正常
            Message query = new Message(Command.STUDENT_QUERY, null);
            query.setToken(studentToken);
            String queryStatus = client.exchange(query).getStatusCode();
            assertTrue(
                    StatusCode.SUCCESS.equals(queryStatus)
                            || StatusCode.NOT_FOUND.equals(queryStatus),
                    "学生查询本人学籍不应被拒，实得 " + queryStatus);

            // 按主键查他人：期望 403；该主键恰好不存在时回 404，两者都说明鉴权层已放行
            Message othersQuery = new Message(Command.STUDENT_QUERY, 1L);
            othersQuery.setToken(studentToken);
            String othersStatus = client.exchange(othersQuery).getStatusCode();
            assertTrue(
                    StatusCode.FORBIDDEN.equals(othersStatus)
                            || StatusCode.NOT_FOUND.equals(othersStatus),
                    "学生查询他人学籍应被拒或未找到，实得 " + othersStatus);

            // 登记学籍 → 403
            StudentProfile profile = new StudentProfile("uuid-e2e-2", 2026,
                    CampusStatus.ENROLLED);
            Message register = new Message(Command.STUDENT_REGISTER, profile);
            register.setToken(studentToken);
            assertEquals(StatusCode.FORBIDDEN, client.exchange(register).getStatusCode(),
                    "学生登记学籍应被拒 403");

            // 改学籍状态 → 403
            StudentProfile statusChange = new StudentProfile();
            statusChange.setId(1L);
            statusChange.setStatus(CampusStatus.WITHDRAWN);
            Message change = new Message(Command.STUDENT_CHANGE_STATUS, statusChange);
            change.setToken(studentToken);
            assertEquals(StatusCode.FORBIDDEN, client.exchange(change).getStatusCode(),
                    "学生改学籍状态应被拒 403");
        }
    }

    /**
     * 教师角色：注册即建档，登录后用 201 能拿到姓名与人员类别。
     *
     * <p>
     * 这一条对应「我可以用 201 获取到教师和学生的名字吗」与「教师也有信息查看需求」： 教师若没有档案，201 只会回 404，个人信息页的在校档案就是空的；同时按方向检索
     * 也永远只命中学生，教师那一侧是空的。
     *
     * @throws Exception 通信失败
     */
    @Test
    void teacherCanQueryOwnProfileWithName() throws Exception {
        try (TestClient client = new TestClient(s_port)) {
            String adminToken = client.login(ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(adminToken, "管理员登录应返回 token");

            // 账号可能已存在（认证服务为全局单例），已存在时注册回 400，不影响后续登录
            client.registerUser(TEACHER_NAME, TEACHER_DISPLAY_NAME, TEACHER_PASSWORD,
                    Role.TEACHER.getDisplayName(), adminToken);

            String teacherToken = client.login(TEACHER_NAME, TEACHER_PASSWORD);
            assertNotNull(teacherToken, "教师登录应返回 token");

            Message query = new Message(Command.STUDENT_QUERY, null);
            query.setToken(teacherToken);
            Message response = client.exchange(query);

            assertEquals(StatusCode.SUCCESS, response.getStatusCode(),
                    "教师查本人档案应成功（注册即建档），实得 " + response.getStatusCode());
            StudentProfile profile = (StudentProfile) response.getData();
            assertEquals(PersonCategory.TEACHER, profile.getPersonCategory());
            assertEquals(TEACHER_DISPLAY_NAME, profile.getRealName());
        }
    }

    /**
     * 本类已无内嵌夹具：连接与收发逻辑统一在父类 {@link ServerEndToEndSupport} 的 {@code TestClient}。
     */
}
