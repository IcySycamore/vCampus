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
 * 选课一级页面：按角色把该角色**真正能用的**子视图挂成页签。
 *
 * <p>
 * 页签与服务端命令一一对应，避免「有界面没后端」或「有后端没入口」：
 * <ul>
 * <li>学生：选课与退课（300/301/302/315）、我的课表（315）、成绩中心（303）。</li>
 * <li>教师：我的课程（305/313）、我的课表、时间槽设置（307/308/316/317）、成绩中心（303/304）。</li>
 * <li>管理员：课程目录（310/311/312）、排课（306/309/314/318）、成绩中心。</li>
 * </ul>
 *
 * <p>
 * 角色只决定控件是否出现，服务端 403 才是最终防线（见 ADR-0009 D6）。
 */
public class CoursePanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建选课页。
     *
     * @param api  选课 API；null 时各子视图回落为离线预览
     * @param role 当前身份显示名（学生/教师/管理员）
     */
    public CoursePanel(CourseService api, String role) {
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new ModernTabbedPaneUI());
        Role parsed = Role.fromDisplayName(role);
        if (parsed == Role.STUDENT) {
            tabs.addTab("选课与退课", new CourseSelectPanel(api));
            tabs.addTab("我的课表", new StudentTimetablePanel(api));
        }
        if (parsed == Role.TEACHER) {
            tabs.addTab("我的课程", new TeacherCoursePanel(api));
            tabs.addTab("我的课表", new TeacherTimetablePanel(api));
            tabs.addTab("时间槽设置", new TeacherTimeslotPanel(api));
        }
        if (Permissions.can(parsed, Capability.COURSE_MANAGE)) {
            tabs.addTab("课程目录", new CourseAdminPanel(api));
            tabs.addTab("排课", new ScheduleGridPanel(api));
        }
        tabs.addTab("成绩中心", new ScorePanel(api, role));
        add(tabs, BorderLayout.CENTER);
    }
}
