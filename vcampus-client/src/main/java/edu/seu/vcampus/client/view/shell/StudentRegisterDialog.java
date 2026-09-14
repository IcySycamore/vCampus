package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
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
 * 新生登记弹窗（命令 204）：为一个已有账户登记在校档案。
 *
 * <p>
 * 学籍只存账户 uuid（跨模块统一用 uuid，见 ADR-0009 D7），所以这里要求填账户 uuid，
 * 而不是让学生自己注册——账号由用户模块先建好，学籍再挂上去。服务端会校验 uuid 是否合法。
 *
 * <p>
 * 「专业 / 研究方向」是同一个字段的两种叫法：学生填专业，教师填研究方向，因此标签写成两者。
 */
final class StudentRegisterDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 列表页；登记成功后让它重查。 */
    private final StudentManagePanel m_panel;

    /** 账户 uuid 输入框。 */
    private final JTextField m_uuid = new JTextField(24);

    /** 人员类别下拉。 */
    private final JComboBox<String> m_category = new JComboBox<String>();

    /** 年份输入框（学生为入学年份，教师为入职年份）。 */
    private final JTextField m_year = new JTextField(6);

    /** 在校状态下拉。 */
    private final JComboBox<String> m_status = new JComboBox<String>();

    /** 专业 / 研究方向输入框。 */
    private final JTextField m_field = new JTextField(16);

    /**
     * 创建登记弹窗。
     *
     * @param api 学籍 API
     * @param panel 列表页
     */
    StudentRegisterDialog(StudentService api, StudentManagePanel panel) {
        super(SwingUtilities.getWindowAncestor(panel), "新生登记",
                ModalityType.APPLICATION_MODAL);
        this.m_api = api;
        this.m_panel = panel;
        m_category.setModel(new DefaultComboBoxModel<String>(new String[] {
                PersonCategory.STUDENT.getDisplayName(), PersonCategory.TEACHER.getDisplayName() }));
        m_status.setModel(new DefaultComboBoxModel<String>(statusNames()));
        m_year.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        setLayout(new BorderLayout(0, 12));
        add(createForm(), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(panel);
    }

    /** 表单区。 */
    private JPanel createForm() {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 6, 20));
        form.add(new JLabel("账户 uuid"));
        form.add(m_uuid);
        form.add(new JLabel("人员类别"));
        form.add(m_category);
        form.add(new JLabel("年份（入学 / 入职）"));
        form.add(m_year);
        form.add(new JLabel("在校状态"));
        form.add(m_status);
        form.add(new JLabel("专业 / 研究方向"));
        form.add(m_field);
        return form;
    }

    /** 按钮区。 */
    private JPanel createButtons() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 20, 14, 20));
        JButton cancel = new JButton("取消");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        final JButton submit = new JButton("登记");
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submit();
            }
        });
        bar.add(submit);
        return bar;
    }

    /** 校验表单并提交。 */
    private void submit() {
        final StudentProfile profile = buildProfile();
        if (profile == null) {
            return;
        }
        final String shown = profile.getUserUuid();
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.registerStudent(profile);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                dispose();
                m_panel.refresh();
                JOptionPane.showMessageDialog(m_panel, "已为 " + shown + " 建立在校档案",
                        "登记成功", JOptionPane.INFORMATION_MESSAGE);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                JOptionPane.showMessageDialog(StudentRegisterDialog.this, error.getMessage(),
                        "登记失败", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * 由表单取值构造一条学籍。
     *
     * @return 学籍；表单不合法时提示并返回 null
     */
    private StudentProfile buildProfile() {
        String uuid = m_uuid.getText().trim();
        if (uuid.length() == 0) {
            warn("账户 uuid 不能为空");
            return null;
        }
        int year;
        try {
            year = Integer.parseInt(m_year.getText().trim());
        } catch (NumberFormatException e) {
            warn("年份必须是数字");
            return null;
        }
        PersonCategory category =
                PersonCategory.fromDisplayName(String.valueOf(m_category.getSelectedItem()));
        CampusStatus status =
                CampusStatus.fromDisplayName(String.valueOf(m_status.getSelectedItem()));
        if (category == null || status == null) {
            warn("请选择人员类别与在校状态");
            return null;
        }
        StudentProfile profile = new StudentProfile(uuid, category, year, status);
        profile.setField(m_field.getText().trim());
        return profile;
    }

    /** 全部在校状态的可选名。 */
    private static String[] statusNames() {
        CampusStatus[] all = CampusStatus.values();
        String[] names = new String[all.length];
        int index = 0;
        while (index < all.length) {
            names[index] = all[index].getDisplayName();
            index = index + 1;
        }
        return names;
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
