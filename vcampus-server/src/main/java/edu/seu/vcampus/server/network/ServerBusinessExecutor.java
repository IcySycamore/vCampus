package edu.seu.vcampus.server.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 业务处理线程池：把 handler 的执行从连接的读循环里挪出来。
 *
 * <p>
 * <b>为什么必须挪出来</b>：每个连接由一个读线程驱动，它在「recvMessage → 处理 → 回包」之间
 * 循环。如果 handler 就地执行，一个耗时操作（例如拉全量档案并逐个联查姓名）就会把读循环卡住，
 * 这段时间该连接<b>收不到任何消息</b>——包括心跳。心跳堆在 TCP 缓冲区里没人处理，客户端迟迟
 * 等不到确认，最终误判为断连并重连，而重连会把原连接上还没跑完的业务一并丢掉。
 *
 * <p>
 * 把业务丢进本池后，读循环立刻回到 {@code recvMessage}，心跳随到随回；业务慢只是该请求的响应
 * 晚一点，不再牵连连接本身。
 *
 * <p>
 * <b>为什么不复用 {@code server.thread.ThreadPoolManager}</b>：那个池被「每连接一个读线程」
 * 长期占用，若把业务也丢进去，连接一多就会出现「读线程占满池 → 业务任务排不上」的互相饿死。
 * 两个池分开，各自的容量问题不会串到一起。
 *
 * <p>
 * <b>并发后果</b>：同一连接上的多个请求会被并发处理，**响应顺序不再与请求顺序一致**。客户端的
 * 分发器按 {@code uid} 配对请求与响应，因此不影响正确性；但不要假设「先发的一定先回」。
 * 发送侧的 {@code MessageStream.writeMessage} 已加锁，多个业务线程同时回包是安全的。
 *
 * <p>
 * 采用可缓存的线程池：业务耗时是突发性的（大部分请求很快），固定池容易出现「少量慢请求把
 * 池占满，其余请求排队」。空闲线程会被回收，不会长期占用资源；线程设为守护线程，进程退出时
 * 自动结束，不必依赖停机流程。
 */
final class ServerBusinessExecutor {

    /** 业务线程池。 */
    private static final ExecutorService POOL = Executors.newCachedThreadPool(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable task) {
                    Thread thread = new Thread(task, "vcampus-handler");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    /** 私有构造器，禁止实例化工具类。 */
    private ServerBusinessExecutor() {
    }

    /**
     * 提交一个业务处理任务。
     *
     * <p>
     * 池已停止时静默丢弃：连接正在收尾，此时再报错只会掩盖真正的断开原因。
     *
     * @param task 任务（不可为 null）
     */
    static void execute(Runnable task) {
        if (task == null || POOL.isShutdown()) {
            return;
        }
        POOL.execute(task);
    }
}
