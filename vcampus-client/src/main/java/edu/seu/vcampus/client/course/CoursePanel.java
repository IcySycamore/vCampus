package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.component.ModernTabbedPaneUI;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * 选课一级页面：把「选课与退课」和「成绩中心」两个视图合成一个页签容器。
 *
 * <p>「选课与退课」只挂给具备 {@link Capability#COURSE_SELECT} 的角色（学生/管理员），
 * 教师在这里只看得到「成绩中心」（含成绩录入，见 {@link ScorePanel}）。角色只决定
 * 控件是否出现，服务端 403 才是最终防线（见 ADR-0009 D6）。
 */
public class CoursePanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建选课页。
     *
     * @param api 选课 API；null 时各子视图回落为离线预览
     * @param role 当前身份显示名（学生/教师/管理员）
     */
    public CoursePanel(CourseService api, String role) {
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new ModernTabbedPaneUI());
        Role parsed = Role.fromDisplayName(role);
        if (Permissions.can(parsed, Capability.COURSE_SELECT)) {
            tabs.addTab("选课与退课", new CourseSelectPanel(api));
        }
        if (parsed == Role.TEACHER) {
            tabs.addTab("我的课程", new TeacherCoursePanel(api));
            tabs.addTab("偏好时间槽", new PreferencePanel(api));
        }
        if (Permissions.can(parsed, Capability.COURSE_MANAGE)) {
            tabs.addTab("排课", new SchedulePanel(api));
        }
        tabs.addTab("成绩中心", new ScorePanel(api, role));
        add(tabs, BorderLayout.CENTER);
    }
}
