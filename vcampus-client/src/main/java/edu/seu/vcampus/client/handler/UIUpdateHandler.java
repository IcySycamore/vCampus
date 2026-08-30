package edu.seu.vcampus.client.handler;

import edu.seu.vcampus.common.message.Message;

/**
 * 将客户端网络事件交给界面层处理的回调。
 */
public interface UIUpdateHandler {

    /**
     * 处理服务器端返回的消息。
     *
     * @param message 返回消息
     */
    void handleMessage(Message message);

    /**
     * 处理连接关闭事件。
     *
     * @param cause 异常关闭原因；正常关闭时为 null
     */
    void connectionClosed(Exception cause);
}
