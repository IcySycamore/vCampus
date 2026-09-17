package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
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
import javax.swing.JTextField;

/**
 * 教师「偏好时间槽」界面：查看、增删并保存本人的偏好时间槽。学生不具备该能力，界面不会挂载。
 */
public class PreferencePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final CourseService api;
    private final DefaultListModel<String> listModel = new DefaultListModel<String>();
    private final JList<String> list = new JList<String>(listModel);
    private final JComboBox<String> weekdayBox = new JComboBox<String>(WEEKDAYS);
    private final JTextField startField = new JTextField(3);
    private final JTextField endField = new JTextField(3);
    private final JLabel statusLabel = new JLabel("  请登录后设置偏好时间槽");
    private final List<Timeslot> current = new ArrayList<Timeslot>();

    /** 创建离线预览界面。 */
    public PreferencePanel() {
        this(null);
    }

    /**
     * 创建接入选课服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     */
    public PreferencePanel(CourseService api) {
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
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("偏好时间槽");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        heading.add(title, BorderLayout.WEST);
        return heading;
    }

    private JPanel center() {
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(form(), BorderLayout.NORTH);
        list.setFont(UiTheme.font(Font.PLAIN, 15F));
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
        form.add(label("开始(时)"));
        form.add(startField);
        form.add(label("结束(时)"));
        form.add(endField);
        JButton addButton = UiFactory.secondaryButton("添加", "user");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addSlot();
            }
        });
        form.add(addButton);
        JButton saveButton = UiFactory.primaryButton("保存偏好", "user");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        form.add(saveButton);
        return form;
    }

    private void addSlot() {
        try {
            int weekday = weekdayBox.getSelectedIndex() + 1;
            int start = Integer.parseInt(startField.getText().trim());
            int end = Integer.parseInt(endField.getText().trim());
            Timeslot slot = new Timeslot(weekday, start * 60, end * 60);
            current.add(slot);
            render();
            statusLabel.setText("  已添加 " + slot);
        } catch (RuntimeException exception) {
            statusLabel.setText("  时间格式不正确，请按 0-24 小时填写");
        }
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
                api.setMyPreferenceTimeslots(snapshot);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  偏好时间槽已保存");
            }
        });
    }

    private void refresh() {
        if (api == null) {
            statusLabel.setText("  请登录后设置偏好时间槽");
            return;
        }
        UiTasks.run(new UiTasks.Task<List<Timeslot>>() {
            @Override
            public List<Timeslot> run() {
                return api.getMyPreferenceTimeslots();
            }
        }, new UiTasks.Success<List<Timeslot>>() {
            @Override
            public void accept(List<Timeslot> timeslots) {
                current.clear();
                if (timeslots != null) {
                    current.addAll(timeslots);
                }
                render();
                statusLabel.setText("  当前 " + current.size() + " 个偏好时间槽");
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
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        return label;
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }
}
