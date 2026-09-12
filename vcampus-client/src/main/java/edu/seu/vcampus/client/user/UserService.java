package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.dto.UserProfile;
import edu.seu.vcampus.common.util.Sha256Util;

import java.io.IOException;

/**
 * 用户管理客户端服务：登录、注册、登出、查个人档案。
 * 登录走挑战-应答；token 只留客户端内存，连接关闭即丢弃。
 */
public class UserService implements ConnectionListener {

    /** 消息分发器（发送 + 按命令码等待响应）。 */
    private final ClientMessageDispatcher dispatcher;

    /** 内存会话：由本服务组合持有（不落盘，随连接生命周期丢弃）。 */
    private final ClientSession session = new ClientSession();

    /** 请求超时，毫秒。 */
    private final long timeoutMillis;

    /**
     * 构造用户管理客户端服务。
     *
     * @param dispatcher 消息分发器
     */
    public UserService(ClientMessageDispatcher dispatcher) {
        this(dispatcher, NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * 构造用户管理客户端服务并指定请求超时。
     *
     * @param dispatcher 消息分发器
     * @param timeoutMillis 请求超时，毫秒
     */
    public UserService(ClientMessageDispatcher dispatcher, long timeoutMillis) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.dispatcher = dispatcher;
        this.timeoutMillis = timeoutMillis;
    }

    /** @return 当前内存会话（界面取角色/令牌的只读入口） */
    public ClientSession getSession() {
        return session;
    }

    /**
     * 连接已关闭：立即丢弃内存会话（token 不跨连接存活）。
     *
     * @param cause 异常关闭原因；正常关闭时为 null
     */
    @Override
    public void connectionClosed(Exception cause) {
        session.clear();
    }

    /**
     * 登录：完成挑战-应答并把 token 写入内存会话。
     *
     * @param userName 登录名
     * @param role 选定角色
     * @param password 明文密码
     */
    public void login(String userName, String role, String password)
            throws IOException, InterruptedException {
        LoginChallenge challenge = requestChallenge(userName, role);
        // proof = sha256(nonce + sha256(salt + password))，直接写在这里而不单开方法
        String proof = Sha256Util.sha256Hex(challenge.m_nonce
                + Sha256Util.sha256Hex(challenge.m_salt + password));
        LoginResponse result = submitProof(userName, proof);
        session.cache(result.m_token, result.m_session);
    }
    /**
     * 注册（需管理员会话；不采集姓名）。
     *
     * @param userName 登录名
     * @param role 角色
     * @param password 明文密码
     */
    public void register(String userName, String role, String password)
            throws IOException, InterruptedException {
        register(userName, role, password, null);
    }

    /**
     * 注册（需管理员会话）。
     *
     * @param userName 登录名
     * @param role 角色
     * @param password 明文密码
     * @param realName 真实姓名（管理员账号可传 null）
     */
    public void register(String userName, String role, String password, String realName)
            throws IOException, InterruptedException {
        RegisterRequest request = new RegisterRequest();
        request.m_user_name = userName;
        request.m_role = role;
        request.m_password = password;
        request.m_real_name = realName;
        Message message = new Message(Command.USER_REGISTER, request);
        message.setToken(session.getToken());
        requireSuccess(dispatcher.request(message, timeoutMillis));
    }

    /**
     * 登出：通知服务器使会话失效，并清空内存会话。
     */
    public void logout() throws IOException, InterruptedException {
        Message message = new Message(Command.USER_LOGOUT, null);
        message.setToken(session.getToken());
        try {
            dispatcher.request(message, timeoutMillis);
        } finally {
            session.clear();
        }
    }

    /**
     * 查询当前登录者的个人档案（命令 109）。姓名已随登录会话下发，首屏显示不必调这里，
     * 管理员更正姓名后想刷新时再用。
     *
     * @return 个人档案
     */
    public UserProfile queryMyProfile() throws IOException, InterruptedException {
        Message message = new Message(Command.USER_PROFILE_QUERY, null);
        message.setToken(session.getToken());
        Message response = dispatcher.request(message, timeoutMillis);
        requireSuccess(response);
        if (!(response.getData() instanceof UserProfile)) {
            throw new AuthException(response.getStatusCode(), "档案载荷缺失");
        }
        return (UserProfile) response.getData();
    }

    /** @return 是否已登录 */
    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    private LoginChallenge requestChallenge(String userName, String role)
            throws IOException, InterruptedException {
        LoginRequest request = new LoginRequest();
        request.m_user_name = userName;
        request.m_role = role;
        Message response = dispatcher.request(new Message(Command.USER_LOGIN, request),
                timeoutMillis);
        requireSuccess(response);
        if (!(response.getData() instanceof LoginChallenge)) {
            throw new AuthException(response.getStatusCode(), "挑战载荷缺失");
        }
        return (LoginChallenge) response.getData();
    }

    private LoginResponse submitProof(String userName, String proof)
            throws IOException, InterruptedException {
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = userName;
        verify.m_proof = proof;
        Message response = dispatcher.request(new Message(Command.USER_LOGIN_VERIFY, verify),
                timeoutMillis);
        requireSuccess(response);
        if (!(response.getData() instanceof LoginResponse)) {
            throw new AuthException(response.getStatusCode(), "登录响应缺失");
        }
        return (LoginResponse) response.getData();
    }

    private void requireSuccess(Message response) {
        if (response == null) {
            throw new AuthException(null, "服务器无响应（超时）");
        }
        String code = response.getStatusCode();
        if (!StatusCode.SUCCESS.equals(code)) {
            throw new AuthException(code, "服务器拒绝：" + code);
        }
    }
}
