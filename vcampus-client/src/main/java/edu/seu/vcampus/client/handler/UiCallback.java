package edu.seu.vcampus.client.handler;

/**
 * UI 操作回调：把界面更新派发到界面线程（Swing EDT）。
 *
 * <p>
 * 这是客户端处理器相对服务端 {@code MessageHandler} 多出来的那条通道： 处理器收到消息后，用本回调把控件更新切回
 * EDT，避免在接收线程上直接改界面。
 */
public interface UiCallback {

    /**
     * 在界面线程上执行一段界面更新。
     *
     * @param task 界面更新任务
     */
    void run(Runnable task);
}
