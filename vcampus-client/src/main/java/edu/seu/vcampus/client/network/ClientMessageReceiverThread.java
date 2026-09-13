package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

/**
 * 持续读取服务器端对象流的客户端接收任务：把消息与断连事件连同连接代次交回 {@link ClientSocketListener}。
 *
 * <p>
 * 心跳与心跳确认由本类直接丢弃，不向上传递；连接代次用于丢弃上一代连接的迟到事件。
 */
public class ClientMessageReceiverThread implements Runnable {

    private final MessageStream stream;
    private final ClientSocketListener client;
    private final long generation;
    private volatile boolean running = true;

    /**
     * 创建接收任务。
     *
     * @param stream 已创建好输入、输出对象流的消息流
     * @param client 消息与断连事件的落点
     * @param generation 所属连接代次
     */
    public ClientMessageReceiverThread(MessageStream stream, ClientSocketListener client,
            long generation) {
        if (stream == null || client == null) {
            throw new IllegalArgumentException("stream and client must not be null");
        }
        this.stream = stream;
        this.client = client;
        this.generation = generation;
    }

    @Override
    public void run() {
        Exception failure = null;
        try {
            while (running) {
                Message message = stream.recvMessage();
                if (!ClientHeartbeat.isHeartbeat(message)) {
                    client.handleReceived(generation, message);
                }
            }
        } catch (EOFException exception) {
            failure = running ? exception : null;
        } catch (SocketException exception) {
            failure = running ? exception : null;
        } catch (IOException exception) {
            failure = running ? exception : null;
        } catch (ClassNotFoundException exception) {
            failure = exception;
        } finally {
            running = false;
            client.handleConnectionClosed(generation, failure);
        }
    }

    /**
     * 请求停止接收。调用方应先等待宽限期，再关闭底层连接以解除阻塞读取； 强制关闭时尚未读取的消息会被丢弃。
     */
    public void stop() {
        running = false;
    }
}
