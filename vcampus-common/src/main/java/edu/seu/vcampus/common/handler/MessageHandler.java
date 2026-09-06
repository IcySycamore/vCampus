package edu.seu.vcampus.common.handler;

import edu.seu.vcampus.common.message.Message;

/**
 * 消息处理器契约（见 docs/消息处理接口说明.md）。
 *
 * <p>每个业务模块（用户管理 / 学籍 / 选课 / 图书馆 / 商店 / 银行）各提供一个实现类，
 * 在 {@link #handle} 中根据命令码处理业务，处理完成后通过 {@link MessageSender}
 * 主动发送响应——可同步也可异步，由模块自行决定发送时机。
 *
 * <p>实现约定：本方法不抛受检异常；业务异常应在实现内部捕获，并通过 sender
 * 发送携带相应 statusCode（如 500 / 400）的响应 Message。
 */
public interface MessageHandler {

    /**
     * 处理一条请求消息，并通过 sender 发送响应。
     *
     * @param request 请求消息（含 command 与 data）
     * @param sender  响应发送器
     */
    void handle(Message request, MessageSender sender);
}
