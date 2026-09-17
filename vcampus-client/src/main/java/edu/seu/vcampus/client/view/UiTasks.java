package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;

import java.awt.Component;
import java.awt.GraphicsEnvironment;

import javax.swing.JLabel;
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

    /**
     * 后台执行的操作；只允许抛非受检异常（模块 API 契约）。
     *
     * @param <T> 操作结果类型
     */
    public interface Task<T> {

        /**
         * 执行操作。
         *
         * @return 操作结果
         */
        T run();
    }

    /**
     * 成功回调，在 EDT 上执行。
     *
     * @param <T> 操作结果类型
     */
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
     * @param task      后台任务
     * @param onSuccess 成功回调
     * @param <T>       结果类型
     */
    public static <T> void run(final Task<T> task, final Success<T> onSuccess) {
        run(task, onSuccess, null);
    }

    /**
     * 执行任务。
     *
     * @param task      后台任务
     * @param onSuccess 成功回调；null 表示不需要回填
     * @param onFailure 失败回调；null 表示弹提示框
     * @param <T>       结果类型
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

    /**
     * 失败处理器：**弹提示框**，并把原因同时留在状态栏。
     *
     * <p>
     * 只改状态栏是不够的：左下角那一行字很容易被忽略，用户看到的现象是「点了没反应」。所以失败一律 弹窗；状态栏保留同一句话，方便回溯。
     *
     * @param parent      提示框的父组件，用于居中与模态归属；null 时居中于屏幕
     * @param title       提示框标题（如「选课失败」）
     * @param statusLabel 状态栏标签；null 表示不写状态栏
     * @return 失败回调
     */
    public static Failure failureWithDialog(final Component parent, final String title,
            final JLabel statusLabel) {
        return new Failure() {
            @Override
            public void accept(ApiException error) {
                final String message = error == null || error.getMessage() == null
                        ? "操作失败"
                        : error.getMessage();
                if (statusLabel != null) {
                    statusLabel.setText("  " + message);
                }
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        // 无图形环境（单测/服务端场景）不能弹窗：降级为只写状态栏，不抛 HeadlessException
                        if (GraphicsEnvironment.isHeadless()) {
                            return;
                        }
                        JOptionPane.showMessageDialog(parent, message, title,
                                JOptionPane.WARNING_MESSAGE);
                    }
                });
            }
        };
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
