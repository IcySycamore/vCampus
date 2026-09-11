package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.ClientMessageHandler;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.handler.UiCallback;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.random.RandomGen;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 客户端消息分发器：服务端 {@code MessageDispatcher} 的对称件——维护「命令码 → 处理器」映射，并作为
 * <b>收到的消息与连接事件的落点</b>，出站只依赖注入的 {@link MessageSender}。
 *
 * <p>
 * 连接的建立/关闭/重连由应用入口 {@code VCampusClientApp} 持有并驱动，本类<b>不持有连接</b>， 只被动接收连接回调。
 *
 * <p>
 * 出站：发送前用 {@link RandomGen} 分配<b>随机</b>序列号（uid），随机源复用随机数工具， 故不需要额外的传输层。
 *
 * <p>
 * 回传：按「命令码 + 状态码」分发——服务器回显请求的命令码，状态码表示处理结果。 命中等待槽则唤醒请求方（由请求方看状态码判定成败），
 * 否则按命令码交给登记的 {@link ClientMessageHandler}，未登记则走兜底处理器。
 *
 * <p>
 * 约定：同一命令码同时只保留一个在途请求，重复发起会顶替前一个等待方。
 */
public class ClientMessageDispatcher implements UIUpdateHandler {

    /** 随机源：消息序列号复用其随机能力。 */
    private final RandomGen m_random = new RandomGen();

    /** command → 处理器（服务端推送 / 非配对消息）。 */
    private final Map<Integer, ClientMessageHandler> m_handlers = new ConcurrentHashMap<Integer, ClientMessageHandler>();

    /** command → 等待槽（同一命令码同时只有一个在途请求）。 */
    private final Map<Integer, PendingReply> m_pending = new ConcurrentHashMap<Integer, PendingReply>();

    /** 发送通道（连接建立后由应用入口注入）。 */
    private volatile MessageSender m_sender;

    /** 未登记命令码的兜底处理器。 */
    private volatile ClientMessageHandler m_fallback;

    /** 处理器操作界面用的回调（默认在当前线程直接执行）。 */
    private volatile UiCallback m_ui = new InlineUiCallback();

    /** 连接事件监听（模块装配时登记，例如会话随连接失效）。 */
    private final List<ConnectionListener> m_listeners = new CopyOnWriteArrayList<ConnectionListener>();

    /**
     * 注入发送通道：入口把连接对象适配为 {@link MessageSender} 后注入一次。
     *
     * @param sender 发送通道
     * @throws IllegalArgumentException 发送通道为 null
     */
    public void bindSender(MessageSender sender) {
        if (sender == null) {
            throw new IllegalArgumentException("sender must not be null");
        }
        m_sender = sender;
    }

    /**
     * 登记一个命令码对应的处理器。
     *
     * @param command 命令码（各模块号段见 Command）
     * @param handler 处理器
     * @throws IllegalArgumentException 处理器为 null
     */
    public void register(int command, ClientMessageHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        m_handlers.put(Integer.valueOf(command), handler);
    }

    /**
     * 登记未命中任何命令码时的兜底处理器。
     *
     * @param handler 兜底处理器；null 表示取消兜底
     */
    public void registerFallback(ClientMessageHandler handler) {
        m_fallback = handler;
    }

    /**
     * 注入界面操作回调（有界面的入口注入；未注入时在当前线程直接执行）。
     *
     * @param ui UI 回调；null 表示恢复默认
     */
    public void setUiCallback(UiCallback ui) {
        m_ui = ui == null ? new InlineUiCallback() : ui;
    }

    /**
     * 登记连接事件监听（模块装配时登记，连接断开时统一通知）。
     *
     * @param listener 监听器
     * @throws IllegalArgumentException 监听器为 null
     */
    public void addConnectionListener(ConnectionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        m_listeners.add(listener);
    }

    /**
     * 发送一条消息：分配随机序列号后经发送通道写出，不等待响应。
     *
     * @param message 待发消息
     * @throws IllegalArgumentException 消息为 null
     * @throws IllegalStateException 发送通道尚未注入
     */
    public void send(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        assignUid(message);
        sender().send(message);
    }

    /**
     * 发送请求并等待同一命令码的响应。
     *
     * @param request 请求消息
     * @param timeoutMillis 等待超时，毫秒
     * @return 响应消息（状态码由调用方判定）；超时或连接断开返回 null
     * @throws InterruptedException 等待被中断
     * @throws IllegalArgumentException 请求为 null
     * @throws IllegalStateException 发送通道尚未注入
     */
    public Message request(Message request, long timeoutMillis) throws InterruptedException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        Integer command = Integer.valueOf(request.getCommand());
        assignUid(request);
        // 先登记等待槽再发送：响应可能由接收线程在发送后立即送达
        PendingReply reply = new PendingReply();
        m_pending.put(command, reply);
        try {
            sender().send(request);
        } catch (RuntimeException e) {
            m_pending.remove(command);// 发送失败：摘掉槽位后原样抛出
            throw e;
        }
        try {
            return reply.await(timeoutMillis);
        } finally {
            m_pending.remove(command);
        }
    }

    /**
     * 路由一条收到的消息：先按命令码唤醒等待中的请求方，否则按命令码交给处理器。
     *
     * @param message 收到的消息；null 直接忽略
     */
    public void dispatch(Message message) {
        if (message == null) {
            return;
        }
        Integer command = Integer.valueOf(message.getCommand());
        PendingReply reply = m_pending.remove(command);
        if (reply != null) {
            reply.deliver(message);// 命令码回显即本次请求的响应，状态码由请求方判定
            return;
        }
        ClientMessageHandler handler = m_handlers.get(command);
        if (handler == null) {
            handler = m_fallback;
        }
        if (handler == null) {
            return;
        }
        try {
            handler.handle(message, sender(), m_ui);
        } catch (RuntimeException e) {
            // 与连接层同款隔离：单个处理器出错不应打断接收线程
            System.err.println("客户端消息分发失败: " + e.getMessage());
        }
    }

    /** 连接层回调：收到的消息交给路由。 */
    @Override
    public void handleMessage(Message message) {
        dispatch(message);
    }

    /** 连接层回调：连接已关闭——先唤醒全部在途请求方（视为未收到响应），再通知登记的监听者。 */
    @Override
    public void connectionClosed(Exception cause) {
        for (Integer command : m_pending.keySet()) {
            PendingReply reply = m_pending.remove(command);
            if (reply != null) {
                reply.release();
            }
        }
        for (ConnectionListener listener : m_listeners) {
            listener.connectionClosed(cause);
        }
    }

    /**
     * 为待发消息分配随机序列号；已有序列号则保留。
     *
     * @param message 待发消息
     */
    private void assignUid(Message message) {
        if (message.getUid() == null) {
            long uid = m_random.getUuid().getMostSignificantBits() & Long.MAX_VALUE;
            message.setUid(Long.valueOf(uid));
        }
    }

    /**
     * 取已注入的发送通道。
     *
     * @return 发送通道
     * @throws IllegalStateException 尚未注入
     */
    private MessageSender sender() {
        MessageSender sender = m_sender;
        if (sender == null) {
            throw new IllegalStateException("sender is not bound");
        }
        return sender;
    }

    /** 等待槽位。 */
    private static final class PendingReply {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Message message;

        void deliver(Message value) {
            message = value;
            latch.countDown();
        }

        void release() {
            latch.countDown();
        }

        Message await(long timeoutMillis) throws InterruptedException {
            if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                return null;
            }
            return message;
        }
    }

    /** 默认界面回调：未注入界面环境时在当前线程直接执行。 */
    private static final class InlineUiCallback implements UiCallback {
        @Override
        public void run(Runnable task) {
            task.run();
        }
    }
}
