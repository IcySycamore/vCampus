package edu.seu.vcampus.server;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.util.Sha256Util;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 端到端测试的共用夹具：{@link TestClient} 封装「连接 + 按协议建对象流 + 收发 + 挑战应答登录」。
 *
 * <p>
 * 从 {@code ServerEndToEndTest} 抽出（原文件破 500 行上限）。继承本类的测试可直接使用 {@code TestClient}， 无需再各自重写一套 socket
 * 收发逻辑。
 */
abstract class ServerEndToEndSupport {

    /** 构造器：仅供同包子类继承使用（本类只作静态夹具的宿主）。 */
    ServerEndToEndSupport() {
    }

    /**
     * 测试客户端：封装「连接 + 按协议建对象流 + 收发 + 挑战应答登录」。
     */
    static final class TestClient implements Closeable {

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
        Message exchange(Message request) throws IOException, ClassNotFoundException {
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
        String login(String username, String password) throws IOException, ClassNotFoundException {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.m_user_name = username;

            Message challengeResponse = exchange(new Message(Command.USER_LOGIN, loginRequest));
            if (!StatusCode.SUCCESS.equals(challengeResponse.getStatusCode())) {
                return null;
            }
            LoginChallenge challenge = (LoginChallenge) challengeResponse.getData();

            String saltedHash = Sha256Util.sha256Hex(challenge.m_salt + password);
            String proof = Sha256Util.sha256Hex(challenge.m_nonce + saltedHash);

            LoginVerify verify = new LoginVerify();
            verify.m_user_name = username;
            verify.m_proof = proof;

            Message tokenResponse = exchange(new Message(Command.USER_LOGIN_VERIFY, verify));
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
        Message registerUser(String username, String password, String role, String adminToken)
                throws IOException, ClassNotFoundException {
            return registerUser(username, null, password, role, adminToken);
        }

        /**
         * 注册账号（含姓名）。
         *
         * @param username    新账号登录名
         * @param displayName 姓名（可为 null，服务端不采集时界面回落登录名）
         * @param password    新账号明文密码
         * @param role        角色显示名
         * @param adminToken  管理员会话 token
         * @return 注册响应
         * @throws IOException            通信失败
         * @throws ClassNotFoundException 响应反序列化失败
         */
        Message registerUser(String username, String displayName, String password, String role,
                String adminToken) throws IOException, ClassNotFoundException {
            RegisterRequest body = new RegisterRequest();
            body.m_user_name = username;
            body.m_display_name = displayName;
            body.m_password = password;
            body.m_role = role;

            Message request = new Message(Command.USER_REGISTER, body);
            request.setToken(adminToken);
            return exchange(request);
        }

        /**
         * 按序探测学籍主键，定位属于指定用户 uuid 的那条记录。
         *
         * <p>
         * 主键由服务端自增分配、且不回传给客户端（双方持有的是不同对象副本）， 因此由客户端按序查询反查。
         *
         * @param token    会话 token
         * @param userUuid 目标用户 uuid
         * @return 学籍主键；未找到返回 -1
         * @throws IOException            通信失败
         * @throws ClassNotFoundException 响应反序列化失败
         */
        long findProfileId(String token, String userUuid)
                throws IOException, ClassNotFoundException {
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
