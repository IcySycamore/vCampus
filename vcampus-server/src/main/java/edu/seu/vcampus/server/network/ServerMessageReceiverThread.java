package edu.seu.vcampus.server.network;

import edu.seu.vcampus.server.util.ServerLog;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;
import edu.seu.vcampus.server.user.SessionManager;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * 单个客户端连接的长连接会话处理线程。
 */
public class ServerMessageReceiverThread implements Runnable {

    /** 全局共享的消息分发器。 */
    private static final ServerMessageDispatcher DISPATCHER = new ServerMessageDispatcher();
    /** 客户端连接。 */
    private final Socket socket;
    /** 认证模块的会话管理器。 */
    private final SessionManager sessionManager;

    /** 连接编号：同一连接的多行日志靠它串起来。 */
    private final String connectionId;

    /** 本连接认证后的会话；首个带有效 token 的请求到来时填上。 */
    private volatile SessionEntry session;

    /** 未显式指定连接编号时的自增源。 */
    private static final java.util.concurrent.atomic.AtomicInteger SEQ = new java.util.concurrent.atomic.AtomicInteger();
    /** 上层已建好的消息流；为 null 时由本线程在 run 中自行创建。 */
    private final MessageStream providedStream;

    /** 连接运行状态。 */
    private volatile boolean running = true;

    /**
     * 创建客户端处理线程（由本线程负责创建消息流）。
     *
     * @param socket         已建立的客户端连接
     * @param sessionManager 认证模块的会话管理器
     */
    public ServerMessageReceiverThread(Socket socket, SessionManager sessionManager) {
        if (socket == null) {
            throw new IllegalArgumentException("socket must not be null");
        }
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionManager must not be null");
        }
        this.socket = socket;
        this.sessionManager = sessionManager;
        this.providedStream = null;
        this.connectionId = "conn-" + SEQ.incrementAndGet();
    }

    /**
     * 创建客户端处理线程（复用上层已建好的消息流）。
     *
     * <p>
     * 对象流的流头只能读一次：{@code ServerSocketListener.accept()} 在创建 MessageStream 时已完成握手读取，若此处再建一个
     * MessageStream 会导致阻塞或 读到脏数据。因此握手由监听端负责时，用本构造函数把已建好的流交给线程复用。
     *
     * @param stream         已初始化（含握手）的消息流
     * @param sessionManager 认证模块的会话管理器
     */
    public ServerMessageReceiverThread(MessageStream stream, SessionManager sessionManager) {
        this(stream, sessionManager, "conn-" + SEQ.incrementAndGet());
    }

    /**
     * 创建客户端处理线程（复用上层已建好的消息流，并指定连接编号）。
     *
     * <p>
     * 连接编号由监听端分配并跨两处使用：监听端回显「连接建立」，本线程回显「会话建立」与「连接断开」， 同一连接的日志靠它串起来。
     *
     * @param stream         已初始化（含握手）的消息流
     * @param sessionManager 认证模块的会话管理器
     * @param connectionId   连接编号
     */
    public ServerMessageReceiverThread(MessageStream stream, SessionManager sessionManager,
            String connectionId) {
        if (stream == null) {
            throw new IllegalArgumentException("stream must not be null");
        }
        if (stream.getSocket() == null) {
            throw new IllegalArgumentException("stream must wrap an established socket");
        }
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionManager must not be null");
        }
        if (connectionId == null || connectionId.trim().length() == 0) {
            throw new IllegalArgumentException("connectionId must not be blank");
        }
        this.socket = stream.getSocket();
        this.sessionManager = sessionManager;
        this.providedStream = stream;
        this.connectionId = connectionId;
    }

    /**
     * 获取全局消息分发器，供服务启动阶段注册各模块处理器。
     *
     * @return 全局唯一的消息分发器
     */
    public static ServerMessageDispatcher getDispatcher() {
        return DISPATCHER;
    }

    /**
     * 执行客户端长连接会话循环。
     */
    @Override
    public void run() {
        MessageStream messageStream = null;

        try {
            socket.setSoTimeout(15000);
            messageStream = providedStream != null ? providedStream : new MessageStream(socket);

            final ServerMessageSender messageSender = new ServerMessageSender(messageStream);

            while (running && !socket.isClosed() && socket.isConnected()) {
                Message request;

                try {
                    request = messageStream.recvMessage();
                } catch (SocketTimeoutException e) {
                    ServerLog.warning("连接 " + describeConnection() + " 静默 15 秒无消息，关闭：来自 "
                            + socket.getRemoteSocketAddress());
                    break;
                } catch (EOFException e) {
                    ServerLog.info("连接 " + describeConnection() + " 已断开：来自 "
                            + socket.getRemoteSocketAddress());
                    break;
                } catch (SocketException e) {
                    ServerLog.info("连接 " + describeConnection() + " Socket 已关闭：来自 "
                            + socket.getRemoteSocketAddress());
                    break;
                } catch (ClassNotFoundException e) {
                    ServerLog.error("连接 " + describeConnection() + " 消息反序列化失败", e);
                    break;
                }

                if (request == null) {
                    break;
                }
                // 标上来源连接：登录处理器据此区分「同一客户端的密码复核」与「另一个客户端抢登录」，
                // 也顺便让同一条连接的多行日志能串起来。
                request.setConnectionId(connectionId);

                if (isHeartbeat(request)) {
                    sendHeartbeatAck(messageSender);
                    continue;
                }

                if (requiresAuthentication(request.getCommand())) {
                    SessionEntry entry = sessionManager.validate(request.getToken());
                    if (entry == null) {
                        sendUnauthorized(messageSender, request);
                        continue;
                    }
                    noteIdentity(entry);
                }

                // 业务处理丢线程池：读循环要立刻回到 recvMessage，否则一个慢 handler
                // 会把后续消息（包括心跳）堵在 TCP 缓冲区里，客户端误判为断连并重连。
                final Message task = request;
                ServerBusinessExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DISPATCHER.dispatch(task, messageSender);
                        } catch (RuntimeException e) {
                            ServerLog.error("连接 " + connectionId + " 消息分发失败", e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            ServerLog.error("连接 " + describeConnection() + " 连接异常", e);
        } finally {
            closeSession(messageStream);
        }
    }

    /**
     * 连接日志里的身份描述：连接编号 + 会话 uuid（未认证时只有编号）。
     *
     * @return 形如 {@code conn-3} 或 {@code conn-3（会话 uuid xxx）}
     */
    private String describeConnection() {
        SessionEntry current = session;
        if (current == null || current.getUuid() == null) {
            return connectionId;
        }
        return connectionId + "（会话 uuid " + current.getUuid() + "）";
    }

    /**
     * 记下本连接的登录身份，只在首次认证时回显一行。
     *
     * @param entry 会话记录
     */
    private void noteIdentity(SessionEntry entry) {
        SessionEntry previous = session;
        session = entry;
        if (previous == null && entry != null) {
            ServerLog.info("连接 " + connectionId + " 会话建立：账户 uuid " + entry.getUuid());
        }
    }

    /**
     * 停止当前连接的处理循环。
     */
    public void stop() {
        running = false;
    }

    /**
     * 判断请求是否为心跳消息。
     *
     * @param request 请求消息
     * @return 是否为心跳
     */
    private boolean isHeartbeat(Message request) {
        return request.getCommand() == Command.HEARTBEAT;
    }

    /**
     * 回复心跳确认消息。
     *
     * @param sender 当前连接的消息发送器
     */
    private void sendHeartbeatAck(ServerMessageSender sender) {
        Message response = new Message(Command.HEARTBEAT, "HEARTBEAT_ACK");
        response.setStatusCode(StatusCode.SUCCESS);
        sender.send(response);
    }

    /**
     * 判断命令是否需要 Token 鉴权。
     *
     * @param command 请求命令码
     * @return 是否需要鉴权
     */
    private boolean requiresAuthentication(int command) {
        // 登出不鉴权：注销一个已经失效的 token 本就应该算成功（幂等），
        // 否则客户端「注销临时复核会话」拿到的 401 会被自己的会话失效逻辑误伤。
        return command != Command.USER_LOGIN && command != Command.USER_REGISTER
                && command != Command.USER_LOGIN_VERIFY && command != Command.USER_LOGOUT;
    }

    /**
     * 返回未授权响应。
     *
     * @param sender  当前连接的消息发送器
     * @param request 原始请求
     */
    private void sendUnauthorized(ServerMessageSender sender, Message request) {
        Message response = new Message(request.getCommand(), null);
        // 与分发器一致：回填请求 uid，客户端才能把该拒绝响应与对应请求配对
        response.setUid(request.getUid());
        response.setStatusCode(StatusCode.UNAUTHORIZED);
        response.setData("Token 无效或已过期，请重新登录");
        sender.send(response);
    }

    /**
     * 关闭消息流和 Socket。
     *
     * @param messageStream 当前消息流
     */
    private void closeSession(MessageStream messageStream) {
        try {
            if (messageStream != null) {
                messageStream.close();
            } else if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            ServerLog.warning("连接 " + connectionId + " 关闭失败：" + e.getMessage());
        }
    }
}