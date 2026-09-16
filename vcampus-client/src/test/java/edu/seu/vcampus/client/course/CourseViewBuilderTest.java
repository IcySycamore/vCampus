package edu.seu.vcampus.client.course;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 选课界面视图构建测试。
 */
public class CourseViewBuilderTest {

    /**
     * 标题区应能构建。
     */
    @Test
    void buildsHeading() {
        assertNotNull(CourseViewBuilder.heading());
    }

    /**
     * 内容区应包含工具栏与表格滚动区。
     */
    @Test
    void buildsContentWithTable() {
        JPanel content = CourseViewBuilder.content(new JTable(), new JPanel());

        assertNotNull(content);
        assertTrue(content.getComponentCount() >= 2);
    }

    /**
     * 工具栏应包含搜索、刷新、选课与退课按钮。
     */
    @Test
    void buildsToolbarWithButtons() {
        CourseSelectPanel panel = new CourseSelectPanel();
        JPanel toolbar = CourseViewBuilder.toolbar(new JTextField(), panel.controller);

        assertNotNull(toolbar);
        assertTrue(toolbar.getComponentCount() >= 6);
    }

    /**
     * 状态标签样式设置后应为不透明。
     */
    @Test
    void stylesStatusLabel() {
        JLabel label = new JLabel("状态");
        CourseViewBuilder.styleStatus(label);

        assertTrue(label.isOpaque());
    }
}

