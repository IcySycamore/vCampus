package edu.seu.vcampus.client.handler;

/**
 * 连接事件回调：连接关闭时通知监听方（如清理内存会话、引导重新登录）。
 */
public interface ConnectionListener {

    /**
     * 连接已关闭。
     *
     * @param cause 异常关闭原因；正常关闭时为 null
     */
    void connectionClosed(Exception cause);
}
