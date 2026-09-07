package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

/**
 * 持续读取服务器端对象流的客户端接收任务。
 */
public class MessageReceiver implements Runnable {

    private final MessageStream stream;
    private final UIUpdateHandler handler;
    private volatile boolean running = true;

    /**
     * 创建接收任务。
     *
     * @param stream 已创建好输入、输出对象流的消息流
     * @param handler 网络事件处理器
     */
    public MessageReceiver(MessageStream stream, UIUpdateHandler handler) {
        if (stream == null || handler == null) {
            throw new IllegalArgumentException("stream and handler must not be null");
        }
        this.stream = stream;
        this.handler = handler;
    }

    @Override
    public void run() {
        Exception failure = null;
        try {
            while (running) {
                Message message = stream.recvMessage();
                handler.handleMessage(message);
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
            handler.connectionClosed(failure);
        }
    }

    /**
     * 请求停止接收。调用方应先等待宽限期，再关闭底层连接以解除阻塞读取；
     * 强制关闭时尚未读取的消息会被丢弃。
     */
    public void stop() {
        running = false;
    }
}
