package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 修改申请审核操作栏。 */
final class StudentModifyAuditActions extends JPanel {

    private static final long serialVersionUID = 1L;

    private final StudentService m_api;
    private final StudentModifyAuditPanel m_panel;
    private final JTextField m_comment = new JTextField(18);

    StudentModifyAuditActions(StudentService api, StudentModifyAuditPanel panel,
            PageBarPanel pager) {
        this.m_api = api;
        this.m_panel = panel;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(createButtons(), BorderLayout.WEST);
        add(pager, BorderLayout.EAST);
    }

    private JPanel createButtons() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(new JLabel("审核意见"));
        actions.add(m_comment);
        JButton detail = new JButton("查看详情");
        detail.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showDetail(m_api, m_panel);
            }
        });
        actions.add(detail);
        actions.add(button("通过", true));
        actions.add(button("驳回", false));
        return actions;
    }

    static void showDetail(StudentService api, StudentModifyAuditPanel panel) {
        StudentModifyRequest target = panel.selected();
        if (target == null) {
            JOptionPane.showMessageDialog(panel, "请先在表格里选中一条申请",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ModifyRequestDetailDialog.open(panel, api, target);
    }

    private JButton button(String text, final boolean approved) {
        JButton button = new JButton(text);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                audit(approved);
            }
        });
        return button;
    }

    private void audit(final boolean approved) {
        final StudentModifyRequest target = m_panel.selected();
        if (target == null) {
            warn("请先在表格里选中一条申请");
            return;
        }
        if (target.getRequestId() == null) {
            warn("该申请单没有编号");
            return;
        }
        final String requestId = target.getRequestId().toString();
        final String comment = m_comment.getText().trim();
        if (!approved && comment.length() == 0) {
            warn("驳回请填写审核意见——学生需要知道被驳回的原因");
            return;
        }
        if (approved && !confirmApprove(requestId)) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.auditModification(requestId, approved, comment);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                m_comment.setText("");
                m_panel.refresh();
            }
        });
    }

    private boolean confirmApprove(String requestId) {
        int choice = JOptionPane.showConfirmDialog(this,
                "通过申请单 #" + requestId + " 会把申请内容写入学籍，之后不能撤销。是否继续？",
                "确认通过", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.WARNING_MESSAGE);
    }
}
