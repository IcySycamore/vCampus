package edu.seu.vcampus.server.thread;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolManagerTest 
{

    @Test
    void testSingleton() {
        ThreadPoolManager instance1 = ThreadPoolManager.getInstance();
        ThreadPoolManager instance2 = ThreadPoolManager.getInstance();
        assertSame(instance1, instance2, "ThreadPoolManager 必须是单例");
    }

    @Test
    void testConcurrentExecution() throws InterruptedException {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        int taskCount = 20;
        final CountDownLatch latch = new CountDownLatch(taskCount);
        final AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            manager.execute(new Runnable() {
                @Override
                public void run() {
                    counter.incrementAndGet();
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(3, TimeUnit.SECONDS);
        assertTrue(finished, "所有并发任务应在 3 秒内执行完毕");
        assertEquals(taskCount, counter.get(), "提交的任务数量与实际完成数量应一致");
    }

    @Test
    void testTaskRunsOnPoolThread() throws InterruptedException {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> worker =
                new AtomicReference<Thread>();

        manager.execute(new Runnable() {
            @Override
            public void run() {
                worker.set(Thread.currentThread());
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS),
                "线程池任务应在 3 秒内开始执行");
        assertNotNull(worker.get());
        assertNotEquals(Thread.currentThread(), worker.get(),
                "任务不应在测试线程中执行");
    }
}