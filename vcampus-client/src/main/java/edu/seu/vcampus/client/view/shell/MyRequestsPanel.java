package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 我的申请页（学生）：查看自己提交的学籍修改申请走到了哪一步（命令 207，服务端按会话收窄）。
 *
 * <p>
 * 与 {@link StudentModifyAuditPanel} 的分工是「提的人看结果、批的人看待办」：那边默认只看待审，
 * 这边默认看全部——学生关心的是「我上次那条到底过了没有」，不是还有多少条在处理。
 *
 * <p>
 * 本页只读：申请提交成功不等于学籍变了，学籍要等教务通过才动，改学籍仍走「我的档案」。 通过之后这里状态变成「已通过」，回「我的档案」点刷新就能看到新学籍。
 */
final class MyRequestsPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 状态下拉的「不过滤」项。 */
    private static final String ALL_STATUSES = "全部状态";

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 状态过滤下拉。 */
    private final JComboBox<String> m_status_filter = new JComboBox<String>();

    /** 表格模型。 */
    private final DefaultTableModel m_model = ModifyRequestTableModels.create();

    /** 表格。 */
    private final JTable m_table = new JTable(m_model);

    /** 当前页的申请单；与表格行号一一对应。 */
    private final List<StudentModifyRequest> m_rows = new ArrayList<StudentModifyRequest>();

    /** 就地提示（空列表、查询失败都写这里，不弹窗）。 */
    private final JLabel m_hint = new JLabel(" ");

    /** 分页栏。 */
    private final PageBarPanel m_pager;

    /**
     * 创建我的申请页，并立即查询一次。
     *
     * @param api 学籍 API
     * @throws IllegalArgumentException api 为 null
     */
    MyRequestsPanel(final StudentService api) {
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
        add(createFooter(), BorderLayout.SOUTH);
        refresh();
    }

    /** 按当前条件重新查询并回填。 */
    void refresh() {
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
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                m_rows.clear();
                ModifyRequestTableModels.fill(m_model, m_rows);
                m_pager.sync(null);
                m_hint.setText("查询失败：" + error.getMessage());
            }
        });
    }

    /**
     * 按控件当前取值组装查询条件。
     *
     * <p>
     * 刻意不设申请人：服务端按会话收窄，这里设了也会被覆盖；与其留一个看起来有用的假开关， 不如不写。
     *
     * @return 查询条件
     */
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
     * 回填表格与分页栏，并给出空列表的说明。
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
        m_hint.setText(m_rows.isEmpty()
                ? "还没有提交过修改申请；在「我的档案」里点「申请修改」即可提交"
                : " ");
    }

    /** 过滤栏：状态 + 刷新。 */
    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);
        bar.add(new JLabel("状态"));
        m_status_filter.setModel(new DefaultComboBoxModel<String>(new String[] {
                ALL_STATUSES, ModifyRequestStatus.PENDING.getDisplayName(),
                ModifyRequestStatus.APPROVED.getDisplayName(),
                ModifyRequestStatus.REJECTED.getDisplayName() }));
        bar.add(m_status_filter);
        JButton search = UiFactory.primaryButton("刷新", "search");
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                m_pager.resetToFirstPage();
                refresh();
            }
        });
        bar.add(search);
        return bar;
    }

    /** 表格区域。 */
    private JScrollPane createTableArea() {
        m_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiFactory.styleTable(m_table);
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setPreferredSize(new Dimension(760, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return scroll;
    }

    /** 底部：提示 + 分页栏。 */
    private JPanel createFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        m_hint.setForeground(UiTheme.MUTED);
        bar.add(m_hint, BorderLayout.WEST);
        bar.add(m_pager, BorderLayout.EAST);
        return bar;
    }
}
