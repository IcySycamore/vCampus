package edu.seu.vcampus.common.handler;

import edu.seu.vcampus.common.message.Message;

/**
 * 消息处理器契约（见 docs/消息处理接口说明.md）。
 *
 * <p>每个业务模块（用户管理 / 学籍 / 选课 / 图书馆 / 商店 / 银行）各提供一个实现类，
 * 在 {@link #handle} 中根据命令码把消息分发到本模块内部的具体处理逻辑。服务器端的
 * MessageDispatcher 依据 command 把消息路由到对应的实现。
 *
 * <p>实现约定：本方法不抛受检异常；业务异常应在实现内部捕获，并返回携带
 * 相应 statusCode（如 500 / 400）的响应 Message。
 */
public interface MessageHandler {

    /**
     * 处理一条请求消息，返回响应消息。
     *
     * @param request 请求消息（含 command 与 data）
     * @return 响应消息（statusCode 标明处理结果）
     */
    Message handle(Message request);
}
