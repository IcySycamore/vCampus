package edu.seu.vcampus.common.handler;

import edu.seu.vcampus.common.message.Message;

/**
 * 消息发送器契约：把「发送响应」的能力交给业务模块，
 * 让模块在异步处理完成后自行决定发送时机（见 docs/消息处理接口说明.md）。
 *
 * <p>实现约定：send 不抛受检异常；底层 IO 异常（如连接断开）由具体实现内部处理。
 */
public interface MessageSender {

    /**
     * 发送一条响应消息。
     *
     * @param response 响应消息（不可为 null）
     */
    void send(Message response);
}
