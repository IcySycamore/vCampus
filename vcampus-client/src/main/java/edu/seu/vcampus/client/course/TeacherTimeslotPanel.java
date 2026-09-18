package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.component.ModernTabbedPaneUI;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Timeslot;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * 教师「时间槽设置」：可用时间槽（命令 316/317）与偏好时间槽（命令 307/308）两个页签。
 *
 * <p>
 * 两者的区别写在界面上：可用时间槽是排课的硬约束（上课时间必须落在其中），偏好时间槽只是教师的愿望时段。
 */
public class TeacherTimeslotPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * 创建时间槽设置页。
     *
     * @param api 选课 API；null 时只显示提示
     */
    public TeacherTimeslotPanel(CourseService api) {
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        if (api == null) {
            JLabel hint = new JLabel("  请登录后设置时间槽");
            hint.setForeground(UiTheme.MUTED);
            add(hint, BorderLayout.NORTH);
            return;
        }
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new ModernTabbedPaneUI());
        tabs.addTab("可用时间槽", new TimeslotEditorPanel(availableGateway(api)));
        tabs.addTab("偏好时间槽", new TimeslotEditorPanel(preferenceGateway(api)));
        add(tabs, BorderLayout.CENTER);
    }

    private static TimeslotEditorPanel.Gateway availableGateway(final CourseService api) {
        return new TimeslotEditorPanel.Gateway() {
            @Override
            public List<Timeslot> load() {
                List<Timeslot> slots = api.getMyAvailableTimeslots();
                return slots == null ? new ArrayList<Timeslot>() : slots;
            }

            @Override
            public void save(List<Timeslot> slots) {
                api.setMyAvailableTimeslots(slots);
            }
        };
    }

    private static TimeslotEditorPanel.Gateway preferenceGateway(final CourseService api) {
        return new TimeslotEditorPanel.Gateway() {
            @Override
            public List<Timeslot> load() {
                List<Timeslot> slots = api.getMyPreferenceTimeslots();
                return slots == null ? new ArrayList<Timeslot>() : slots;
            }

            @Override
            public void save(List<Timeslot> slots) {
                api.setMyPreferenceTimeslots(slots);
            }
        };
    }
}
