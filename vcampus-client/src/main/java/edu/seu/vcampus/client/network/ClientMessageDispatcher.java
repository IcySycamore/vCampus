package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.constant.StatusCode;

import edu.seu.vcampus.client.handler.ClientMessageHandler;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.handler.UiCallback;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.random.RandomGen;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Routes client messages to a waiting request or a registered push handler. The application owns
 * the connection and binds its sending channel here.
 */
public class ClientMessageDispatcher implements UIUpdateHandler {
    private final RandomGen m_random = new RandomGen();
    private final Map<Integer, ClientMessageHandler> m_handlers = new ConcurrentHashMap<Integer, ClientMessageHandler>();
    private final ClientReplyTracker m_replies = new ClientReplyTracker();
    private final List<ConnectionListener> m_listeners = new CopyOnWriteArrayList<ConnectionListener>();
    private volatile MessageSender m_sender;
    private volatile ClientMessageHandler m_fallback;
    private volatile UiCallback m_ui = new InlineUiCallback();

    /** 会话失效动作（响应回 401 时触发）；null 表示无人处理。 */
    private volatile Runnable m_sessionExpired;

    /**
     * Binds the connection's sending channel.
     * 
     * @param sender sending channel
     */
    public void bindSender(MessageSender sender) {
        if (sender == null) {
            throw new IllegalArgumentException("sender must not be null");
        }
        m_sender = sender;
    }

    /**
     * Registers a push handler for one command.
     * 
     * @param command command number
     * @param handler message handler
     */
    public void register(int command, ClientMessageHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        m_handlers.put(Integer.valueOf(command), handler);
    }

    /**
     * Registers the handler used for unknown commands.
     * 
     * @param handler fallback handler; null clears it
     */
    public void registerFallback(ClientMessageHandler handler) {
        m_fallback = handler;
    }

    /**
     * Selects how handlers schedule UI work.
     * 
     * @param ui UI callback; null restores inline execution
     */
    public void setUiCallback(UiCallback ui) {
        m_ui = ui == null ? new InlineUiCallback() : ui;
    }

    /**
     * 登记「登录态已失效」动作：任何响应回 401 时立即触发。
     *
     * <p>
     * 只在分发器这一处处理：各模块各弹一个「登录状态已失效」只会让界面停在「已登录但什么都点不动」的状态。
     *
     * @param action 动作；null 清除
     */
    public void setSessionExpiredAction(Runnable action) {
        m_sessionExpired = action;
    }

    /**
     * Registers a listener for connection loss.
     * 
     * @param listener connection listener
     */
    public void addConnectionListener(ConnectionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        m_listeners.add(listener);
    }

    /**
     * Assigns a uid and sends without waiting for a reply.
     * 
     * @param message outbound message
     */
    public void send(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        assignUid(message);
        sender().send(message);
    }

    /**
     * Sends a request and waits for a reply with the same command.
     * 
     * @param request       request message
     * @param timeoutMillis timeout in milliseconds
     * @return reply, or null after timeout or connection loss
     * @throws InterruptedException when waiting is interrupted
     */
    public Message request(Message request, long timeoutMillis) throws InterruptedException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        assignUid(request);
        return m_replies.await(Integer.valueOf(request.getCommand()), sender(), request,
                timeoutMillis);
    }

    /**
     * Routes a received message.
     * 
     * @param message received message; null is ignored
     */
    public void dispatch(Message message) {
        if (message == null) {
            return;
        }
        if (StatusCode.UNAUTHORIZED.equals(message.getStatusCode())) {
            // 令牌失效：先通知会话收尾（回登录页），再把响应交给等待方（它只会抛异常，界面已不在）
            Runnable action = m_sessionExpired;
            if (action != null) {
                action.run();
            }
        }
        if (m_replies.deliver(message)) {
            return;
        }
        ClientMessageHandler handler = m_handlers.get(Integer.valueOf(message.getCommand()));
        if (handler == null) {
            handler = m_fallback;
        }
        if (handler != null) {
            handlePush(handler, message);
        }
    }

    /** Routes a message received by the socket layer. @param message received message */
    @Override
    public void handleMessage(Message message) {
        dispatch(message);
    }

    /**
     * Releases waiting requests and notifies connection listeners.
     * 
     * @param cause close cause; null for a normal close
     */
    @Override
    public void connectionClosed(Exception cause) {
        m_replies.releaseAll();
        for (ConnectionListener listener : m_listeners) {
            listener.connectionClosed(cause);
        }
    }

    private void handlePush(ClientMessageHandler handler, Message message) {
        try {
            handler.handle(message, sender(), m_ui);
        } catch (RuntimeException e) {
            System.err.println("客户端消息分发失败: " + e.getMessage());
        }
    }

    private void assignUid(Message message) {
        if (message.getUid() == null) {
            long uid = m_random.getUuid().getMostSignificantBits() & Long.MAX_VALUE;
            message.setUid(Long.valueOf(uid));
        }
    }

    private MessageSender sender() {
        MessageSender sender = m_sender;
        if (sender == null) {
            throw new IllegalStateException("sender is not bound");
        }
        return sender;
    }

    /** Runs UI work inline until the application installs an EDT callback. */
    private static final class InlineUiCallback implements UiCallback {
        @Override
        public void run(Runnable task) {
            task.run();
        }
    }
}
