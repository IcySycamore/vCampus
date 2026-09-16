package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;

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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** 学籍管理页：按条件检索学籍、分页浏览，并发起管理操作。 */
public class StudentManagePanel extends JPanel {

    /** 类别下拉的「不过滤」项。 */
    static final String ALL_CATEGORIES = "全部类别";

    /** 状态下拉的「不过滤」项。 */
    static final String ALL_STATUSES = "全部状态";

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 当前身份（决定按钮可见性）。 */
    private final Role m_role;

    /** 关键词输入框。 */
    private final JTextField m_keyword = new JTextField(12);

    /** 人员类别下拉。 */
    private final JComboBox<String> m_category = new JComboBox<String>();

    /** 在校状态下拉。 */
    private final JComboBox<String> m_status = new JComboBox<String>();

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
        });
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(createFilterBar(), BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);
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
        StudentQuery query = new StudentQuery();
        query.setKeyword(m_keyword.getText().trim());
        Object category = m_category.getSelectedItem();
        if (category != null && !ALL_CATEGORIES.equals(category)) {
            query.setPersonCategory(PersonCategory.fromDisplayName(String.valueOf(category)));
        }
        Object status = m_status.getSelectedItem();
        if (status != null && !ALL_STATUSES.equals(status)) {
            query.setStatus(CampusStatus.fromDisplayName(String.valueOf(status)));
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
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setPreferredSize(new Dimension(720, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return scroll;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(new StudentActionBar(m_api, m_role, this), BorderLayout.WEST);
        bar.add(m_pager, BorderLayout.EAST);
        return bar;
    }
}
