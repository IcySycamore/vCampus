package edu.seu.vcampus.client.auth;

import java.io.IOException;

/** 在后台释放连接，避免窗口关闭时阻塞 Swing 事件线程。 */
public final class SessionCleanup {
    private SessionCleanup() {
    }

    /**
     * 解除界面回调，并异步停止会话及其网络线程。
     * @param session 待释放的会话，可为 null
     */
    public static void closeAsync(final ClientSession session) {
        if (session == null) {
            return;
        }
        session.setHandler(null);
        Thread closer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    session.close();
                } catch (IOException ignored) {
                    // close 已停止会话并清理连接；窗口退出不再弹出网络错误。
                }
            }
        }, "vcampus-session-close");
        closer.setDaemon(true);
        closer.start();
    }
}
