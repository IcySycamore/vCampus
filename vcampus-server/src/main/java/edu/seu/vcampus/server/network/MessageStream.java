package edu.seu.vcampus.server.network;

import java.io.IOException;
import java.net.Socket;

/**
 * 服务端消息流兼容入口；通用收发逻辑位于 common 模块。
 */
public class MessageStream extends edu.seu.vcampus.common.network.MessageStream {

    /**
     * 基于已建立的 socket 创建消息流。
     *
     * @param socket 已建立的连接
     * @throws IOException 创建对象流失败
     */
    public MessageStream(Socket socket) throws IOException {
        super(socket);
    }
}
