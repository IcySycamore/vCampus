package edu.seu.vcampus.client.network;

import java.io.IOException;

/** 在后台执行有限次数的重连，不占用接收线程。 */
final class ClientReconnectTask implements Runnable {

    private final ClientSocket client;
    private final ClientConnectionFactory connectionFactory;
    private final long generation;

    ClientReconnectTask(ClientSocket client, ClientConnectionFactory connectionFactory,
            long generation) {
        this.client = client;
        this.connectionFactory = connectionFactory;
        this.generation = generation;
    }

    static Thread create(ClientSocket client, ClientConnectionFactory connectionFactory,
            long generation) {
        Thread thread = new Thread(
                new ClientReconnectTask(client, connectionFactory, generation),
                "vcampus-reconnect");
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void run() {
        try {
            ClientConnectionFactory.Connection connection =
                    connectionFactory.openWithRetry();
            client.installReconnect(connection, generation);
        } catch (IOException ignored) {
            // UI 已收到原始断线事件；有限次数重连耗尽后保持断开状态。
        } finally {
            client.reconnectFinished(Thread.currentThread());
        }
    }
}
