package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
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
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.util.List;

/**
 * 用户管理命令处理器：把 USER_LOGIN / USER_LOGIN_VERIFY / USER_REGISTER / USER_LOGOUT 接到
 * {@link AuthService}。
 *
 * <p>
 * 负责 Message.data 反序列化 → 调业务方法 → 组装响应 Message 并经 sender 发送。会话令牌经
 * {@code LoginResponse} 在登录成功时一次性分发，客户端 之后把 token 放回
 * {@code Message.token}；身份权威在 SessionManager。 注册等受限命令按会话真实角色鉴权（401 / 403）。
 */
public class AuthServiceHandler implements MessageHandler {

    /** 认证业务服务。 */
    private final AuthService m_auth;

    /** 用户管理服务（管理轨：查询/编辑/启停/注销）。 */
    private final UserAdminService m_admin;

    /**
     * 构造处理器（认证与用户管理）。
     *
     * @param auth 认证服务
     * @param admin 用户管理服务
     */
    public AuthServiceHandler(AuthService auth, UserAdminService admin) {
        if (auth == null || admin == null) {
            throw new IllegalArgumentException("auth and admin must not be null");
        }
        this.m_auth = auth;
        this.m_admin = admin;
    }

    /**
     * 处理一条用户管理请求：按命令码分派到对应业务方法。
     *
     * @param request 请求消息
     * @param sender 响应发送器
     */
    @Override
    public void handle(Message request, MessageSender sender) {
        try {
            switch (request.getCommand()) {
            case Command.USER_LOGIN:
                loginChallengeHandler(request, sender);
                break;
            case Command.USER_LOGIN_VERIFY:
                loginVerifyHandler(request, sender);
                break;
            case Command.USER_REGISTER:
                registerHandler(request, sender);
                break;
            case Command.USER_LOGOUT:
                handleLogout(request, sender);
                break;
            case Command.USER_UNREGISTER:
                unregisterHandler(request, sender);
                break;
            case Command.USER_LIST:
                listHandler(request, sender);
                break;
            case Command.USER_UPDATE:
                updateHandler(request, sender);
                break;
            case Command.USER_TOGGLE_ENABLED:
                toggleEnabledHandler(request, sender);
                break;
            case Command.USER_CHANGE_PASSWORD:
                changePasswordHandler(request, sender);
                break;
            case Command.USER_BATCH_REGISTER:
                batchRegisterHandler(request, sender);
                break;
            case Command.USER_BATCH_UNREGISTER:
                batchUnregisterHandler(request, sender);
                break;
            default:
                sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            }
        } catch (RuntimeException e) {
            // 契约约定不向连接层抛出；未知异常统一回 500
            sendError(sender, request.getCommand(), StatusCode.INTERNAL_ERROR);
        }
    }

    /** 登录第①步：回 LoginChallenge{salt, nonce}。 */
    private void loginChallengeHandler(Message request, MessageSender sender) {
        if (!(request.getData() instanceof LoginRequest)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        LoginRequest req = (LoginRequest) request.getData();
        LoginChallenge challenge = m_auth.loginChallenge(req.m_user_name);
        sendOk(sender, request.getCommand(), challenge);
    }

    /** 登录第③步：校验 proof，通过则经 LoginResponse 一次性分发 token 并回真实角色。 */
    private void loginVerifyHandler(Message request, MessageSender sender) {
        if (!(request.getData() instanceof LoginVerify)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        LoginVerify verify = (LoginVerify) request.getData();
        String token = m_auth.loginVerify(verify.m_user_name, verify.m_proof);
        if (token == null) {
            sendError(sender, request.getCommand(), StatusCode.UNAUTHORIZED);
            return;
        }
        if (!m_auth.isEnabled(verify.m_user_name)) {// 禁用账号：撤销刚签发的 token，回 P102
            m_auth.logout(token);
            sendError(sender, request.getCommand(), StatusCode.USER_DISABLED);
            return;
        }
        LoginResponse result = new LoginResponse();
        result.m_token = token;// 唯一一次分发：token 交给客户端本地缓存
        result.m_session = m_auth.validateToken(token);// 同一份会话记录，客户端缓存后即可查询身份
        Message response = new Message(request.getCommand(), result);
        response.setStatusCode(StatusCode.SUCCESS);
        sender.send(response);
    }

    /** 注册（仅管理员）：未登录 401，无 USER_MANAGE 能力 403，重复 400。 */
    private void registerHandler(Message request, MessageSender sender) {
        if (requireCapability(request, sender, Capability.USER_MANAGE) == null) {
            return;
        }
        if (!(request.getData() instanceof RegisterRequest)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        RegisterRequest req = (RegisterRequest) request.getData();
        try {
            m_auth.register(req.m_user_name, req.m_password, req.m_role);
        } catch (IllegalStateException e) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        sendOk(sender, request.getCommand(), null);
    }

    /** 登出（需有效会话）：按 Message.token 使会话失效后回 SUCCESS。 */
    private void handleLogout(Message request, MessageSender sender) {
        if (requireToken(request, sender) == null) {
            return;
        }
        m_auth.logout(request.getToken());
        sendOk(sender, request.getCommand(), null);
    }

    /** 校验会话 token：有效返回会话记录，否则发 401 返回 null。 */
    private SessionEntry requireToken(Message request, MessageSender sender) {
        String token = request.getToken();
        if (token == null) {
            sendError(sender, request.getCommand(), StatusCode.UNAUTHORIZED);
            return null;
        }
        SessionEntry entry = m_auth.validateToken(token);
        if (entry == null) {
            sendError(sender, request.getCommand(), StatusCode.UNAUTHORIZED);
        }
        return entry;
    }

    /** 校验会话与能力：未登录 401、无该能力 403，均返回 null。 */
    private SessionEntry requireCapability(Message request, MessageSender sender,
            Capability capability) {
        SessionEntry entry = requireToken(request, sender);
        if (entry == null) {
            return null;
        }
        Role role = Role.fromDisplayName(entry.getRole());
        if (!Permissions.can(role, capability)) {
            sendError(sender, request.getCommand(), StatusCode.FORBIDDEN);
            return null;
        }
        return entry;
    }

    /** 注销账户（仅管理员）：撤销各模块档案后删除账户。 */
    private void unregisterHandler(Message request, MessageSender sender) {
        if (requireCapability(request, sender, Capability.USER_MANAGE) == null) {
            return;
        }
        if (!(request.getData() instanceof UserRefRequest)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        UserRefRequest payload = (UserRefRequest) request.getData();
        if (!m_admin.unregister(payload.getUserName())) {
            sendError(sender, request.getCommand(), StatusCode.NOT_FOUND);
            return;
        }
        sendOk(sender, request.getCommand(), null);
    }

    /** 分页查询用户（仅管理员）：载荷缺省表示查询全部。 */
    private void listHandler(Message request, MessageSender sender) {
        if (requireCapability(request, sender, Capability.USER_MANAGE) == null) {
            return;
        }
        UserQuery query = request.getData() instanceof UserQuery ? (UserQuery) request.getData()
                : null;
        sendOk(sender, request.getCommand(), m_admin.listUsers(query));
    }

    /** 编辑用户姓名（仅管理员，不改角色）。 */
    private void updateHandler(Message request, MessageSender sender) {
        if (requireCapability(request, sender, Capability.USER_MANAGE) == null) {
            return;
        }
        if (!(request.getData() instanceof UserUpdateRequest)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        UserUpdateRequest payload = (UserUpdateRequest) request.getData();
        if (payload.getUserName() == null || payload.getUserName().trim().length() == 0) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        if (!m_admin.updateUser(payload.getUserName(), payload.getDisplayName())) {
            sendError(sender, request.getCommand(), StatusCode.NOT_FOUND);
            return;
        }
        sendOk(sender, request.getCommand(), null);
    }

    /** 启用/禁用用户（仅管理员）。 */
    private void toggleEnabledHandler(Message request, MessageSender sender) {
        if (requireCapability(request, sender, Capability.USER_MANAGE) == null) {
            return;
        }
        if (!(request.getData() instanceof UserEnabledRequest)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        UserEnabledRequest payload = (UserEnabledRequest) request.getData();
        if (!m_admin.setEnabled(payload.getUserName(), payload.isEnabled())) {
            sendError(sender, request.getCommand(), StatusCode.NOT_FOUND);
            return;
        }
        sendOk(sender, request.getCommand(), null);
    }

    /** 修改密码：本人改密用 proof 校验旧密码；改他人密码需 USER_MANAGE（管理员重置）。 */
    private void changePasswordHandler(Message request, MessageSender sender) {
        SessionEntry entry = requireToken(request, sender);
        if (entry == null) {
            return;
        }
        if (!(request.getData() instanceof ChangePasswordRequest)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        ChangePasswordRequest payload = (ChangePasswordRequest) request.getData();
        String target = payload.getUserName() == null || payload.getUserName().trim().length() == 0
                ? entry.getUsername()
                : payload.getUserName().trim();
        boolean self = target.equals(entry.getUsername());
        if (!self && !Permissions.can(Role.fromDisplayName(entry.getRole()),
                Capability.USER_MANAGE)) {
            sendError(sender, request.getCommand(), StatusCode.FORBIDDEN);
            return;
        }
        boolean changed = m_auth.changePassword(target, self ? payload.getProof() : null,
                payload.getNewSalt(), payload.getNewHash());
        if (!changed) {
            sendError(sender, request.getCommand(),
                    self ? StatusCode.WRONG_PASSWORD : StatusCode.NOT_FOUND);
            return;
        }
        sendOk(sender, request.getCommand(), null);
    }

    /** 批量注册（命令 103，仅管理员）：载荷为 {@code List<RegisterRequest>}。 */
    @SuppressWarnings("unchecked")
    private void batchRegisterHandler(Message request, MessageSender sender) {
        if (requireCapability(request, sender, Capability.USER_MANAGE) == null) {
            return;
        }
        if (!(request.getData() instanceof List)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        sendOk(sender, request.getCommand(),
                m_auth.registerAll((List<RegisterRequest>) request.getData()));
    }

    /** 批量注销（命令 105，仅管理员）：载荷为 {@code List<String>} 登录名。 */
    @SuppressWarnings("unchecked")
    private void batchUnregisterHandler(Message request, MessageSender sender) {
        if (requireCapability(request, sender, Capability.USER_MANAGE) == null) {
            return;
        }
        if (!(request.getData() instanceof List)) {
            sendError(sender, request.getCommand(), StatusCode.BAD_REQUEST);
            return;
        }
        sendOk(sender, request.getCommand(),
                m_admin.unregisterAll((List<String>) request.getData()));
    }

    /** 发送成功响应。 */
    private void sendOk(MessageSender sender, int command, Object data) {
        Message response = new Message(command, data);
        response.setStatusCode(StatusCode.SUCCESS);
        sender.send(response);
    }

    /** 发送错误响应。 */
    private void sendError(MessageSender sender, int command, String code) {
        Message response = new Message(command, null);
        response.setStatusCode(code);
        sender.send(response);
    }
}