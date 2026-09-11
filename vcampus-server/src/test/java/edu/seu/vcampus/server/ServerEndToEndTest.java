package edu.seu.vcampus.server;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.entity.EnrollmentStatus;
import edu.seu.vcampus.common.entity.StudentProfile;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.util.Sha256Util;
import edu.seu.vcampus.server.auth.AuthService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务端端到端集成测试：真起服务器 + 真 socket 连接，验证
 * 「监听 → 线程池 → ClientThread 连接级鉴权 → 全局分发器路由 → 业务处理器 →
 * 响应经同一连接回传」这条完整链路，而不是各层单测拼凑。
 *
 * <p>装配走的是生产入口 {@link VCampusServerApp#startServer(int)}，因此这里能跑通
 * 就意味着真实启动路径可用。端口用 0 由系统分配，避免与本机占用冲突。
 */
class ServerEndToEndTest {

    /** 预置管理员账号。 */
    private static final String ADMIN_NAME = "e2e_admin";

    /** 预置管理员密码。 */
    private static final String ADMIN_PASSWORD = "e2e_pwd_2026";

    /** 测试学生账号。 */
    private static final String STUDENT_NAME = "e2e_student";

    /** 测试学生密码。 */
    private static final String STUDENT_PASSWORD = "e2e_stu_pwd";

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
        // 注册命令要求管理员会话，冷启动时库中无任何账号，故直接经认证服务落库。
        try {
            AuthService.getInstance().register(ADMIN_NAME, ADMIN_PASSWORD,
                    Role.ADMIN.getDisplayName());
        } catch (IllegalStateException alreadyExists) {
            // 认证服务为全局单例，重复启动时账号已存在，忽略即可
        }

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

        final long deadline = System.currentTimeMillis()
                + STARTUP_TIMEOUT_MILLIS;
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
     * 未携带 token 的业务命令应在连接层被拦下并回 401，
     * 而不是让 NPE 中断连接。
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
            assertEquals(StatusCode.NOT_FOUND,
                    client.exchange(queryMissing).getStatusCode(),
                    "查询不存在的学籍应回 404");

            // 登记学籍 → 200（主键由服务端分配，客户端本地对象拿不到写回值）
            StudentProfile profile = new StudentProfile("uuid-e2e-1", 2026,
                    EnrollmentStatus.ENROLLED);
            Message register = new Message(Command.STUDENT_REGISTER, profile);
            register.setToken(token);
            assertEquals(StatusCode.SUCCESS,
                    client.exchange(register).getStatusCode(),
                    "管理员登记学籍应成功");

            // 主键由服务端自增分配，客户端按序探测出刚登记那条
            long allocatedId = client.findProfileId(token, "uuid-e2e-1");
            assertTrue(allocatedId > 0, "登记后应能查到该学籍记录");

            // 改学籍状态 → 200
            StudentProfile statusChange = new StudentProfile();
            statusChange.setId(allocatedId);
            statusChange.setStatus(EnrollmentStatus.SUSPENDED);
            Message change = new Message(Command.STUDENT_CHANGE_STATUS,
                    statusChange);
            change.setToken(token);
            assertEquals(StatusCode.SUCCESS,
                    client.exchange(change).getStatusCode(),
                    "管理员改学籍状态应成功");

            // 再查同一主键 → 200，且字段与登记/修改结果一致
            Message queryById = new Message(Command.STUDENT_QUERY,
                    allocatedId);
            queryById.setToken(token);
            Message queryResponse = client.exchange(queryById);
            assertEquals(StatusCode.SUCCESS, queryResponse.getStatusCode(),
                    "已登记的学籍应可查到");
            StudentProfile found = (StudentProfile) queryResponse.getData();
            assertEquals("uuid-e2e-1", found.getUserUuid(),
                    "查到的学籍应属于登记时的用户 uuid");
            assertEquals(EnrollmentStatus.SUSPENDED, found.getStatus(),
                    "改状态后查询应返回新状态");
        }
    }

    /**
     * 学生角色：能登录、能查学籍，但登记学籍与改状态应被拒 403。
     *
     * @throws Exception 通信失败
     */
    @Test
    void studentCannotRegisterOrChangeStatus() throws Exception {
        try (TestClient client = new TestClient(s_port)) {
            String adminToken = client.login(ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(adminToken, "管理员登录应返回 token");

            // 学生账号可能已存在（认证服务为全局单例），已存在时注册回 400，不影响后续登录
            client.registerUser(STUDENT_NAME, STUDENT_PASSWORD,
                    Role.STUDENT.getDisplayName(), adminToken);

            String studentToken = client.login(STUDENT_NAME, STUDENT_PASSWORD);
            assertNotNull(studentToken, "学生登录应返回 token");

            // 查询对所有角色开放 → 不是 401/403
            Message query = new Message(Command.STUDENT_QUERY, 1L);
            query.setToken(studentToken);
            String queryStatus =
                    client.exchange(query).getStatusCode();
            assertTrue(StatusCode.SUCCESS.equals(queryStatus)
                            || StatusCode.NOT_FOUND.equals(queryStatus),
                    "学籍查询对所有角色开放，实得 " + queryStatus);

            // 登记学籍 → 403
            StudentProfile profile = new StudentProfile("uuid-e2e-2", 2026,
                    EnrollmentStatus.ENROLLED);
            Message register = new Message(Command.STUDENT_REGISTER, profile);
            register.setToken(studentToken);
            assertEquals(StatusCode.FORBIDDEN,
                    client.exchange(register).getStatusCode(),
                    "学生登记学籍应被拒 403");

            // 改学籍状态 → 403
            StudentProfile statusChange = new StudentProfile();
            statusChange.setId(1L);
            statusChange.setStatus(EnrollmentStatus.WITHDRAWN);
            Message change = new Message(Command.STUDENT_CHANGE_STATUS,
                    statusChange);
            change.setToken(studentToken);
            assertEquals(StatusCode.FORBIDDEN,
                    client.exchange(change).getStatusCode(),
                    "学生改学籍状态应被拒 403");
        }
    }

    /**
     * 测试客户端：封装「连接 + 按协议建对象流 + 收发 + 挑战应答登录」。
     */
    private static final class TestClient implements Closeable {

        /** 探测学籍主键时的扫描上限。 */
        private static final long PROBE_MAX_ID = 50L;

        /** 底层连接。 */
        private final Socket m_socket;

        /** 输出对象流（先创建）。 */
        private final ObjectOutputStream m_out;

        /** 输入对象流（后创建）。 */
        private final ObjectInputStream m_in;

        /**
         * 连接服务器并按协议初始化对象流。
         *
         * @param port 服务器端口
         * @throws IOException 连接或建流失败
         */
        TestClient(int port) throws IOException {
            m_socket = new Socket("127.0.0.1", port);
            m_socket.setSoTimeout(10000);
            // 与协议一致：先建输出流并 flush，再建输入流，避免两端互等流头
            m_out = new ObjectOutputStream(m_socket.getOutputStream());
            m_out.flush();
            m_in = new ObjectInputStream(m_socket.getInputStream());
        }

        /**
         * 发送一条请求并同步等待响应。
         *
         * @param request 请求
         * @return 响应
         * @throws IOException            通信失败
         * @throws ClassNotFoundException 响应反序列化失败
         */
        Message exchange(Message request) throws IOException,
                ClassNotFoundException {
            m_out.writeObject(request);
            m_out.flush();
            return (Message) m_in.readObject();
        }

        /**
         * 走完挑战-应答三步登录。
         *
         * @param username 用户名
         * @param password 明文密码
         * @return 会话 token；任一步失败返回 null
         * @throws IOException            通信失败
         * @throws ClassNotFoundException 响应反序列化失败
         */
        String login(String username, String password) throws IOException,
                ClassNotFoundException {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.m_user_name = username;

            Message challengeResponse = exchange(
                    new Message(Command.USER_LOGIN, loginRequest));
            if (!StatusCode.SUCCESS.equals(challengeResponse.getStatusCode())) {
                return null;
            }
            LoginChallenge challenge =
                    (LoginChallenge) challengeResponse.getData();

            String saltedHash =
                    Sha256Util.sha256Hex(challenge.m_salt + password);
            String proof = Sha256Util.sha256Hex(challenge.m_nonce + saltedHash);

            LoginVerify verify = new LoginVerify();
            verify.m_user_name = username;
            verify.m_proof = proof;

            Message tokenResponse = exchange(
                    new Message(Command.USER_LOGIN_VERIFY, verify));
            if (!StatusCode.SUCCESS.equals(tokenResponse.getStatusCode())) {
                return null;
            }
            return ((LoginResponse) tokenResponse.getData()).m_token;
        }

        /**
         * 以管理员会话注册一个账号。
         *
         * @param username   新账号登录名
         * @param password   新账号明文密码
         * @param role       角色显示名
         * @param adminToken 管理员会话 token
         * @return 注册响应
         * @throws IOException            通信失败
         * @throws ClassNotFoundException 响应反序列化失败
         */
        Message registerUser(String username, String password, String role,
                String adminToken) throws IOException,
                ClassNotFoundException {
            RegisterRequest body = new RegisterRequest();
            body.m_user_name = username;
            body.m_password = password;
            body.m_role = role;

            Message request = new Message(Command.USER_REGISTER, body);
            request.setToken(adminToken);
            return exchange(request);
        }

        /**
         * 按序探测学籍主键，定位属于指定用户 uuid 的那条记录。
         *
         * <p>主键由服务端自增分配、且不回传给客户端（双方持有的是不同对象副本），
         * 因此由客户端按序查询反查。
         *
         * @param token    会话 token
         * @param userUuid 目标用户 uuid
         * @return 学籍主键；未找到返回 -1
         * @throws IOException            通信失败
         * @throws ClassNotFoundException 响应反序列化失败
         */
        long findProfileId(String token, String userUuid) throws IOException,
                ClassNotFoundException {
            long candidate = 1L;
            while (candidate <= PROBE_MAX_ID) {
                Message query = new Message(Command.STUDENT_QUERY, candidate);
                query.setToken(token);
                Message response = exchange(query);
                if (StatusCode.SUCCESS.equals(response.getStatusCode())) {
                    StudentProfile found = (StudentProfile) response.getData();
                    if (userUuid.equals(found.getUserUuid())) {
                        return candidate;
                    }
                }
                candidate = candidate + 1L;
            }
            return -1L;
        }

        /**
         * 关闭连接。
         *
         * @throws IOException 关闭失败
         */
        @Override
        public void close() throws IOException {
            m_socket.close();
        }
    }
}
