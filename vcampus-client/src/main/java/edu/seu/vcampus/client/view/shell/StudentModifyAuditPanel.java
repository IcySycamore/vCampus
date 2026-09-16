package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.client.view.component.TableSortBinder;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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

    /** 每页条数：与学籍列表一致，一页少放几条，靠翻页看其余。 */
    private static final int PAGE_SIZE = 5;

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 筛选条（字段 / 关键词 / 状态）。 */
    private final ModifyAuditQueryBar m_filter = new ModifyAuditQueryBar();

    /** 表格模型。 */
    private final DefaultTableModel m_model = ModifyRequestTableModels.create();

    /** 表格。 */
    private final JTable m_table = new JTable(m_model);

    /** 点表头排序（排序由服务端算，这里只上报哪一列、什么方向）。 */
    private final TableSortBinder<RequestField> m_sort = new TableSortBinder<RequestField>(
            m_table, new TableSortBinder.ColumnMap<RequestField>() {
                @Override
                public RequestField fieldOf(int column) {
                    return ModifyRequestTableModels.sortFieldOf(column);
                }
            });

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
        }, PAGE_SIZE);
        m_filter.setOnQuery(new Runnable() {
            @Override
            public void run() {
                requery();
            }
        });
        m_sort.setOnSortChanged(new Runnable() {
            @Override
            public void run() {
                requery();
            }
        });
        m_sort.bind();
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(m_filter, BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        add(new StudentModifyAuditActions(m_api, this, m_pager), BorderLayout.SOUTH);
        refresh();
    }

    /** 回到第一页并按当前条件重查（改条件或换排序后都走这里）。 */
    private void requery() {
        m_pager.resetToFirstPage();
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

    /** 按筛选条、表头排序与当前页码组装查询条件。 */
    private ModifyRequestQuery currentQuery() {
        ModifyRequestQuery query =
                m_filter.toQuery(m_pager.getPageNumber(), m_pager.getPageSize());
        query.setSortBy(m_sort.getField());
        query.setDescending(m_sort.isDescending());
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
        m_filter.showEmpty(m_rows.isEmpty());
    }

    /** 表格区域。 */
    private JScrollPane createTableArea() {
        m_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiFactory.styleTable(m_table);
        m_table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    StudentModifyAuditActions.showDetail(
                            m_api, StudentModifyAuditPanel.this);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setPreferredSize(new Dimension(720, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return scroll;
    }

    /**
     * 取当前选中的申请单。
     *
     * @return 申请单；未选中返回 null
     */
    StudentModifyRequest selected() {
        int row = m_table.getSelectedRow();
        if (row < 0 || row >= m_rows.size()) {
            return null;
        }
        return m_rows.get(row);
    }

}
