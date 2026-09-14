package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 界面任务执行器：把「后台调 API + EDT 回填」这件事收成一处（见 ADR-0009 D1）。
 *
 * <p>
 * 模块 API 是同步阻塞的，若直接在事件线程里调用会卡住界面；而 Swing 又要求改控件必须回到 EDT。 页面统一写成:
 *
 * <pre>
 * UiTasks.run(new UiTasks.Task&lt;PageResponse&lt;User&gt;&gt;() {
 *     public PageResponse&lt;User&gt; run() {
 *         return api.listUsers(query);
 *     }
 * }, new UiTasks.Success&lt;PageResponse&lt;User&gt;&gt;() {
 *     public void accept(PageResponse&lt;User&gt; page) {
 *         fillTable(page.getItems());
 *     }
 * });
 * </pre>
 *
 * <p>
 * 失败时：{@link ApiException} 的文案来自 {@link ApiErrors}，页面不需要写 try/catch。
 * 未提供失败回调时弹一个提示框；传入失败回调则由页面自行展示（例如登录页写红字）。
 */
public final class UiTasks {

    /** 后台执行的操作；只允许抛非受检异常（模块 API 契约）。 */
    public interface Task<T> {

        /**
         * 执行操作。
         *
         * @return 操作结果
         */
        T run();
    }

    /** 成功回调，在 EDT 上执行。 */
    public interface Success<T> {

        /**
         * 处理结果。
         *
         * @param result 操作结果
         */
        void accept(T result);
    }

    /** 失败回调，在 EDT 上执行。 */
    public interface Failure {

        /**
         * 处理失败。
         *
         * @param error 失败原因
         */
        void accept(ApiException error);
    }

    /** 私有构造器，禁止实例化工具类。 */
    private UiTasks() {
    }

    /**
     * 执行任务，失败时弹提示框。
     *
     * @param task 后台任务
     * @param onSuccess 成功回调
     * @param <T> 结果类型
     */
    public static <T> void run(final Task<T> task, final Success<T> onSuccess) {
        run(task, onSuccess, null);
    }

    /**
     * 执行任务。
     *
     * @param task 后台任务
     * @param onSuccess 成功回调；null 表示不需要回填
     * @param onFailure 失败回调；null 表示弹提示框
     * @param <T> 结果类型
     * @throws IllegalArgumentException 任务为 null
     */
    public static <T> void run(final Task<T> task, final Success<T> onSuccess,
            final Failure onFailure) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final T result = task.run();
                    if (onSuccess != null) {
                        onEdt(new Runnable() {
                            @Override
                            public void run() {
                                onSuccess.accept(result);
                            }
                        });
                    }
                } catch (RuntimeException e) {
                    final ApiException error = toApiException(e);
                    if (onFailure != null) {
                        onEdt(new Runnable() {
                            @Override
                            public void run() {
                                onFailure.accept(error);
                            }
                        });
                    } else {
                        showError(error);
                    }
                }
            }
        }, "vcampus-ui-task");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 把任意运行时异常归一为 {@link ApiException}：业务失败原样透出，其它异常视为内部错误。
     *
     * @param error 原始异常
     * @return 归一后的异常
     */
    public static ApiException toApiException(RuntimeException error) {
        if (error instanceof ApiException) {
            return (ApiException) error;
        }
        return new ApiException(edu.seu.vcampus.common.constant.StatusCode.INTERNAL_ERROR,
                error.getMessage() == null ? "客户端内部错误" : error.getMessage());
    }

    private static void onEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static void showError(final ApiException error) {
        onEdt(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, error.getMessage(), "操作失败",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }
}
