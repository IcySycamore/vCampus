package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

/** 复用登录连接的客户端会话；token 仅保存在内存，身份失效后须重新登录。 */
public final class ClientSession implements UIUpdateHandler, Closeable {
    private final ClientSocket client;
    private final LoginExchange loginExchange = new LoginExchange();
    private volatile UIUpdateHandler handler;
    private String username;
    private String role;
    private String token;
    private boolean started;
    private boolean closed;
    private boolean disconnected;

    /**
     * 创建尚未连接的登录会话。
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public ClientSession(String host, int port) {
        client = new ClientSocket(host, port, this);
    }

    ClientSession(ClientSocket client) {
        this.client = client;
    }

    /**
     * 在后台线程执行一次挑战应答登录，失败后关闭此会话。
     * @param user 登录名，经验证后作为客户端显示身份
     * @param password 密码字符数组，方法结束时清零
     * @param selectedRole 界面选择，仅作请求提示，最终角色以服务器为准
     * @throws IOException 登录被拒绝、超时或连接失败
     */
    public void login(String user, char[] password, String selectedRole) throws IOException {
        try {
            synchronized (this) {
                if (started || closed) {
                    throw new IOException("请创建新的登录会话");
                }
                started = true;
            }
            if (user == null || user.trim().length() == 0
                    || password == null || password.length == 0) {
                throw new IOException("请输入用户 ID 和密码");
            }
            client.connect();
            LoginResponse result = loginExchange.authenticate(
                    client, user.trim(), password, selectedRole);
            synchronized (this) {
                if (closed || disconnected) {
                    throw new IOException("连接已断开，请重新登录");
                }
                username = user.trim();
                role = result.m_role;
                token = result.m_token;
            }
        } catch (IOException | RuntimeException exception) {
            try {
                close();
            } catch (IOException failure) {
                exception.addSuppressed(failure);
            }
            throw exception;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /** @return 已通过登录验证的用户名，未登录为 null */
    public synchronized String getUsername() {
        return username;
    }

    /** @return 服务器确认的角色，未登录为 null */
    public synchronized String getRole() {
        return role;
    }

    /** @return 是否仍持有有效的本地登录状态 */
    public synchronized boolean isAuthenticated() {
        return token != null && !closed && !disconnected;
    }

    /** @return 当前传输连接是否可用 */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * 在后台线程发送业务消息，统一覆盖 token 与发送者字段。
     * @param request 业务请求
     * @throws IOException 未登录、身份已过期或发送失败
     */
    public void send(Message request) throws IOException {
        synchronized (this) {
            if (!isAuthenticated()) {
                throw new IOException("登录已失效，请重新登录");
            }
            request.setToken(token);
            request.setSender(username);
        }
        try {
            client.send(request);
        } catch (IOException exception) {
            connectionClosed(exception);
            throw exception;
        }
    }

    /**
     * 绑定主窗口事件处理器，传 null 可解除绑定。
     * @param handler 主窗口处理器
     */
    public void setHandler(UIUpdateHandler handler) {
        this.handler = handler;
        if (handler != null && !isAuthenticated()) {
            handler.connectionClosed(new IOException("登录已失效，请重新登录"));
        }
    }

    @Override
    public void handleMessage(Message message) {
        if (message == null) {
            return;
        }
        int command = message.getCommand();
        if (command == Command.USER_LOGIN || command == Command.USER_LOGIN_VERIFY) {
            loginExchange.receive(message);
            return;
        }
        if (command == 1) {
            return;
        }
        if (StatusCode.UNAUTHORIZED.equals(message.getStatusCode())) {
            invalidate();
        }
        UIUpdateHandler current = handler;
        if (current != null) {
            current.handleMessage(message);
        }
    }

    @Override
    public void connectionClosed(Exception cause) {
        synchronized (this) {
            disconnected = true;
            invalidate();
        }
        loginExchange.disconnected(cause);
        UIUpdateHandler current = handler;
        if (current != null) {
            current.connectionClosed(cause);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            closed = true;
            invalidate();
        }
        handler = null;
        loginExchange.disconnected(null);
        client.close();
    }

    private synchronized void invalidate() {
        token = null;
        username = null;
        role = null;
    }
}
