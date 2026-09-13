package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 学籍修改申请弹窗（命令 202），学生用。
 *
 * <p>
 * 三个输入框都用当前档案预填，<b>只有真正改动过的字段才会进申请单</b>——免得审核页出现一条
 * 「改了又好像没改」的记录；三项都没动就不让提交。
 *
 * <p>
 * 提交成功后学籍<b>不会立刻变</b>：服务端只落一条待审记录，教务在 203 通过后才会写回学籍，
 * 所以提示语是「已提交，等待教务审核」而不是「已修改」。
 *
 * <p>
 * 字段名取自 {@code StudentModifyRequest.FIELD_*}（双端共用的白名单常量）。在校状态传的是
 * <b>枚举名</b>（{@code SUSPENDED}）而不是界面上的中文：服务端按枚举名解析，中文只用于显示。
 */
final class StudentModifyApplyDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 申请对象：已加载的本人档案。 */
    private final StudentProfile m_profile;

    /** 学术方向输入框（学生是专业，教师是研究方向，这里只有学生用得到）。 */
    private final JTextField m_field = new JTextField(16);

    /** 入校年份输入框。 */
    private final JTextField m_year = new JTextField(6);

    /** 在校状态下拉：显示中文，提交时换成枚举名。 */
    private final JComboBox<String> m_status = new JComboBox<String>();

    /** 申请理由输入框。 */
    private final JTextField m_reason = new JTextField(24);

    /**
     * 创建申请弹窗。
     *
     * @param api     学籍 API
     * @param profile 本人档案（需要已有主键）
     * @param owner   父组件，用于定位窗口
     */
    StudentModifyApplyDialog(StudentService api, StudentProfile profile, Component owner) {
        super(SwingUtilities.getWindowAncestor(owner), "申请修改学籍",
                ModalityType.APPLICATION_MODAL);
        this.m_api = api;
        this.m_profile = profile;
        m_field.setText(orEmpty(profile.getField()));
        m_year.setText(String.valueOf(profile.getJoinYear()));
        m_status.setModel(new DefaultComboBoxModel<String>(statusNames()));
        if (profile.getStatus() == null) {
            // 档案没有状态时不预选：预选会让「没动过」被误判成一次变更。
            m_status.setSelectedIndex(-1);
        } else {
            m_status.setSelectedItem(profile.getStatus().getDisplayName());
        }
        setLayout(new BorderLayout(0, 12));
        add(createForm(), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 组装申请单里的「字段名 → 新值」，只保留真正改动过的项。
     *
     * <p>
     * 纯函数：不碰界面、不发请求，改哪几项、值怎么写全在这里，因此可以直接单测。
     *
     * @param current 当前档案
     * @param field   界面上的学术方向
     * @param year    界面上的年份文本
     * @param status  选中的在校状态<b>枚举名</b>；空串表示未选
     * @return 要提交的变更；没有改动时为空 Map
     */
    static Map<String, String> changesOf(StudentProfile current, String field, String year,
            String status) {
        Map<String, String> changes = new LinkedHashMap<String, String>();
        if (current == null) {
            return changes;
        }
        String newField = field == null ? "" : field.trim();
        if (newField.length() > 0 && !newField.equals(orEmpty(current.getField()))) {
            changes.put(StudentModifyRequest.FIELD_FIELD, newField);
        }
        String newYear = year == null ? "" : year.trim();
        if (newYear.length() > 0 && !newYear.equals(String.valueOf(current.getJoinYear()))) {
            changes.put(StudentModifyRequest.FIELD_JOIN_YEAR, newYear);
        }
        String oldStatus = current.getStatus() == null ? "" : current.getStatus().name();
        String newStatus = status == null ? "" : status;
        if (newStatus.length() > 0 && !newStatus.equals(oldStatus)) {
            changes.put(StudentModifyRequest.FIELD_STATUS, newStatus);
        }
        return changes;
    }

    /** @return 表单区 */
    private JPanel createForm() {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(14, 18, 4, 18));
        form.add(new JLabel("学术方向"));
        form.add(m_field);
        form.add(new JLabel("入校年份"));
        form.add(m_year);
        form.add(new JLabel("在校状态"));
        form.add(m_status);
        form.add(new JLabel("申请理由（必填）"));
        form.add(m_reason);
        JLabel hint = new JLabel("没动的字段不会提交；审核通过后才真正生效。");
        form.add(hint);
        form.add(new JLabel(""));
        return form;
    }

    /** @return 按钮区 */
    private JPanel createButtons() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancel = new JButton("取消");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        bar.add(cancel);
        JButton submit = new JButton("提交申请");
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submit();
            }
        });
        bar.add(submit);
        return bar;
    }

    /** 校验并提交申请。 */
    private void submit() {
        if (m_profile == null || m_profile.getId() == null) {
            warn("这份档案还没有主键，无法提交申请");
            return;
        }
        final String reason = m_reason.getText().trim();
        if (reason.length() == 0) {
            warn("请填写申请理由——教务要据此判断批不批");
            return;
        }
        CampusStatus status = statusOf(m_status.getSelectedItem());
        final Map<String, String> changes = changesOf(m_profile, m_field.getText(),
                m_year.getText(), status == null ? "" : status.name());
        if (changes.isEmpty()) {
            warn("没有改动任何字段，不需要提交申请");
            return;
        }
        if (changes.containsKey(StudentModifyRequest.FIELD_JOIN_YEAR)
                && !isInteger(m_year.getText().trim())) {
            warn("入校年份要填整数");
            return;
        }
        final StudentModifyRequest request =
                new StudentModifyRequest(m_profile.getId(), changes, reason);
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.applyModification(request);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                JOptionPane.showMessageDialog(StudentModifyApplyDialog.this,
                        "已提交，等待教务审核。", "已提交", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        });
    }

    /** @return 全部在校状态的显示名 */
    private static String[] statusNames() {
        CampusStatus[] values = CampusStatus.values();
        String[] names = new String[values.length];
        int index = 0;
        while (index < values.length) {
            names[index] = values[index].getDisplayName();
            index = index + 1;
        }
        return names;
    }

    /**
     * 显示名 → 枚举。选项为空（未选）时返回 null。
     *
     * @param item 下拉选中项
     * @return 对应枚举；无法识别时 null
     */
    private static CampusStatus statusOf(Object item) {
        if (item == null) {
            return null;
        }
        return CampusStatus.fromDisplayName(String.valueOf(item));
    }

    /**
     * 文本非空判断。
     *
     * @param value 原值
     * @return 原值；null 时为空串
     */
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 是否十进制整数。
     *
     * @param text 文本
     * @return 能否用 int 解析
     */
    private static boolean isInteger(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * 提示一条信息。
     *
     * @param message 提示文本
     */
    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.WARNING_MESSAGE);
    }
}
