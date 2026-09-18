package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.CourseScheduler;
import edu.seu.vcampus.common.course.Timeslot;

import java.awt.BorderLayout;
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
 * 「一组时间槽」的编辑器：列表 + 星期/起止节次 + 添加 / 删除选中 / 保存 / 刷新。
 *
 * <p>
 * 教师有两组语义不同的时间槽（可用：排课必须落在其中；偏好：希望被排在其中的时段），原来只有「可用」有界面， 偏好时间槽（命令 307/308）没有任何入口。两组共用本类，只是存取通道不同。
 */
final class TimeslotEditorPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAYS = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };

    /** 一组时间槽的存取通道。 */
    interface Gateway {

        /** @return 当前时间槽 */
        List<Timeslot> load();

        /** @param slots 待保存的时间槽 */
        void save(List<Timeslot> slots);
    }

    private final Gateway gateway;
    private final DefaultListModel<String> listModel = new DefaultListModel<String>();
    private final JList<String> list = new JList<String>(listModel);
    private final JComboBox<String> weekdayBox = new JComboBox<String>(WEEKDAYS);
    private final JComboBox<String> startPeriodBox = new JComboBox<String>(periodNames());
    private final JComboBox<String> endPeriodBox = new JComboBox<String>(periodNames());
    private final JLabel statusLabel = new JLabel("  ");
    private final List<Timeslot> current = new ArrayList<Timeslot>();

    TimeslotEditorPanel(Gateway gateway) {
        this.gateway = gateway;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 0, 18));
        add(form(), BorderLayout.NORTH);
        list.setFont(UiTheme.font(Font.PLAIN, UiTheme.SIZE_BODY));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        add(scroll, BorderLayout.CENTER);
        CourseViewBuilder.styleStatus(statusLabel);
        add(statusLabel, BorderLayout.SOUTH);
        refresh();
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
        JButton refreshButton = UiFactory.secondaryButton("刷新", "refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refresh();
            }
        });
        form.add(refreshButton);
        return form;
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
        for (Timeslot existing : current) {
            if (existing.overlaps(slot)) {
                statusLabel.setText("  该时间段与已添加的 " + existing + " 重叠");
                return;
            }
        }
        current.add(slot);
        render();
        statusLabel.setText("  已添加 " + slot + "（别忘了点保存）");
    }

    private void removeSelected() {
        int index = list.getSelectedIndex();
        if (index < 0) {
            statusLabel.setText("  请先在列表中选择要删除的时间段");
            return;
        }
        current.remove(index);
        render();
        statusLabel.setText("  已删除选中时间段（别忘了点保存）");
    }

    private void save() {
        final List<Timeslot> snapshot = new ArrayList<Timeslot>(current);
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                gateway.save(snapshot);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  已保存 " + snapshot.size() + " 个时间段");
            }
        }, UiTasks.failureWithDialog(this, "时间槽操作失败", statusLabel));
    }

    /** 重新从服务端读取。 */
    void refresh() {
        UiTasks.run(new UiTasks.Task<List<Timeslot>>() {
            @Override
            public List<Timeslot> run() {
                return gateway.load();
            }
        }, new UiTasks.Success<List<Timeslot>>() {
            @Override
            public void accept(List<Timeslot> slots) {
                current.clear();
                if (slots != null) {
                    current.addAll(slots);
                }
                render();
                statusLabel.setText("  当前 " + current.size() + " 个时间段");
            }
        }, UiTasks.failureWithDialog(this, "时间槽操作失败", statusLabel));
    }

    private void render() {
        listModel.clear();
        for (Timeslot slot : current) {
            listModel.addElement(slot.toString());
        }
    }

    private static String[] periodNames() {
        String[] names = new String[CourseScheduler.PERIODS];
        for (int i = 0; i < CourseScheduler.PERIODS; i++) {
            names[i] = CourseScheduler.periodName(i);
        }
        return names;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        return label;
    }
}
