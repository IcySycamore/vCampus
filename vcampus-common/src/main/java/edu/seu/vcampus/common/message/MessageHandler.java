package edu.seu.vcampus.common.message;

/**
 * 消息处理器契约（双端共用）：处理一条请求，并通过 {@link MessageSender} 发送响应。
 *
 * <p>
 * 服务端各业务模块（用户管理 / 学籍 / 选课 / 图书馆 / 商店 / 银行）各提供一个实现类， 在 {@link #handle}
 * 中根据命令码处理业务，处理完成后主动发送响应——可同步也可异步， 由模块自行决定发送时机。
 *
 * <p>
 * 客户端对应契约为 {@code client.handler.ClientMessageHandler}：同为「处理消息 + 通过 sender 回发」，
 * 但额外带一个界面回调参数，因此在客户端单独定义。
 *
 * <p>
 * 实现约定：本方法不抛受检异常；业务异常应在实现内部捕获，并通过 sender 发送携带相应 statusCode （如 500 / 400）的响应
 * Message。
 */
public interface MessageHandler {

    /**
     * 处理一条消息，并通过 sender 发送响应。
     *
     * @param request 请求消息（含 command 与 data）
     * @param sender 响应发送器
     */
    void handle(Message request, MessageSender sender);
}
