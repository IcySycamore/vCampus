package edu.seu.vcampus.server.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 服务端线程池管理器：统一管理并调度处理客户端请求的并发线程池（见 docs/ADR 0006）。
 *
 * <p>采用单例模式配置显式参数的 {@link ThreadPoolExecutor} 与有界队列，
 * 并提供自定义命名 ThreadFactory（格式为 {@code vcampus-worker-%d}）以及优雅关闭机制。
 */
public class ThreadPoolManager {

    private static final Logger logger = Logger.getLogger(ThreadPoolManager.class.getName());

    /** 默认核心线程数。 */
    private static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    /** 默认最大线程数。 */
    private static final int DEFAULT_MAX_POOL_SIZE = DEFAULT_CORE_POOL_SIZE * 2;

    /** 空闲线程存活时间（秒）。 */
    private static final long KEEP_ALIVE_TIME_SECONDS = 60L;

    /** 有界任务队列容量。 */
    private static final int QUEUE_CAPACITY = 256;

    /** 底层线程池 Executor。 */
    private final ThreadPoolExecutor executor;

    /** 单例持有人。 */
    private static class Holder {
        private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
    }

    /**
     * 获取 ThreadPoolManager 单例实例。
     *
     * @return 单例对象
     */
    public static ThreadPoolManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 私有构造函数：显式构造有界线程池与命名线程工厂。
     */
    private ThreadPoolManager() {
        ThreadFactory namedThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "vcampus-worker-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };

        this.executor = new ThreadPoolExecutor(
                DEFAULT_CORE_POOL_SIZE,
                DEFAULT_MAX_POOL_SIZE,
                KEEP_ALIVE_TIME_SECONDS,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY),
                namedThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        logger.info(String.format("ThreadPoolManager 初始化完成 [Core: %d, Max: %d, QueueCapacity: %d]",
                DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE, QUEUE_CAPACITY));
    }

    /**
     * 提交一个 Runnable 任务（如 {@link ClientThread}）至线程池执行。
     *
     * @param task 待执行的任务
     * @throws IllegalArgumentException   当 task 为 null 时抛出
     * @throws RejectedExecutionException 当线程池已关闭或无法接受新任务时抛出
     */
    public void execute(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("提交的任务不能为 null");
        }
        executor.execute(task);
    }

    /**
     * 优雅关闭线程池：拒绝新任务，等待已提交任务执行完毕；超时后强制关闭。
     *
     * @param timeoutSeconds 最大等待超时时间（秒）
     */
    public void shutdown(long timeoutSeconds) {
        logger.info("正在关闭线程池...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                logger.warning("线程池未能在指定时间内完全平滑关闭，尝试强制关闭...");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.severe("线程池强制关闭超时，可能存在未终止的线程");
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "等待线程池关闭时被中断", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("线程池已关闭");
    }

    /**
     * 使用默认超时时间（30 秒）优雅关闭线程池。
     */
    public void shutdown() {
        shutdown(30L);
    }

    /**
     * 获取当前活动线程数。
     *
     * @return 活动线程数量
     */
    public int getActiveCount() {
        return executor.getActiveCount();
    }

    /**
     * 获取底层线程池对象（供单元测试与性能监控使用）。
     *
     * @return {@link ThreadPoolExecutor} 实例
     */
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }
}