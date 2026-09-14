package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.StatusCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UiTasks 测试：后台执行 + EDT 回填 + 失败归一（ADR-0009 D1/D10）。
 *
 * <p>
 * 断言用闩子等待回调，不依赖真实 EDT 是否就绪（SwingUtilities.invokeLater 在无界面环境下也能入队）。
 */
class UiTasksTest {

    /** 成功路径：回调拿到任务返回值。 */
    @Test
    void deliversResultToCallback() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<String> received = new AtomicReference<String>();

        UiTasks.run(new UiTasks.Task<String>() {
            @Override
            public String run() {
                return "ok";
            }
        }, new UiTasks.Success<String>() {
            @Override
            public void accept(String result) {
                received.set(result);
                done.countDown();
            }
        });

        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertEquals("ok", received.get());
    }

    /** 失败路径：ApiException 原样交给失败回调。 */
    @Test
    void deliversApiExceptionToFailureCallback() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<ApiException> received = new AtomicReference<ApiException>();

        UiTasks.run(new UiTasks.Task<String>() {
            @Override
            public String run() {
                throw new ApiException(StatusCode.FORBIDDEN);
            }
        }, new UiTasks.Success<String>() {
            @Override
            public void accept(String result) {
                // 不应走到这里
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                received.set(error);
                done.countDown();
            }
        });

        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertNotNull(received.get());
        assertEquals(StatusCode.FORBIDDEN, received.get().getStatusCode());
    }

    /** 非 ApiException 的运行时异常被归一为内部错误，不吞异常。 */
    @Test
    void wrapsUnexpectedException() {
        RuntimeException raw = new IllegalStateException("boom");
        ApiException wrapped = UiTasks.toApiException(raw);
        assertEquals(StatusCode.INTERNAL_ERROR, wrapped.getStatusCode());
        assertEquals("boom", wrapped.getMessage());
    }

    /** 已是 ApiException 时原样透出。 */
    @Test
    void passesThroughApiException() {
        ApiException original = new ApiException(ApiErrors.LOCAL_NETWORK);
        assertSame(original, UiTasks.toApiException(original));
    }

    /** 任务为 null 时立刻失败（编程错误，不必等到后台线程）。 */
    @Test
    void rejectsNullTask() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                UiTasks.run(null, null);
            }
        });
    }
}
