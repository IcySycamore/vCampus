package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 命令分发器：维护「命令码范围 → MessageHandler」的映射，把收到的消息路由到对应模块。
 *
 * <p>
 * 提供两个重载的登记入口：
 * <ul>
 * <li>{@link #register(int, int, MessageHandler)}：登记一整段命令码（如某模块的
 * 200-299 号段），段内各命令由该处理器自行分支；</li>
 * <li>{@link #register(int, MessageHandler)}：只登记一个命令码，等价于
 * {@code register(command, command, handler)}。</li>
 * </ul>
 * 同一范围重复登记按<b>覆盖</b>处理（与逐命令码登记的覆盖语义一致）；范围之间
 * <b>部分重叠</b>属号段划分错误，直接拒绝，以便尽早暴露冲突。
 *
 * <p>
 * 异步模式：{@link #dispatch} 只负责找到处理器并调用，响应由处理器通过 {@link MessageSender}
 * 自行发送；命令码未登记时由分发器通过 sender 回 400。
 */
public class ServerMessageDispatcher {

    /** 命令码范围登记表（登记少、路由多，用写时复制保证遍历期安全）。 */
    private final List<RangeEntry> m_ranges = new CopyOnWriteArrayList<RangeEntry>();

    /**
     * 登记一整段命令码对应的处理器。
     *
     * @param start 起始命令码（含）
     * @param end 终止命令码（含）
     * @param handler 处理器实现
     * @throws IllegalArgumentException handler 为 null、start 大于 end，或范围与已登记范围部分重叠时抛出
     */
    public void register(int start, int end, MessageHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        if (start > end) {
            throw new IllegalArgumentException("start must not exceed end");
        }
        Iterator<RangeEntry> iterator = m_ranges.iterator();
        while (iterator.hasNext()) {
            RangeEntry entry = iterator.next();
            if (start == entry.m_start && end == entry.m_end) {
                // 完全相同范围：覆盖旧登记，不视为冲突
                m_ranges.remove(entry);
                continue;
            }
            if (start <= entry.m_end && end >= entry.m_start) {
                throw new IllegalArgumentException(
                        "range overlaps with existing range " + entry);
            }
        }
        m_ranges.add(new RangeEntry(start, end, handler));
    }

    /**
     * 登记单个命令码对应的处理器（等价于登记 {@code [command, command]} 范围）。
     *
     * @param command 命令码（各模块号段见 Command）
     * @param handler 处理器实现
     */
    public void register(int command, MessageHandler handler) {
        register(command, command, handler);
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
        int command = request.getCommand();
        Iterator<RangeEntry> iterator = m_ranges.iterator();
        while (iterator.hasNext()) {
            RangeEntry entry = iterator.next();
            if (command >= entry.m_start && command <= entry.m_end) {
                // 统一回填 uid：客户端据此把响应与请求精确配对。各模块无需重复实现，
                // 也避免出现「有的模块回填、有的不回填」的不一致。
                entry.m_handler.handle(request,
                        new UidFillingSender(sender, request.getUid()));
                return;
            }
        }
        Message response = new Message(command, null);
        response.setUid(request.getUid());
        response.setStatusCode(StatusCode.BAD_REQUEST);
        sender.send(response);
    }

    /**
     * 一条命令码范围及其处理器。
     */
    private static final class RangeEntry {

        /** 起始命令码（含）。 */
        private final int m_start;

        /** 终止命令码（含）。 */
        private final int m_end;

        /** 处理器。 */
        private final MessageHandler m_handler;

        /**
         * 构造一条范围登记。
         *
         * @param start 起始命令码（含）
         * @param end 终止命令码（含）
         * @param handler 处理器
         */
        RangeEntry(int start, int end, MessageHandler handler) {
            this.m_start = start;
            this.m_end = end;
            this.m_handler = handler;
        }

        /**
         * @return 区间描述，用于冲突报错时定位
         */
        @Override
        public String toString() {
            return "[" + m_start + ", " + m_end + "]";
        }
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
