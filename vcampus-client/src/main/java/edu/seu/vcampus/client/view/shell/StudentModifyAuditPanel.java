package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 修改审核页（教师 / 管理员）：列出学籍修改申请并通过或驳回（命令 207 查询、203 审核）。
 *
 * <p>
 * 默认过滤「待审核」——这个页面的日常用法就是清待办，看历史申请是偶尔为之，所以更常用的
 * 那一种设为默认值，少点一次。
 *
 * <p>
 * <b>通过会真正改学籍</b>：服务端在 203 通过时才把申请里的字段写进 {@code StudentProfile}，
 * 所以「通过」前先确认一次，避免误点造成数据变更；驳回则要求填写意见，学生要知道原因。
 */
public class StudentModifyAuditPanel extends JPanel {

    /** 状态下拉的「不过滤」项。 */
    private static final String ALL_STATUSES = "全部状态";

    /** 过滤栏说明（有待审申请时显示）。 */
    private static final String DEFAULT_HINT = "默认只看待审；通过会把申请内容真正写入学籍";

    /** 过滤栏说明（一条都没有时显示）：区分「没人提」与「这条链路坏了」。 */
    private static final String EMPTY_HINT =
            "没有符合条件的申请：学生在「我的档案」点「申请修改」提交后才会出现在这里";

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 过滤栏就地说明。 */
    private final JLabel m_hint = new JLabel(DEFAULT_HINT);

    /** 状态过滤下拉。 */
    private final JComboBox<String> m_status_filter = new JComboBox<String>();

    /** 审核意见输入框。 */
    private final JTextField m_comment = new JTextField(18);

    /** 表格模型。 */
    private final DefaultTableModel m_model = ModifyRequestTableModels.create();

    /** 表格。 */
    private final JTable m_table = new JTable(m_model);

    /** 当前页的申请单；与表格行号一一对应。 */
    private final List<StudentModifyRequest> m_rows = new ArrayList<StudentModifyRequest>();

    /** 分页栏。 */
    private final PageBarPanel m_pager;

    /**
     * 创建审核页，并立即查询待审申请。
     *
     * @param api 学籍 API
     * @throws IllegalArgumentException api 为 null
     */
    public StudentModifyAuditPanel(final StudentService api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        this.m_api = api;
        this.m_pager = new PageBarPanel(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(createFilterBar(), BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);
        refresh();
    }

    /** 按当前条件重新查询并回填。 */
    public final void refresh() {
        final ModifyRequestQuery query = currentQuery();
        UiTasks.run(new UiTasks.Task<PageResponse<StudentModifyRequest>>() {
            @Override
            public PageResponse<StudentModifyRequest> run() {
                return m_api.listModifyRequests(query);
            }
        }, new UiTasks.Success<PageResponse<StudentModifyRequest>>() {
            @Override
            public void accept(PageResponse<StudentModifyRequest> page) {
                fill(page);
            }
        });
    }

    /** 按控件当前取值组装查询条件。 */
    private ModifyRequestQuery currentQuery() {
        ModifyRequestQuery query = new ModifyRequestQuery();
        Object status = m_status_filter.getSelectedItem();
        if (status != null && !ALL_STATUSES.equals(status)) {
            query.setStatus(ModifyRequestStatus.fromDisplayName(String.valueOf(status)));
        }
        query.setPageNumber(m_pager.getPageNumber());
        query.setPageSize(m_pager.getPageSize());
        return query;
    }

    /**
     * 回填表格与分页栏。
     *
     * @param page 服务端分页结果；null 视为空
     */
    private void fill(PageResponse<StudentModifyRequest> page) {
        m_rows.clear();
        if (page != null) {
            m_rows.addAll(page.getItems());
        }
        ModifyRequestTableModels.fill(m_model, m_rows);
        m_pager.sync(page);
        m_hint.setText(m_rows.isEmpty() ? EMPTY_HINT : DEFAULT_HINT);
    }

    /** 过滤栏：状态 + 查询。 */
    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);
        bar.add(new JLabel("状态"));
        m_status_filter.setModel(new DefaultComboBoxModel<String>(new String[] {
                ModifyRequestStatus.PENDING.getDisplayName(), ALL_STATUSES,
                ModifyRequestStatus.APPROVED.getDisplayName(),
                ModifyRequestStatus.REJECTED.getDisplayName() }));
        bar.add(m_status_filter);
        JButton search = UiFactory.primaryButton("查询", "search");
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                m_pager.resetToFirstPage();
                refresh();
            }
        });
        bar.add(search);
        m_hint.setForeground(UiTheme.MUTED);
        bar.add(m_hint);
        return bar;
    }

    /** 表格区域。 */
    private JScrollPane createTableArea() {
        m_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiFactory.styleTable(m_table);
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setPreferredSize(new Dimension(720, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return scroll;
    }

    /** 底部：审核意见 + 通过 / 驳回，右侧分页栏。 */
    private JPanel createActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(new JLabel("审核意见"));
        actions.add(m_comment);
        actions.add(button("通过", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                audit(true);
            }
        }));
        actions.add(button("驳回", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                audit(false);
            }
        }));
        bar.add(actions, BorderLayout.WEST);
        bar.add(m_pager, BorderLayout.EAST);
        return bar;
    }

    /**
     * 审核选中的申请。
     *
     * @param approved true 通过、false 驳回
     */
    private void audit(final boolean approved) {
        final StudentModifyRequest target = selected();
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
                refresh();
            }
        });
    }

    /**
     * 通过前确认。
     *
     * @param requestId 申请单编号
     * @return 用户是否确认
     */
    private boolean confirmApprove(String requestId) {
        int choice = JOptionPane.showConfirmDialog(this,
                "通过申请单 #" + requestId + " 会把申请内容写入学籍，之后不能撤销。是否继续？",
                "确认通过", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    /**
     * 取当前选中的申请单。
     *
     * @return 申请单；未选中返回 null
     */
    private StudentModifyRequest selected() {
        int row = m_table.getSelectedRow();
        if (row < 0 || row >= m_rows.size()) {
            return null;
        }
        return m_rows.get(row);
    }

    /**
     * 造一个次要按钮。
     *
     * @param text 文案
     * @param listener 点击回调
     * @return 按钮
     */
    private static JButton button(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        return button;
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
