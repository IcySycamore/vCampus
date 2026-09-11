package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.util.Sha256Util;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** 一次连接上的两步登录交换，兼容认证响应尚未回传 uid 的现有协议。 */
final class LoginExchange {
    private final BlockingQueue<Object> replies = new LinkedBlockingQueue<Object>(4);
    private volatile int expectedCommand;

    LoginResponse authenticate(ClientSocket client, String username, char[] password,
            String selectedRole) throws IOException {
        LoginRequest request = new LoginRequest();
        request.m_user_name = username;
        request.m_role = selectedRole;
        Object first = exchange(client, Command.USER_LOGIN, request);
        if (!(first instanceof LoginChallenge)) {
            throw new IOException("登录响应格式不正确");
        }
        LoginChallenge challenge = (LoginChallenge) first;
        if (empty(challenge.m_salt) || empty(challenge.m_nonce)) {
            throw new IOException("登录挑战无效");
        }
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = username;
        verify.m_proof = Sha256Util.sha256Hex(challenge.m_nonce
                + Sha256Util.sha256Hex(challenge.m_salt + new String(password)));
        Object second = exchange(client, Command.USER_LOGIN_VERIFY, verify);
        if (!(second instanceof LoginResponse)) {
            throw new IOException("登录响应格式不正确");
        }
        LoginResponse result = (LoginResponse) second;
        if (empty(result.m_token) || empty(result.m_role)) {
            throw new IOException("登录响应缺少会话或身份信息");
        }
        return result;
    }

    void receive(Message message) {
        if (message.getCommand() == expectedCommand) {
            replies.offer(message);
        }
    }

    void disconnected(Exception cause) {
        replies.clear();
        replies.offer(new IOException("连接已断开，请重新登录", cause));
    }

    private Object exchange(ClientSocket client, int command, Object data) throws IOException {
        expectedCommand = command;
        client.send(new Message(command, data));
        try {
            Object received = replies.poll(10, TimeUnit.SECONDS);
            if (received == null) {
                throw new IOException("登录响应超时，请稍后重试");
            }
            if (received instanceof IOException) {
                throw (IOException) received;
            }
            Message response = (Message) received;
            if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
                throw new IOException(StatusCode.UNAUTHORIZED.equals(response.getStatusCode())
                        ? "用户名或密码不正确" : "登录失败，请确认服务器认证服务已就绪");
            }
            return response.getData();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("登录已取消", exception);
        } finally {
            expectedCommand = 0;
        }
    }

    private boolean empty(String value) {
        return value == null || value.trim().length() == 0;
    }
}
