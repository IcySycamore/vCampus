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

    /** 每条命令码一把串行锁，保证同一命令同时只有一个等待者。 */
    private final Map<Integer, Object> commandLocks = new ConcurrentHashMap<Integer, Object>();

    /** 最近一次被投递的响应，其请求带的是哪个令牌；用来分辨这个 401 是不是主会话被拒。 */
    private volatile String m_lastRequestToken;

    Message await(Integer command, MessageSender sender, Message request,
            long timeoutMillis) throws InterruptedException {
        // 同一命令码的请求必须串行：等待表是「命令码 → 单个等待者」，并发时后一个请求会把前一个的
        // 槽位覆盖掉，前一个永远收不到响应，只能等满超时（现象是「服务器无响应（超时）」反复弹窗）。
        // 服务端按连接顺序回包，串行之后既保证「一次只有一个等待者」，也保证响应不会串号。
        synchronized (lockFor(command)) {
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
    }

    /** 取（或建立）某条命令码的串行锁。 */
    private Object lockFor(Integer command) {
        Object lock = commandLocks.get(command);
        if (lock != null) {
            return lock;
        }
        synchronized (commandLocks) {
            Object existing = commandLocks.get(command);
            if (existing == null) {
                existing = new Object();
                commandLocks.put(command, existing);
            }
            return existing;
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
