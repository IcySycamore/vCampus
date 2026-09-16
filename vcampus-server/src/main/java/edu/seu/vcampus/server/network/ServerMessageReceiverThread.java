package edu.seu.vcampus.server.network;

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
    /** 上层已建好的消息流；为 null 时由本线程在 run 中自行创建。 */
    private final MessageStream providedStream;

    /** 连接运行状态。 */
    private volatile boolean running = true;

    /**
     * 创建客户端处理线程（由本线程负责创建消息流）。
     *
     * @param socket 已建立的客户端连接
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
    }

    /**
     * 创建客户端处理线程（复用上层已建好的消息流）。
     *
     * <p>
     * 对象流的流头只能读一次：{@code ServerSocketListener.accept()} 在创建 MessageStream
     * 时已完成握手读取，若此处再建一个 MessageStream 会导致阻塞或 读到脏数据。因此握手由监听端负责时，用本构造函数把已建好的流交给线程复用。
     *
     * @param stream 已初始化（含握手）的消息流
     * @param sessionManager 认证模块的会话管理器
     */
    public ServerMessageReceiverThread(MessageStream stream, SessionManager sessionManager) {
        if (stream == null) {
            throw new IllegalArgumentException("stream must not be null");
        }
        if (stream.getSocket() == null) {
            throw new IllegalArgumentException("stream must wrap an established socket");
        }
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionManager must not be null");
        }
        this.socket = stream.getSocket();
        this.sessionManager = sessionManager;
        this.providedStream = stream;
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
                    System.err.println("客户端 15 秒未发送消息，关闭连接: " + socket.getRemoteSocketAddress());
                    break;
                } catch (EOFException e) {
                    System.out.println("客户端已断开连接: " + socket.getRemoteSocketAddress());
                    break;
                } catch (SocketException e) {
                    System.out.println("客户端 Socket 已断开: " + socket.getRemoteSocketAddress());
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("客户端消息反序列化失败: " + e.getMessage());
                    break;
                }

                if (request == null) {
                    break;
                }

                if (isHeartbeat(request)) {
                    sendHeartbeatAck(messageSender);
                    continue;
                }

                if (requiresAuthentication(request.getCommand())
                        && sessionManager.validate(request.getToken()) == null) {
                    sendUnauthorized(messageSender, request);
                    continue;
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
                            System.err.println("消息分发失败: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("客户端连接异常: " + e.getMessage());
        } finally {
            closeSession(messageStream);
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
        return command != Command.USER_LOGIN && command != Command.USER_REGISTER
                && command != Command.USER_LOGIN_VERIFY;
    }

    /**
     * 返回未授权响应。
     *
     * @param sender 当前连接的消息发送器
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
            System.err.println("关闭客户端连接失败: " + e.getMessage());
        }
    }
}