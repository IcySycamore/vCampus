package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.ChangePasswordRequest;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.dto.UserEnabledRequest;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.dto.UserRefRequest;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.entity.User;
import edu.seu.vcampus.common.util.Sha256Util;

import java.util.List;

/**
 * 用户管理客户端 API：界面只调这里的方法，不认识 {@code Message}/{@code Command}（见 ADR-0009）。
 *
 * <p>
 * 三类方法：<b>我的轨</b>（{@link #login}、{@link #logout}、{@link #changePassword}、
 * {@link #currentSession}）不带身份参数；<b>管理轨</b>（{@link #listUsers}、{@link #updateUser}、
 * {@link #toggleUserEnabled}、{@link #unregister}、{@link #register}）显式传目标。
 *
 * <p>
 * 全部方法<b>同步阻塞</b>：调用即发请求，返回即结果，失败抛非受检 {@link ApiException} （文案取
 * {@link ApiErrors}）。界面请用 {@code UiTasks.run(...)} 调用，不要直接写在事件线程里。
 *
 * <p>
 * 登录走挑战-应答：① 请求挑战（salt/nonce）→ ② 本地算
 * {@code proof = sha256(nonce + sha256(salt + password))} → ③ 提交验证， 成功后 token 与
 * {@link SessionEntry} 只缓存在内存，连接断开即丢弃。
 */
public class UserService implements ConnectionListener {

    /** 消息分发器（发送 + 按命令码等待响应）。 */
    private final ClientMessageDispatcher m_dispatcher;

    /** 内存会话：token + 服务端会话记录。 */
    private final ClientSession m_session = new ClientSession();

    /** 请求超时，毫秒。 */
    private final long m_timeout_millis;

    /** 客户端新盐的随机源。 */
    private final RandomGen m_random = new RandomGen();

    /**
     * 构造用户管理客户端 API（使用默认请求超时）。
     *
     * @param dispatcher 消息分发器
     */
    public UserService(ClientMessageDispatcher dispatcher) {
        this(dispatcher, NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * 构造用户管理客户端 API 并指定请求超时。
     *
     * @param dispatcher 消息分发器
     * @param timeoutMillis 请求超时，毫秒
     * @throws IllegalArgumentException 分发器为 null
     */
    public UserService(ClientMessageDispatcher dispatcher, long timeoutMillis) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.m_dispatcher = dispatcher;
        this.m_timeout_millis = timeoutMillis;
    }

    /**
     * 登录：完成挑战-应答并把 token 与服务端会话记录写入内存会话。
     *
     * @param userName 登录名
     * @param role 登录页选定的身份；服务器会校验其与账号真实角色是否一致
     * @param password 明文密码
     * @throws ApiException 服务器拒绝（状态码见异常）或本地超时/断线
     */
    public void login(String userName, Role role, String password) {
        LoginChallenge challenge = requestChallenge(userName, role);
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = userName;
        verify.m_proof = computeProof(challenge, password);
        Object data = call(Command.USER_LOGIN_VERIFY, verify).getData();
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
            call(Command.USER_LOGOUT, null);
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
        LoginChallenge challenge = requestChallenge(entry.getUsername(),
                Role.fromDisplayName(entry.getRole()));
        String proof = computeProof(challenge, oldPassword);
        String newSalt = m_random.randomHex(16);
        String newHash = Sha256Util.sha256Hex(newSalt + newPassword);
        call(Command.USER_CHANGE_PASSWORD,
                new ChangePasswordRequest(null, proof, newSalt, newHash));
    }

    /**
     * 分页查询用户（管理轨，需 {@code USER_MANAGE}）。
     *
     * @param query 查询条件；null 表示全部
     * @return 分页结果
     * @throws ApiException 无权限或本地失败
     */
    public PageResponse<User> listUsers(UserQuery query) {
        return userPage(call(Command.USER_LIST, query).getData());
    }

    /**
     * 编辑用户姓名（管理轨，需 {@code USER_MANAGE}；不改角色）。
     *
     * @param request 编辑请求
     * @throws ApiException 无权限、目标不存在或本地失败
     */
    public void updateUser(UserUpdateRequest request) {
        call(Command.USER_UPDATE, request);
    }

    /**
     * 启用/禁用用户（管理轨，需 {@code USER_MANAGE}）。
     *
     * @param userName 目标登录名
     * @param enabled 目标状态
     * @throws ApiException 无权限、目标不存在或本地失败
     */
    public void toggleUserEnabled(String userName, boolean enabled) {
        call(Command.USER_TOGGLE_ENABLED, new UserEnabledRequest(userName, enabled));
    }

    /**
     * 新建账户（管理轨，需 {@code USER_MANAGE}）；服务器会同步建立该账号的各模块档案。
     *
     * @param userName 登录名
     * @param role 角色
     * @param password 明文密码
     * @throws ApiException 重名、无权限或本地失败
     */
    public void register(String userName, Role role, String password) {
        register(userName, null, role, password);
    }

    /**
     * 新建账户（管理轨，需 {@code USER_MANAGE}）：姓名随注册一并提交，避免多一次往返。
     *
     * @param userName 登录名
     * @param displayName 姓名；null 或空表示取登录名
     * @param role 角色
     * @param password 明文密码
     * @throws ApiException 重名、无权限或本地失败
     */
    public void register(String userName, String displayName, Role role, String password) {
        RegisterRequest request = new RegisterRequest();
        request.m_user_name = userName;
        request.m_display_name = displayName;
        request.m_role = role == null ? null : role.getDisplayName();
        request.m_password = password;
        call(Command.USER_REGISTER, request);
    }

    /**
     * 注销账户（管理轨，需 {@code USER_MANAGE}）；服务器同时撤销该账号的各模块档案。
     *
     * @param userName 目标登录名
     * @throws ApiException 无权限、目标不存在或本地失败
     */
    public void unregister(String userName) {
        call(Command.USER_UNREGISTER, new UserRefRequest(userName));
    }

    /**
     * 批量注册（管理轨，需 {@code USER_MANAGE}）：用于从文件导入账号。
     *
     * <p>
     * 逐条建号、逐条记账：重复登录名等失败原因随结果返回，不影响其它账号入库。
     *
     * @param requests 注册请求列表
     * @return 批量结果（成功数 + 失败明细）
     * @throws ApiException 无权限或本地失败
     */
    public BatchResult batchRegister(List<RegisterRequest> requests) {
        return batchResult(call(Command.USER_BATCH_REGISTER, requests).getData());
    }

    /**
     * 批量注销（管理轨，需 {@code USER_MANAGE}）。
     *
     * @param userNames 登录名列表
     * @return 批量结果（成功数 + 失败明细）
     * @throws ApiException 无权限或本地失败
     */
    public BatchResult batchUnregister(List<String> userNames) {
        return batchResult(call(Command.USER_BATCH_UNREGISTER, userNames).getData());
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

    /** @return 服务端下发的会话记录（含 uuid/登录名/角色）；未登录返回 null */
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

    private LoginChallenge requestChallenge(String userName, Role role) {
        LoginRequest request = new LoginRequest();
        request.m_user_name = userName;
        request.m_role = role == null ? null : role.getDisplayName();
        Object data = call(Command.USER_LOGIN, request).getData();
        if (!(data instanceof LoginChallenge)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (LoginChallenge) data;
    }

    /** 统一的「发请求 + 判状态码」：非受检化受检异常，本地失败归一为 Lxxx。 */
    private Message call(int command, Object payload) {
        Message request = new Message(command, payload);
        request.setToken(m_session.getToken());
        try {
            return requireSuccess(m_dispatcher.request(request, m_timeout_millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        }
    }

    private Message requireSuccess(Message response) {
        if (response == null) {
            throw new ApiException(ApiErrors.LOCAL_TIMEOUT);
        }
        if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
            throw new ApiException(response.getStatusCode());
        }
        return response;
    }

    private String computeProof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }

    @SuppressWarnings("unchecked")
    private PageResponse<User> userPage(Object data) {
        if (!(data instanceof PageResponse)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (PageResponse<User>) data;
    }

    private BatchResult batchResult(Object data) {
        if (!(data instanceof BatchResult)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (BatchResult) data;
    }
}
