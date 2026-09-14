package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.user.BatchProgressListener;
import edu.seu.vcampus.client.user.UserAdminService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.entity.User;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 用户管理面板（管理轨，需 {@code USER_MANAGE}）：筛选 + 分页表格 + 动作条。
 *
 * <p>
 * 本类只做<b>编排</b>：查询条件交给 {@link UserQueryBar}，表格与选中交给 {@link UserManageTable}，
 * 动作用 {@link UserManageActions} 与 {@link UserBatchImport}。因此这里不出现对话框与请求细节，
 * 分页状态也只有一处（{@code m_page_number}）。
 */
public class UserManagePanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 管理轨 API。 */
    private final UserAdminService m_api;

    /** 筛选条。 */
    private final UserQueryBar m_query = new UserQueryBar();

    /** 表格。 */
    private final UserManageTable m_table = new UserManageTable();

    /** 页脚：进度提示 + 分页控件。 */
    private final UserPagerPanel m_pager = new UserPagerPanel();

    /** 单条动作。 */
    private final UserManageActions m_actions;

    /** 批量动作。 */
    private final UserBatchImport m_batch;

    /** 当前页码。 */
    private int m_pageNumber = 1;

    /** 每页记录数。 */
    private int m_pageSize = 20;

    /**
     * 构造用户管理面板。
     *
     * @param api 用户管理 API
     * @throws IllegalArgumentException api 为 null
     */
    public UserManagePanel(UserAdminService api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        this.m_api = api;
        this.m_actions = new UserManageActions(api, this, refreshAction());
        this.m_batch = new UserBatchImport(api, this, refreshAction(), progressSink());
        configurePager();
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        m_query.setOnQuery(new Runnable() {
            @Override
            public void run() {
                m_pageNumber = 1;
                refresh();
            }
        });
        add(m_query, BorderLayout.NORTH);
        add(m_table, BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);
        refresh();
    }

    /** 按当前条件重新查询并回填表格。 */
    public final void refresh() {
        final UserQuery query = m_query.toQuery(m_pageNumber, m_pageSize);
        UiTasks.run(new UiTasks.Task<PageResponse<User>>() {
            @Override
            public PageResponse<User> run() {
                return m_api.listUsers(query);
            }
        }, new UiTasks.Success<PageResponse<User>>() {
            @Override
            public void accept(PageResponse<User> page) {
                fill(page);
            }
        });
    }

    /** 回填表格与分页信息。 */
    private void fill(PageResponse<User> page) {
        m_table.show(page);
        m_pageNumber = page.getPageNumber();
        m_pageSize = page.getPageSize();
        m_pager.showPage(m_pageNumber, page.getTotalPages(), page.getTotal());
    }

    /** 绑定分页动作（页码状态只存在本类）。 */
    private void configurePager() {
        m_pager.setOnPrevious(new Runnable() {
            @Override
            public void run() {
                if (m_pageNumber > 1) {
                    m_pageNumber--;
                    refresh();
                }
            }
        });
        m_pager.setOnNext(new Runnable() {
            @Override
            public void run() {
                m_pageNumber++;
                refresh();
            }
        });
    }

    /** 动作条：左侧动作按钮，右侧分页控件。 */
    private JPanel createActionBar() {
        return new UserActionBar(m_actions, m_batch, m_table, m_pager);
    }

    /** 刷新回调。 */
    private Runnable refreshAction() {
        return new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        };
    }

    /** 进度出口：批量操作在后台线程上报进度，这里回切到事件线程显示。 */
    private BatchProgressListener progressSink() {
        return new BatchProgressListener() {
            @Override
            public void onProgress(final int completed, final int total) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        m_pager.showStatus("正在处理 " + completed + " / " + total);
                    }
                });
            }
        };
    }

    /** @return 表格（供测试断言） */
    public UserManageTable table() {
        return m_table;
    }

    /** @return 筛选条（供测试断言） */
    public UserQueryBar queryBar() {
        return m_query;
    }
}
