package edu.seu.vcampus.client.user;

/**
 * 批量操作进度回调（见 ADR-0010 D1：批量命令单包上限 50 条，由客户端负责分片）。
 *
 * <p>
 * 回调在发起批量操作的线程上触发（通常是 {@code UiTasks} 的后台线程），因此实现里<b>不得</b>直接操作 Swing
 * 控件；需要更新界面时请回切到事件线程。存在这个回调的原因是：分片必须是<b>可见</b>的， 否则用户导入 1000 条时会以为程序卡死。
 */
public interface BatchProgressListener {

    /**
     * 通知一片已处理完毕。
     *
     * @param completed 已处理的条目数（含失败条目）
     * @param total     总条目数
     */
    void onProgress(int completed, int total);
}
