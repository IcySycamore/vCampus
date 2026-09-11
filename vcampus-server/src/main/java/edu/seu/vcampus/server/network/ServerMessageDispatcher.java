package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令分发器：维护 command → MessageHandler 的映射，把收到的消息路由到对应模块。
 *
 * <p>
 * 异步模式：{@link #dispatch} 只负责找到处理器并调用，响应由处理器通过 {@link MessageSender}
 * 自行发送；命令码未登记时由分发器通过 sender 回 400。
 */
public class ServerMessageDispatcher {

    /** command → 处理器 的映射（读写均线程安全）。 */
    private final Map<Integer, MessageHandler> handlers = new ConcurrentHashMap<Integer, MessageHandler>();

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
     * @param sender 响应发送器（不可为 null）
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
            response.setUid(request.getUid());
            response.setStatusCode(StatusCode.BAD_REQUEST);
            sender.send(response);
            return;
        }
        // 统一回填 uid：客户端据此把响应与请求精确配对。各模块无需重复实现，
        // 也避免出现「有的模块回填、有的不回填」的不一致。
        handler.handle(request, new UidFillingSender(sender, request.getUid()));
    }

    /**
     * 发送器包装：在响应未携带 uid 时回填请求的 uid。
     *
     * <p>
     * 处理器自行构造响应时常常只填命令码（如 {@code new Message(command, data)}）， 由本包装统一补齐
     * uid，作为协议约定「响应回填请求 uid」的兜底实现。
     */
    private static final class UidFillingSender implements MessageSender {

        /** 被包装的发送器。 */
        private final MessageSender m_delegate;

        /** 请求 uid。 */
        private final Long m_uid;

        /**
         * 构造包装发送器。
         *
         * @param delegate 被包装的发送器
         * @param uid 请求 uid，可为 null
         */
        UidFillingSender(MessageSender delegate, Long uid) {
            this.m_delegate = delegate;
            this.m_uid = uid;
        }

        /**
         * 回填 uid 后转发响应。
         *
         * @param response 响应消息
         */
        @Override
        public void send(Message response) {
            if (response != null && response.getUid() == null) {
                response.setUid(m_uid);
            }
            m_delegate.send(response);
        }
    }
}
