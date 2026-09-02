package edu.seu.vcampus.server.dispatch;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.message.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令分发器：维护 command → MessageHandler 的映射，把收到的消息路由到对应模块。
 *
 * <p>各业务模块在服务器启动阶段通过 {@link #register} 登记自己负责的命令码；
 * 服务器收到消息后调用 {@link #dispatch}，按 command 找到处理器并调用，
 * 无需关心具体是哪个模块实现（见 docs/消息处理接口说明.md）。
 */
public class MessageDispatcher {

    /** command → 处理器 的映射（读写均线程安全）。 */
    private final Map<Integer, MessageHandler> handlers =
            new ConcurrentHashMap<Integer, MessageHandler>();

    /**
     * 登记一个命令码对应的处理器。
     *
     * @param command 命令码（各模块号段见 Command）
     * @param handler 处理器实现
     */
    public void register(int command, MessageHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        handlers.put(command, handler);
    }

    /**
     * 按请求的命令码路由到对应处理器，返回其响应。
     *
     * @param request 请求消息（不可为 null）
     * @return 处理器返回的响应；命令码未登记时返回 400 响应
     */
    public Message dispatch(Message request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        MessageHandler handler = handlers.get(request.getCommand());
        if (handler == null) {
            Message response = new Message(request.getCommand(), null);
            response.setStatusCode(StatusCode.BAD_REQUEST);
            return response;
        }
        return handler.handle(request);
    }
}
