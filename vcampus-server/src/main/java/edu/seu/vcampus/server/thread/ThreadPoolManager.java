package edu.seu.vcampus.server.thread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager {
    private static ThreadPoolManager instance;
    private final ExecutorService threadPool;

    private ThreadPoolManager() {
        // 设置合理的线程池核心数（如CPU核心数* 2）
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.threadPool = Executors.newFixedThreadPool(corePoolSize);
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public void execute(Runnable task) {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.execute(task);
        }
    }

    public void shutdown() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}