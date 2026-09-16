package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 学籍管理页（教师 / 管理员）：按条件检索学籍、分页浏览，并发起管理操作。
 *
 * <p>
 * 本类只负责「查询 + 列表 + 分页」：过滤条件由 {@link StudentQueryBar} 提供（含关键词、人员类别、
 * 在校状态），三个写操作（新生登记 / 修改状态 / 注销）在 {@link StudentActionBar} 里。拆成三块
 * 是为了每块都能一眼读完，也为了「控件 → 查询条件」这段映射能不建界面就单测。
 *
 * <p>
 * <b>每页 5 条</b>：学籍列表的行数增长很快，一页铺满会让「一共多少人、我在第几页」变得不可见；
 * 少放几条、把余下的交给翻页，反而更容易看清结果集的规模。
 *
 * <p>
 * 权限只是「显示与否」：本页整体要求 {@code STUDENT_VIEW_ALL}（由调用方判定后才构造），
 * 单个按钮再各自判能力（教师因此只看得到列表、看不到任何按钮）；真正的准入在服务端，越权一律回 403。
 */
public class StudentManagePanel extends JPanel {

    /** 每页条数。 */
    public static final int PAGE_SIZE = 5;

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 当前身份（决定按钮可见性）。 */
    private final Role m_role;

    /** 筛选条。 */
    private final StudentQueryBar m_filter = new StudentQueryBar();

    /** 表格模型。 */
    private final DefaultTableModel m_model = StudentTableModels.create();

    /** 表格。 */
    private final JTable m_table = new JTable(m_model);

    /** 当前页的行实体；与表格行号一一对应，用于「选中哪一行」。 */
    private final List<StudentProfile> m_rows = new ArrayList<StudentProfile>();

    /** 分页栏。 */
    private final PageBarPanel m_pager;

    /**
     * 创建学籍管理页，并立即查询第一页。
     *
     * @param api 学籍 API
     * @param role 当前身份；null 视为无任何能力
     * @throws IllegalArgumentException api 为 null
     */
    public StudentManagePanel(final StudentService api, Role role) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        this.m_api = api;
        this.m_role = role;
        this.m_pager = new PageBarPanel(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }, PAGE_SIZE);
        // 查询与重置都要回到第一页：条件变了之后还停在第 3 页，会看到一半的旧结果集
        m_filter.setOnQuery(new Runnable() {
            @Override
            public void run() {
                requery();
            }
        });
        m_filter.setOnReset(new Runnable() {
            @Override
            public void run() {
                requery();
            }
        });
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(m_filter, BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);
        refresh();
    }

    /** 回到第一页并按当前条件重查。 */
    private void requery() {
        m_pager.resetToFirstPage();
        refresh();
    }

    /** 按当前条件重新查询并回填表格。 */
    public final void refresh() {
        final StudentQuery query = currentQuery();
        UiTasks.run(new UiTasks.Task<PageResponse<StudentProfile>>() {
            @Override
            public PageResponse<StudentProfile> run() {
                return m_api.listStudents(query);
            }
        }, new UiTasks.Success<PageResponse<StudentProfile>>() {
            @Override
            public void accept(PageResponse<StudentProfile> page) {
                fill(page);
            }
        });
    }

    /**
     * 取当前选中的学籍。
     *
     * @return 选中的学籍；未选中返回 null
     */
    StudentProfile selected() {
        int row = m_table.getSelectedRow();
        if (row < 0 || row >= m_rows.size()) {
            return null;
        }
        return m_rows.get(row);
    }

    /** 按筛选条当前取值与分页栏当前页码组装查询条件。 */
    private StudentQuery currentQuery() {
        return m_filter.toQuery(m_pager.getPageNumber(), m_pager.getPageSize());
    }

    /**
     * 回填表格与分页栏。
     *
     * @param page 服务端分页结果；null 视为空
     */
    private void fill(PageResponse<StudentProfile> page) {
        m_rows.clear();
        if (page != null) {
            m_rows.addAll(page.getItems());
        }
        StudentTableModels.fill(m_model, m_rows);
        m_pager.sync(page);
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

    /** 底部：左侧按能力显示的操作按钮，右侧分页栏。 */
    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(new StudentActionBar(m_api, m_role, this), BorderLayout.WEST);
        bar.add(m_pager, BorderLayout.EAST);
        return bar;
    }
}
