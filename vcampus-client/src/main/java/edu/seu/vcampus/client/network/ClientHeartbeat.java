package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** 定期发送网络层心跳，并把发送失败交给连接生命周期处理。 */
final class ClientHeartbeat implements Runnable {

    static final int HEARTBEAT_COMMAND = 1;
    private final ClientSocket client;
    private final MessageStream stream;
    private final long generation;
    private final ScheduledExecutorService scheduler;

    private ClientHeartbeat(ClientSocket client, MessageStream stream,
            long generation, ScheduledExecutorService scheduler) {
        this.client = client;
        this.stream = stream;
        this.generation = generation;
        this.scheduler = scheduler;
    }

    /**
     * 为一代连接启动心跳任务。
     *
     * @param client 客户端连接
     * @param stream 本代连接的消息流
     * @param generation 连接代次
     * @param intervalMillis 心跳间隔，毫秒
     * @return 已启动的心跳任务
     */
    static ClientHeartbeat start(ClientSocket client, MessageStream stream,
            long generation, long intervalMillis) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable task) {
                        Thread thread = new Thread(task, "vcampus-heartbeat");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
        ClientHeartbeat heartbeat = new ClientHeartbeat(
                client, stream, generation, scheduler);
        scheduler.scheduleAtFixedRate(heartbeat, intervalMillis,
                intervalMillis, TimeUnit.MILLISECONDS);
        return heartbeat;
    }

    /** 发送一次不带 token 的心跳。 */
    @Override
    public void run() {
        try {
            stream.writeMessage(new Message(HEARTBEAT_COMMAND, null));
        } catch (IOException exception) {
            client.handleConnectionClosed(generation, exception);
        }
    }

    /** 停止本代连接的后续心跳。 */
    void stop() {
        scheduler.shutdownNow();
    }

    /** 停止指定心跳；尚未启动时不执行操作。 */
    static void stop(ClientHeartbeat heartbeat) {
        if (heartbeat != null) {
            heartbeat.stop();
        }
    }

    /** @return 消息是否属于网络层心跳或心跳确认 */
    static boolean isHeartbeat(Message message) {
        return message != null && message.getCommand() == HEARTBEAT_COMMAND;
    }
}
