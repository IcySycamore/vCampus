package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Coordinates synchronous callers with replies arriving on the receiver thread. */
final class ClientReplyTracker {
    private final Map<Integer, PendingReply> pending = new ConcurrentHashMap<Integer, PendingReply>();

    /** 最近一次被投递的响应，其请求带的是哪个令牌；用来分辨这个 401 是不是主会话被拒。 */
    private volatile String m_lastRequestToken;

    Message await(Integer command, MessageSender sender, Message request,
            long timeoutMillis) throws InterruptedException {
        PendingReply reply = new PendingReply(request.getToken());
        pending.put(command, reply);
        try {
            sender.send(request);
        } catch (RuntimeException e) {
            pending.remove(command, reply);
            throw e;
        }
        try {
            return reply.await(timeoutMillis);
        } finally {
            pending.remove(command, reply);
        }
    }

    boolean deliver(Message message) {
        PendingReply reply = pending.remove(Integer.valueOf(message.getCommand()));
        if (reply == null) {
            return false;
        }
        m_lastRequestToken = reply.requestToken;
        reply.deliver(message);
        return true;
    }

    /**
     * 最近一次被投递的响应，其请求携带的会话令牌。
     *
     * @return 令牌；该请求未携带令牌（如登录、密码复核）时为 null
     */
    String lastRequestToken() {
        return m_lastRequestToken;
    }

    void releaseAll() {
        for (Integer command : pending.keySet()) {
            PendingReply reply = pending.remove(command);
            if (reply != null) {
                reply.release();
            }
        }
    }

    /** One waiting caller and the reply delivered to it. */
    private static final class PendingReply {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final String requestToken;
        private volatile Message message;

        PendingReply(String requestToken) {
            this.requestToken = requestToken;
        }

        void deliver(Message value) {
            message = value;
            latch.countDown();
        }

        void release() {
            latch.countDown();
        }

        Message await(long timeoutMillis) throws InterruptedException {
            return latch.await(timeoutMillis, TimeUnit.MILLISECONDS) ? message : null;
        }
    }
}
