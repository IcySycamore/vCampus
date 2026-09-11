package edu.seu.vcampus.server.dispatch;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 命令分发器：维护「命令码范围 → MessageHandler」的映射，把收到的消息路由到对应模块。
 *
 * <p>
 * 各模块按号段划分命令码（如 100-199 用户管理、200-299 学籍），因此每个模块
 * 只需注册一个范围（起始命令码 + 终止命令码 + 处理器），处理器内部再按具体
 * 命令码分支处理。
 *
 * <p>异步模式：{@link #dispatch} 只负责找到处理器并调用，响应由处理器通过
 * {@link MessageSender} 自行发送；命令码未登记时由分发器通过 sender 回 400。
 */
public class MessageDispatcher {

    /** 命令码范围列表（读写均线程安全；register 少、dispatch 多）。 */
    private final List<RangeEntry> m_ranges =
            new CopyOnWriteArrayList<RangeEntry>();

    /**
     * 登记一段命令码范围对应的处理器。
     *
     * <p>同一范围重复登记按<b>覆盖</b>处理（与逐命令码注册的 Map.put 语义一致），
     * 便于测试或重复装配时按命令码单独登记；只有<b>部分重叠</b>才视为配置错误。
     *
     * @param start   起始命令码（含）
     * @param end     终止命令码（含）
     * @param handler 处理器实现
     * @throws IllegalArgumentException handler 为 null、start 大于 end、
     *         或范围与已登记范围部分重叠时抛出
     */
    public void register(int start, int end, MessageHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        if (start > end) {
            throw new IllegalArgumentException("start must not exceed end");
        }
        Iterator<RangeEntry> it = m_ranges.iterator();
        while (it.hasNext()) {
            RangeEntry entry = it.next();
            if (start == entry.m_start && end == entry.m_end) {
                // 完全相同范围：覆盖旧登记，不视为冲突
                m_ranges.remove(entry);
                continue;
            }
            boolean overlaps = start <= entry.m_end && end >= entry.m_start;
            if (overlaps) {
                throw new IllegalArgumentException(
                        "range overlaps with existing range " + entry);
            }
        }
        m_ranges.add(new RangeEntry(start, end, handler));
    }

    /**
     * 登记单个命令码对应的处理器（范围注册的便捷形式）。
     *
     * @param command 命令码
     * @param handler 处理器实现
     */
    public void register(int command, MessageHandler handler) {
        register(command, command, handler);
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
        int command = request.getCommand();
        Iterator<RangeEntry> it = m_ranges.iterator();
        while (it.hasNext()) {
            RangeEntry entry = it.next();
            if (command >= entry.m_start && command <= entry.m_end) {
                entry.m_handler.handle(request, sender);
                return;
            }
        }
        Message response = new Message(command, null);
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
         * 构造一条范围记录。
         *
         * @param start   起始命令码
         * @param end     终止命令码
         * @param handler 处理器
         */
        RangeEntry(int start, int end, MessageHandler handler) {
            this.m_start = start;
            this.m_end = end;
            this.m_handler = handler;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "[" + m_start + ", " + m_end + "]";
        }
    }
}
