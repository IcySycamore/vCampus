package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;

/** 将接收线程事件连同所属连接代次转交给客户端。 */
final class ClientReceiverHandler implements UIUpdateHandler {

    private final ClientSocket client;
    private final long generation;

    ClientReceiverHandler(ClientSocket client, long generation) {
        this.client = client;
        this.generation = generation;
    }

    @Override
    public void handleMessage(Message message) {
        client.handleReceived(generation, message);
    }

    @Override
    public void connectionClosed(Exception cause) {
        client.handleConnectionClosed(generation, cause);
    }
}
