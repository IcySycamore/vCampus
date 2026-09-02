package edu.seu.vcampus.server.handler;

import edu.seu.vcampus.common.message.Message;

/**
 * 请求分发器接口：负责分发并处理客户端发来的请求消息。
 */
public interface RequestDispatcher {

    /**
     * 分发并处理客户端请求。
     *
     * @param request 客户端请求消息
     * @return 响应消息，若无需回复则返回 null
     * @throws Exception 业务处理异常
     */
    Message dispatch(Message request) throws Exception;
}