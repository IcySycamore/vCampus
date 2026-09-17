package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.client.view.component.TableSortBinder;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentField;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** 学籍管理页：按条件检索学籍、分页浏览，并发起管理操作。 */
public class StudentManagePanel extends JPanel {

    /** 每页条数。 */
    public static final int PAGE_SIZE = 5;

    /** 可写角色的页签名。 */
    private static final String TITLE_MANAGE = "学籍管理";

    /** 只读角色的页签名。 */
    private static final String TITLE_QUERY = "学籍查询";

    /**
     * 页签标题：能改的才叫「学籍管理」，只读的只能叫「学籍查询」。
     *
     * <p>
     * 名字跟着权限走，不是抠字眼：一个只能查不能改的页签叫「管理」，会让人以为找错了地方，
     * 也会让人怀疑自己是不是缺权限。判定放在这里而不是各个调用点，是为了将来加角色时只需改一处。
     *
     * @param role 当前身份；null 视为只读
     * @return 页签标题
     */
    public static String tabTitle(Role role) {
        return writable(role) ? TITLE_MANAGE : TITLE_QUERY;
    }

    /**
     * 该身份在本页能否写（登记 / 改状态 / 注销）。
     *
     * <p>
     * 三项任一为真就算可写：管理员三项全有，教师一项也没有。不写成「三项全有才算」是因为
     * 将来若出现只会注销的人（比如宿管），页签也该叫管理。
     *
     * @param role 身份
     * @return 可写返回 true
     */
    private static boolean writable(Role role) {
        return Permissions.can(role, Capability.STUDENT_REGISTER)
                || Permissions.can(role, Capability.STUDENT_CHANGE_STATUS)
                || Permissions.can(role, Capability.STUDENT_DELETE);
    }

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

    /**
     * 点表头排序。
     *
     * <p>
     * 挂在表格上而不是另做一个下拉：用户看到一列数据想「按它排」时，手自然会去点那一列的表头。
     * 排序结果由服务端算（列表是分页的，本地只能排当前页），这里只上报「哪一列、什么方向」。
     */
    private final TableSortBinder<StudentField> m_sort = new TableSortBinder<StudentField>(
            m_table, new TableSortBinder.ColumnMap<StudentField>() {
                @Override
                public StudentField fieldOf(int column) {
                    return StudentTableModels.sortFieldOf(column);
                }
            });

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
        m_sort.setOnSortChanged(new Runnable() {
            @Override
            public void run() {
                // 换了排序字段就得回到第一页：停在第 3 页看新顺序的第一屏没有意义
                requery();
            }
        });
        m_sort.bind();
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

    private StudentQuery currentQuery() {
        StudentQuery query = m_filter.toQuery(m_pager.getPageNumber(), m_pager.getPageSize());
        query.setSortBy(m_sort.getField());
        query.setDescending(m_sort.isDescending());
        return query;
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

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);
        bar.add(new JLabel("关键词"));
        bar.add(m_keyword);
        m_category.setModel(new DefaultComboBoxModel<String>(new String[] {
                ALL_CATEGORIES, PersonCategory.STUDENT.getDisplayName(),
                PersonCategory.TEACHER.getDisplayName() }));
        bar.add(m_category);
        m_status.setModel(new DefaultComboBoxModel<String>(new String[] { ALL_STATUSES,
                CampusStatus.ENROLLED.getDisplayName(), CampusStatus.SUSPENDED.getDisplayName(),
                CampusStatus.WITHDRAWN.getDisplayName(), CampusStatus.GRADUATED.getDisplayName(),
                CampusStatus.RETIRED.getDisplayName() }));
        bar.add(m_status);
        JButton search = UiFactory.primaryButton("查询", "search");
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

    private JScrollPane createTableArea() {
        m_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiFactory.styleTable(m_table);
        // 双击一行看详情：表格列宽就那么大，「专业 / 研究方向」会被截成「计算机科学与…」，
        // 而这一格恰恰是查人时最常要看的。与「修改审核」页的双击行为保持一致。
        m_table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showDetail();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setPreferredSize(new Dimension(720, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return scroll;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(createRowActions(), BorderLayout.WEST);
        bar.add(m_pager, BorderLayout.EAST);
        return bar;
    }

    /**
     * 左侧按钮区：先是永远可用的「查看详情」，再接按能力显示的写操作。
     *
     * <p>
     * 「查看详情」不放进 {@link StudentActionBar}：那里是按能力决定出不出现的写操作，而查看是只读的，
     * 教师也必须能点。两件事混在一起，早晚会出现「为了给教师看详情而给他开了写权限」。
     *
     * @return 按钮区
     */
    private JPanel createRowActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        JButton detail = new JButton("查看详情");
        detail.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showDetail();
            }
        });
        actions.add(detail);
        actions.add(new StudentActionBar(m_api, m_role, this));
        return actions;
    }
}
