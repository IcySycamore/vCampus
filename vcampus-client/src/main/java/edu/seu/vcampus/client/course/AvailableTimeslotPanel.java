package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.CourseScheduler;
import edu.seu.vcampus.common.course.Timeslot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * 教师「可用时间槽」界面：以「小时 : 分钟」分开输入，增删并保存本人的可用时间槽。
 *
 * <p>
 * 保存后，管理员排课时会据此校验「上课时间是否落在教师的可用时间槽内」。
 */
public class AvailableTimeslotPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAYS = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };

    private final CourseService api;
    private final DefaultListModel<String> listModel = new DefaultListModel<String>();
    private final JList<String> list = new JList<String>(listModel);
    private final JComboBox<String> weekdayBox = new JComboBox<String>(WEEKDAYS);
    private final JComboBox<String> startPeriodBox = new JComboBox<String>(periodNames());
    private final JComboBox<String> endPeriodBox = new JComboBox<String>(periodNames());
    private final JLabel statusLabel = new JLabel("  请登录后设置可用时间槽");
    private final List<Timeslot> current = new ArrayList<Timeslot>();

    /** 创建离线预览界面。 */
    public AvailableTimeslotPanel() {
        this(null);
    }

    /**
     * 创建接入选课服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     */
    public AvailableTimeslotPanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(heading(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        styleStatus();
        add(statusLabel, BorderLayout.SOUTH);
        refresh();
    }

    private JPanel heading() {
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        JLabel title = new JLabel("可用时间槽");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, UiTheme.SIZE_TITLE));
        JLabel subtitle = new JLabel("设置本人可上课的时间段，排课时将据此校验");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, UiTheme.SIZE_SUBTITLE));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        return heading;
    }

    private JPanel center() {
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(form(), BorderLayout.NORTH);
        list.setFont(UiTheme.font(Font.PLAIN, UiTheme.SIZE_BODY));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        center.add(scroll, BorderLayout.CENTER);
        return center;
    }

    private JPanel form() {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        form.setOpaque(false);
        form.add(label("星期"));
        form.add(weekdayBox);
        form.add(label("从"));
        form.add(startPeriodBox);
        form.add(label("到"));
        form.add(endPeriodBox);
        JButton addButton = UiFactory.secondaryButton("添加", "user");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addSlot();
            }
        });
        form.add(addButton);
        JButton removeButton = UiFactory.secondaryButton("删除选中", "return");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeSelected();
            }
        });
        form.add(removeButton);
        JButton saveButton = UiFactory.primaryButton("保存", "user");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        form.add(saveButton);
        return form;
    }

    private static String[] periodNames() {
        String[] names = new String[CourseScheduler.PERIODS];
        for (int i = 0; i < CourseScheduler.PERIODS; i++) {
            names[i] = CourseScheduler.periodName(i);
        }
        return names;
    }

    private void addSlot() {
        int weekday = weekdayBox.getSelectedIndex() + 1;
        int start = startPeriodBox.getSelectedIndex();
        int end = endPeriodBox.getSelectedIndex();
        if (end < start) {
            statusLabel.setText("  结束节次不能早于开始节次");
            return;
        }
        Timeslot slot = CourseScheduler.timeslotOf(weekday, start, end);
        current.add(slot);
        render();
        statusLabel.setText("  已添加 " + slot);
    }

    private void removeSelected() {
        int index = list.getSelectedIndex();
        if (index < 0) {
            statusLabel.setText("  请先在列表中选择要删除的时间段");
            return;
        }
        current.remove(index);
        render();
        statusLabel.setText("  已删除选中时间段");
    }

    private void save() {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        final List<Timeslot> snapshot = new ArrayList<Timeslot>(current);
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.setMyAvailableTimeslots(snapshot);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  可用时间槽已保存");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void refresh() {
        if (api == null) {
            statusLabel.setText("  请登录后设置可用时间槽");
            return;
        }
        UiTasks.run(new UiTasks.Task<List<Timeslot>>() {
            @Override
            public List<Timeslot> run() {
                return api.getMyAvailableTimeslots();
            }
        }, new UiTasks.Success<List<Timeslot>>() {
            @Override
            public void accept(List<Timeslot> timeslots) {
                current.clear();
                if (timeslots != null) {
                    current.addAll(timeslots);
                }
                render();
                statusLabel.setText("  当前 " + current.size() + " 个可用时间段");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void render() {
        listModel.clear();
        for (Timeslot slot : current) {
            listModel.addElement(slot.toString());
        }
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, UiTheme.SIZE_SMALL));
        return label;
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }
}
