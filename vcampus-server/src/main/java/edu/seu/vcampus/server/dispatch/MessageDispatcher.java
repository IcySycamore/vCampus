package edu.seu.vcampus.server.dispatch;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令分发器：维护 command → MessageHandler 的映射，把收到的消息路由到对应模块。
 *
 * <p>异步模式：{@link #dispatch} 只负责找到处理器并调用，响应由处理器通过
 * {@link MessageSender} 自行发送；命令码未登记时由分发器通过 sender 回 400。
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
     * 按请求的命令码路由到对应处理器。
     *
     * @param request 请求消息（不可为 null）
     * @param sender  响应发送器（不可为 null）
     */
    public void dispatch(Message request, MessageSender sender) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (sender == null) {
            throw new IllegalArgumentException("sender must not be null");
        }
        MessageHandler handler = handlers.get(request.getCommand());
        if (handler == null) {
            Message response = new Message(request.getCommand(), null);
            response.setStatusCode(StatusCode.BAD_REQUEST);
            sender.send(response);
            return;
        }
        handler.handle(request, sender);
    }
}
