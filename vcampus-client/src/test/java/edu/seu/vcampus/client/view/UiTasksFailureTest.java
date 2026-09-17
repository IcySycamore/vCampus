package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.StatusCode;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 失败处理器测试：失败必须让用户看得见。
 *
 * <p>
 * 只写状态栏的失败等于「点了没反应」——状态栏在左下角，很容易被忽略。这里钉住两件事： 失败回调会把原因写进状态栏，且在没有图形环境时弹窗调用不会抛异常（否则无头单测会炸）。
 */
public class UiTasksFailureTest {

    /**
     * 失败时既要弹窗（无头时降级），也要把原因留在状态栏。
     */
    @Test
    void failureHandlerWritesStatusAndSurvivesHeadless() {
        JLabel status = new JLabel("  ");

        UiTasks.Failure failure = UiTasks.failureWithDialog(null, "选课失败", status);
        failure.accept(new ApiException(StatusCode.BAD_REQUEST, "课程已满"));

        assertEquals("  课程已满", status.getText());
    }

    /**
     * 状态栏为 null 时也不能崩（部分页只有弹窗、没有状态栏）。
     */
    @Test
    void failureHandlerToleratesMissingStatusLabel() {
        UiTasks.Failure failure = UiTasks.failureWithDialog(null, "操作失败", null);

        failure.accept(new ApiException(StatusCode.INTERNAL_ERROR, "内部错误"));
    }

    /**
     * 异常没有文案时给一个兜底提示，不要弹空框。
     */
    @Test
    void failureHandlerFallsBackWhenMessageMissing() {
        JLabel status = new JLabel("  ");

        UiTasks.failureWithDialog(null, "操作失败", status)
                .accept(new ApiException(StatusCode.BAD_REQUEST, null));

        assertTrue(status.getText().indexOf("操作失败") > 0, status.getText());
    }
}
