package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
//import edu.seu.vcampus.server.dispatch.MessageDispatcher;

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

    /**
     * 构造处理器。
     *
     * @param auth 认证服务
     */
    public AuthServiceHandler(AuthService auth) {
        this.m_auth = auth;
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
        LoginResponse result = new LoginResponse();
        result.m_token = token;// 唯一一次分发：token 交给客户端本地缓存
        result.m_session = m_auth.validateToken(token);// 同一份会话记录，客户端缓存后即可查询身份
        Message response = new Message(request.getCommand(), result);
        response.setStatusCode(StatusCode.SUCCESS);
        sender.send(response);
    }

    /** 注册（仅管理员）：未登录 401，非管理员 403，重复 400。 */
    private void registerHandler(Message request, MessageSender sender) {
        if (requireAdmin(request, sender) == null) {
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

    /** 校验管理员会话：未登录 401，非管理员 403，均返回 null。 */
    private SessionEntry requireAdmin(Message request, MessageSender sender) {
        SessionEntry entry = requireToken(request, sender);
        if (entry == null) {
            return null;
        }
        if (!Role.ADMIN.getDisplayName().equals(entry.getRole())) {
            sendError(sender, request.getCommand(), StatusCode.FORBIDDEN);
            return null;
        }
        return entry;
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