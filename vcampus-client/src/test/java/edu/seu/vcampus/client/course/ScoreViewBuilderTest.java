package edu.seu.vcampus.client.course;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成绩界面视图构建测试。
 */
public class ScoreViewBuilderTest {

    /**
     * 学生与教师视角都应能构建标题区。
     */
    @Test
    void buildsHeadingForStudentAndTeacher() {
        assertNotNull(ScoreViewBuilder.heading(true));
        assertNotNull(ScoreViewBuilder.heading(false));
    }

    /**
     * 内容区应包含工具栏与表格滚动区。
     */
    @Test
    void buildsContentWithTable() {
        JPanel content = ScoreViewBuilder.content(new JTable(), new JPanel());

        assertNotNull(content);
        assertTrue(content.getComponentCount() >= 2);
    }

    /**
     * 学生视角工具栏应包含搜索与刷新。
     */
    @Test
    void buildsStudentToolbar() {
        ScorePanel panel = new ScorePanel();
        JPanel toolbar = ScoreViewBuilder.toolbar(true, new JTextField(), new JTextField(),
                new JTextField(), new JTextField(), panel.controller);

        assertNotNull(toolbar);
        assertTrue(toolbar.getComponentCount() >= 4);
    }

    /**
     * 教师视角工具栏应额外包含成绩录入表单。
     */
    @Test
    void buildsTeacherToolbarWithForm() {
        ScorePanel panel = new ScorePanel("教师");
        JPanel toolbar = ScoreViewBuilder.toolbar(false, new JTextField(), new JTextField(),
                new JTextField(), new JTextField(), panel.controller);

        assertNotNull(toolbar);
        assertTrue(toolbar.getComponentCount() >= 5);
    }

    /**
     * 底部应包含状态栏与 GPA 展示区。
     */
    @Test
    void buildsFooterWithLabels() {
        JPanel footer = ScoreViewBuilder.footer(new JLabel(), new JLabel());

        assertNotNull(footer);
        assertTrue(footer.getComponentCount() >= 2);
    }
}
