package edu.seu.vcampus.client.handler;

import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;

/**
 * 客户端业务处理器契约：与 server 的 {@code MessageHandler} 对称——同样是 「处理消息 + 通过 sender
 * 回发」，并额外多一个 {@link UiCallback} 用于界面操作。
 *
 * <p>
 * 各业务模块（用户管理 / 学籍 / 选课 / 图书馆 / 商店 / 银行）在客户端各提供一个实现类， 按命令码登记到
 * {@code ClientMessageDispatcher}；收到消息时由分发器统一传入回发通道与 UI 回调。
 */
public interface ClientMessageHandler {

    /**
     * 处理一条服务器消息。
     *
     * @param message 服务器消息（含 command 与 data）
     * @param sender 回发通道（与服务端 MessageHandler 同款）
     * @param ui UI 操作回调（把界面更新切到 EDT）
     */
    void handle(Message message, MessageSender sender, UiCallback ui);
}
