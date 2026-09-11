package edu.seu.vcampus.server.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端客户端任务线程池管理器。
 */
public class ThreadPoolManager {

    /** 单例实例。 */
    private static ThreadPoolManager instance;

    /** 客户端任务线程池。 */
    private final ExecutorService threadPool;

    /** 创建固定大小的客户端任务线程池。 */
    private ThreadPoolManager() {
        // 设置合理的线程池核心数（如 CPU 核心数 * 2）。
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.threadPool = Executors.newFixedThreadPool(corePoolSize);
    }

    /**
     * 获取线程池管理器单例。
     *
     * @return 线程池管理器
     */
    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    /**
     * 提交一个客户端任务。
     *
     * @param task 待执行任务
     */
    public void execute(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        if (!threadPool.isShutdown()) {
            threadPool.execute(task);
        }
    }

    /**
     * 停止接受新任务，并等待已提交任务完成。
     */
    public void shutdown() {
        threadPool.shutdown();
    }
}