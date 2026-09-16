package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.dto.ChangePasswordRequest;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.util.Sha256Util;

/**
 * 用户管理「我的轨」客户端 API：登录、登出、修改本人密码、会话查询。
 *
 * <p>
 * 我的轨的方法<b>不带身份参数</b>——身份由服务端从会话解析（见 ADR-0009 D7 附则）；针对他人账号的 操作在 {@link UserAdminService}（管理轨，需
 * {@code USER_MANAGE}），经 {@link #admin()} 获取。
 *
 * <p>
 * 全部方法<b>同步阻塞</b>，失败抛非受检 {@link ApiException}（文案取 {@code ApiErrors}）；界面请用 {@code UiTasks.run(...)}
 * 调用。登录走挑战-应答，token 与 {@link SessionEntry} 只缓存在内存， 连接断开即丢弃。
 */
public class UserService implements ConnectionListener {

    /** 内存会话：token + 服务端会话记录。 */
    private final ClientSession m_session = new ClientSession();

    /** 请求收发细节。 */
    private final UserRequests m_requests;

    /** 管理轨（与本人轨共用同一份会话）。 */
    private final UserAdminService m_admin;

    /** 客户端新盐的随机源。 */
    private final RandomGen m_random = new RandomGen();

    /**
     * 构造用户管理 API（使用默认请求超时）。
     *
     * @param dispatcher 消息分发器
     */
    public UserService(ClientMessageDispatcher dispatcher) {
        this(dispatcher, NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * 构造用户管理 API 并指定请求超时。
     *
     * @param dispatcher    消息分发器
     * @param timeoutMillis 请求超时，毫秒
     * @throws IllegalArgumentException 分发器为 null
     */
    public UserService(ClientMessageDispatcher dispatcher, long timeoutMillis) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.m_requests = new UserRequests(dispatcher, m_session, timeoutMillis);
        this.m_admin = new UserAdminService(m_requests, m_random);
    }

    /**
     * 管理轨 API（需 {@code USER_MANAGE}）：查询、编辑、启停、重置密码、注册、注销、批量。
     *
     * @return 管理轨 API
     */
    public UserAdminService admin() {
        return m_admin;
    }

    /**
     * 登录：完成挑战-应答并把 token 与服务端会话记录写入内存会话。
     *
     * @param userName 登录名
     * @param role     登录页选定的身份；服务器会校验其与账号真实角色是否一致
     * @param password 明文密码
     * @throws ApiException 服务器拒绝（状态码见异常）或本地超时/断线
     */
    public void login(String userName, Role role, String password) {
        LoginChallenge challenge = m_requests.requestChallenge(userName, role);
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = userName;
        verify.m_proof = UserRequests.computeProof(challenge, password);
        Object data = m_requests.call(Command.USER_LOGIN_VERIFY, verify).getData();
        if (!(data instanceof LoginResponse)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        LoginResponse result = (LoginResponse) data;
        m_session.cache(result.m_token, result.m_session);
    }

    /**
     * 登出：通知服务器使会话失效，并清空内存会话。
     *
     * @throws ApiException 本地超时/断线（内存会话仍会被清空）
     */
    public void logout() {
        try {
            m_requests.call(Command.USER_LOGOUT, null);
        } finally {
            m_session.clear();// 无论服务器是否应答，本地会话都必须丢弃
        }
    }

    /**
     * 修改本人密码：先用挑战-应答校验旧密码，再用客户端新盐提交新哈希（明文不上线）。
     *
     * @param oldPassword 旧密码（明文，仅用于本地计算 proof）
     * @param newPassword 新密码（明文，仅用于本地计算哈希）
     * @throws ApiException 未登录、旧密码错误或本地失败
     */
    public void changePassword(String oldPassword, String newPassword) {
        SessionEntry entry = m_session.getEntry();
        if (entry == null) {
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        LoginChallenge challenge = m_requests.requestChallenge(entry.getUsername(),
                Role.fromDisplayName(entry.getRole()));
        String proof = UserRequests.computeProof(challenge, oldPassword);
        String newSalt = m_random.randomHex(16);
        String newHash = Sha256Util.sha256Hex(newSalt + newPassword);
        m_requests.call(Command.USER_CHANGE_PASSWORD,
                new ChangePasswordRequest(null, proof, newSalt, newHash));
    }

    /** @return 是否已登录（token 与会话记录齐备） */
    public boolean isLoggedIn() {
        return m_session.isLoggedIn();
    }

    /**
     * 内存会话（包内可见）：供同包测试预置登录态，不对外暴露。
     *
     * @return 内存会话
     */
    ClientSession session() {
        return m_session;
    }

    /** @return 服务端下发的会话记录（含 uuid/登录名/姓名/角色）；未登录返回 null */
    public SessionEntry currentSession() {
        return m_session.getEntry();
    }

    /** @return 当前会话令牌；未登录返回 null（供集成测试等直接发请求的场景） */
    public String currentToken() {
        return m_session.getToken();
    }

    /**
     * 连接已关闭：立即丢弃内存会话（token 不跨连接存活）。
     *
     * @param cause 异常关闭原因；正常关闭时为 null
     */
    @Override
    public void connectionClosed(Exception cause) {
        m_session.clear();
    }
}
